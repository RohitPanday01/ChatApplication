package com.rohit.ChatApplication.service;

import com.rohit.ChatApplication.data.SliceList;
import com.rohit.ChatApplication.data.channel.profile.PrivateChannelProfile;
import com.rohit.ChatApplication.exception.UserDoesNotExist;
import com.rohit.ChatApplication.service.Typing.ChannelSubscriberForTyping;
import com.rohit.ChatApplication.service.Typing.TypingSubscriber;
import com.rohit.ChatApplication.service.channel.PrivateChannelServiceImpl;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class SessionSubscriptionManager {
    private final ChannelSubscriberForTyping channelSubscriberForTyping;
    private final PrivateChannelServiceImpl privateChannelService;
    private final ConcurrentMap<String, List<PrivateChannelProfile>> channelForUserCache =
            new ConcurrentHashMap<>();

    private final RedisTemplate<String , Object> redisTemplate;

    public SessionSubscriptionManager(ChannelSubscriberForTyping channelSubscriberForTyping,
                                      PrivateChannelServiceImpl privateChannelService,
                                      RedisTemplate<String, Object> redisTemplate) {
       this.channelSubscriberForTyping = channelSubscriberForTyping;
        this.privateChannelService = privateChannelService;
        this.redisTemplate = redisTemplate;
    }

    public void subscribeUserChannels(String userId){


        List<PrivateChannelProfile> privateChannelProfileList =
                privateChannelService.getAllChannelWithoutPagination(userId);
        channelForUserCache.put(userId , privateChannelProfileList );

        for(PrivateChannelProfile privateChannelProfile : privateChannelProfileList){
            channelSubscriberForTyping.subscribePrivateChannel(privateChannelProfile.getId());
        }
    }

    public void unsubscribeUserChannels(String userId) throws UserDoesNotExist {
        List<PrivateChannelProfile> privateChannelProfileList = channelForUserCache.getOrDefault(userId , List.of());

        for(PrivateChannelProfile privateChannelProfile : privateChannelProfileList){
            channelSubscriberForTyping.unsubscribePrivateChannel(privateChannelProfile.getId());
        }

    }

    public void subscribeGroup(String groupId) {
        channelSubscriberForTyping.subscribeGroup(groupId);
    }

    public void unsubscribeGroup(String groupId) {
        channelSubscriberForTyping.unsubscribeGroup(groupId);
    }
}
