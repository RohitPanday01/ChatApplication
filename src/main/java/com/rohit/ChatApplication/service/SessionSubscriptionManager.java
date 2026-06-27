package com.rohit.ChatApplication.service;

import com.rohit.ChatApplication.data.SliceList;
import com.rohit.ChatApplication.data.channel.profile.PrivateChannelProfile;
import com.rohit.ChatApplication.exception.DatabaseRuntimeException;
import com.rohit.ChatApplication.exception.UserDoesNotExist;
import com.rohit.ChatApplication.service.Typing.ChannelSubscriberForTyping;
import com.rohit.ChatApplication.service.Typing.TypingSubscriber;
import com.rohit.ChatApplication.service.channel.PrivateChannelServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class SessionSubscriptionManager {
    private final Logger log = LoggerFactory.getLogger(SessionSubscriptionManager.class);
    private static final String EMPTY_PLACEHOLDER = "_NONE_";
    private static final String REDIS_KEY_PREFIX = "user:channels:";
    private final ChannelSubscriberForTyping channelSubscriberForTyping;
    private final PrivateChannelServiceImpl privateChannelService;


    private final RedisTemplate<String , String > redisTemplate;

    public SessionSubscriptionManager(ChannelSubscriberForTyping channelSubscriberForTyping,
                                      PrivateChannelServiceImpl privateChannelService,
                                      @Qualifier("redisStringTemplate")RedisTemplate<String, String> redisTemplate) {
       this.channelSubscriberForTyping = channelSubscriberForTyping;
        this.privateChannelService = privateChannelService;
        this.redisTemplate = redisTemplate;
    }

    public void subscribeUserChannels(String userId){

        String key = REDIS_KEY_PREFIX + userId;
        List<String> channelIdsToSubscribe = new ArrayList<>();
        boolean isCacheHit = false;

        // 1. Safe Cache Read (Fault Isolation)
        try {
            Set<String> cachedChannelIds = redisTemplate.opsForSet().members(key);
            if (cachedChannelIds != null && !cachedChannelIds.isEmpty()) {
                isCacheHit = true;

                // Check for our cache penetration placeholder
                if (cachedChannelIds.contains(EMPTY_PLACEHOLDER)) {
                    log.debug("Cache hit for user: {} with zero private channels.", userId);
                    return; // User has no channels; exit early without hitting the DB
                }

                channelIdsToSubscribe.addAll(cachedChannelIds);
            }
        } catch (Exception e) {
            // If Redis is down, we log it and continue down to the DB fallback automatically
            log.error("Redis error fetching channels for user: {}, falling back to DB", userId, e);
        }

        // 2. Cache Miss / Database Fetch & Write-Back
        if (!isCacheHit) {
            try {
                // FIX: Removed local 'channelForUserCache' map to prevent memory leaks and state staleness
                List<PrivateChannelProfile> privateChannelProfileList =
                        privateChannelService.getAllChannelWithoutPagination(userId);

                channelIdsToSubscribe = privateChannelProfileList.stream()
                        .map(PrivateChannelProfile::getId)
                        .toList();

                // Safe Cache Write-Back
                try {
                    if (channelIdsToSubscribe.isEmpty()) {
                        // Prevent Cache Penetration if the user has 0 active channels
                        redisTemplate.opsForSet().add(key, EMPTY_PLACEHOLDER);
                    } else {
                        redisTemplate.opsForSet().add(key, channelIdsToSubscribe.toArray(new String[0]));
                    }
                    redisTemplate.expire(key, Duration.ofHours(6));
                } catch (Exception cacheEx) {
                    log.error("Failed to write to Redis cache for user channels: {}", userId, cacheEx);
                }

            } catch (Exception dbEx) {
                log.error("Critical database failure loading channels for user: {}", userId, dbEx);
                throw new DatabaseRuntimeException("Database unavailable during channel subscription processing");
            }
        }

        // 3. Local Node Pub/Sub Routing Subscriptions
        for (String channelId : channelIdsToSubscribe) {
            try {
                channelSubscriberForTyping.subscribePrivateChannel(channelId);
            } catch (Exception subEx) {
                // Defensive Guardrail: If subscription to one channel topic fails,
                // log it and continue so we don't abort the rest of the user's channels.
                log.error("Failed to register local Pub/Sub subscriber for channel: {}", channelId, subEx);
            }
        }
    }

    public void unsubscribeUserChannels(String userId)  {
        String key = REDIS_KEY_PREFIX + userId;
        Set<String> channelIdsToUnsubscribe = null;
        try {
            channelIdsToUnsubscribe = redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Redis failure while fetching channels for unsubscription, user: {}. Falling back to DB.", userId, e);
            // Do not crash. If Redis is down, we fall back to the DB to figure out what to clean up.
        }

        // 2. Fallback to Database if Redis is Down or Cache is Empty
        // This is critical to prevent memory leaks in your local Pub/Sub topic router if Redis fails.
        if (channelIdsToUnsubscribe == null || channelIdsToUnsubscribe.isEmpty()) {
            try {
                List<PrivateChannelProfile> privateChannelProfileList =
                        privateChannelService.getAllChannelWithoutPagination(userId);

                channelIdsToUnsubscribe = privateChannelProfileList.stream()
                        .map(PrivateChannelProfile::getId)
                        .collect(Collectors.toSet());
            } catch (Exception dbEx) {
                log.error("Critical DB failure during unsubscription fallback for user: {}. Local channels might leak!", userId, dbEx);
                return; // Out of options, exit to prevent blocking the WebSocket teardown thread
            }
        }

        // 3. Check for Cache Penetration Placeholder
        if (channelIdsToUnsubscribe.contains(EMPTY_PLACEHOLDER)) {
            log.debug("User {} had zero channels cached. No local unsubscriptions needed.", userId);
            return;
        }

        // 4. Safe Local Node Pub/Sub Unsubscriptions
        for (String channelId : channelIdsToUnsubscribe) {
            try {
                channelSubscriberForTyping.unsubscribePrivateChannel(channelId);
            } catch (Exception unsubEx) {
                // Defensive Guardrail: If unregistering from one topic fails, log it and
                // keep moving so the rest of the user's topics are cleanly closed out.
                log.error("Failed to unregister local Pub/Sub subscriber for channel: {}", channelId, unsubEx);
            }
        }

    }

    public void subscribeGroup(String groupId) {
        channelSubscriberForTyping.subscribeGroup(groupId);
    }

    public void unsubscribeGroup(String groupId) {
        channelSubscriberForTyping.unsubscribeGroup(groupId);
    }
}
