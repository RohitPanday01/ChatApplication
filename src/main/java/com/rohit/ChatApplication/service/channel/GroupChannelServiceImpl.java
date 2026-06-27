package com.rohit.ChatApplication.service.channel;

import com.rohit.ChatApplication.controller.Websocket.PresenceWSHandler;
import com.rohit.ChatApplication.data.GroupMemberProfile;
import com.rohit.ChatApplication.data.channel.profile.GroupChannelProfile;
import com.rohit.ChatApplication.entity.GroupChannel;
import com.rohit.ChatApplication.entity.GroupMember;
import com.rohit.ChatApplication.entity.PrivateChannel;
import com.rohit.ChatApplication.exception.ChannelDoesNotExist;
import com.rohit.ChatApplication.exception.DatabaseRuntimeException;
import com.rohit.ChatApplication.repository.channel.GroupRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupChannelServiceImpl {
    private final Logger log = LoggerFactory.getLogger(GroupChannelServiceImpl.class);

    private final GroupRepo groupRepo;
    private static final Duration CACHE_TTL = Duration.ofHours(3);
    private final RedisTemplate<String ,String > redisTemplate;
    private static final String EMPTY_PLACEHOLDER = "_NONE_";

    public GroupChannelServiceImpl(GroupRepo groupRepo,
                                   @Qualifier("redisStringTemplate") RedisTemplate<String , String> redisTemplate){
        this.groupRepo = groupRepo;
        this.redisTemplate = redisTemplate;
    }

    public Optional<GroupChannel> getChannelById(UUID channelId)  {

//        return groupRepo.findById(channelId).orElseThrow(
//                        () -> new ChannelDoesNotExist(
//                                "channel with id=%s does not exist !".formatted(channelId)));

        Optional<GroupChannel> optionalGroupChannel = groupRepo.findById(channelId);

        if(optionalGroupChannel.isEmpty()){
            return null;
        }
        return optionalGroupChannel;
    }

    public Set<String> findAllGroupsForUser(String userId) {

        String key = "groups:user:" + userId;

        // 1. Safe Cache Read (Fault Isolation)
        try {
            Set<String> groupChannelIds = redisTemplate.opsForSet().members(key);
            if (groupChannelIds != null && !groupChannelIds.isEmpty()) {
                // Check for our cache penetration placeholder
                if (groupChannelIds.contains(EMPTY_PLACEHOLDER)) {
                    return Collections.emptySet();
                }
                return groupChannelIds;
            }
        } catch (Exception e) {
            // Log the error but DO NOT crash. Let the thread fall back to the DB gracefully.
            log.error("Redis error fetching groups for user: {}, falling back to DB", userId, e);
        }

        // 2. Database Fetch & Data Validation
        Set<String> groupChannelIdsFromDb;
        try {
            UUID userUUID = UUID.fromString(userId);
            List<GroupChannel> groupChannelList = groupRepo.findAllByUserId(userUUID);

            groupChannelIdsFromDb = groupChannelList.stream()
                    .map(GroupChannel::getGroupId)
                    .map(UUID::toString)
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format provided for userId: {}", userId, e);
            return Collections.emptySet();
        } catch (Exception e) {
            log.error("Critical database failure loading groups for user: {}", userId, e);
            // Throw a custom domain exception or propagate because if DB is dead, we cannot proceed
            throw new DatabaseRuntimeException("Database unavailable during group hydration");
        }

        // 3. Safe Cache Write-Back (Fault Isolation + TTL)
        try {
            if (groupChannelIdsFromDb.isEmpty()) {
                // FIX: Prevent Cache Penetration by storing a placeholder string
                redisTemplate.opsForSet().add(key, EMPTY_PLACEHOLDER);
            } else {
                redisTemplate.opsForSet().add(key, groupChannelIdsFromDb.toArray(new String[0]));
            }
            // FIX: Enforce a 12-hour expiration time to prevent memory leaks and clear stale data
            redisTemplate.expire(key, Duration.ofHours(12));
        } catch (Exception e) {
            log.error("Failed to populate Redis cache for user groups: {}", userId, e);
        }

        return groupChannelIdsFromDb;
    }

    public Set<String>  getAllGroupMembersOfChannel(String groupChannelId){

        String redisKey = "group:members:" + groupChannelId;

        Set<String> members = redisTemplate.opsForSet().members(redisKey);
        if (members != null && !members.isEmpty()) {
           return  members;
        }

        UUID groupChannelUUID;
        try{
            groupChannelUUID = UUID.fromString(groupChannelId);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid UUID format for group IDs.");
        }


        Set<GroupMember> groupMembers = groupRepo.getAllUserOfChannel(groupChannelUUID);

        if (groupMembers == null || groupMembers.isEmpty()) {
            return Collections.emptySet();
        }


        Set<String> groupMemberUserNames = groupMembers.stream()
                .map(groupMember -> groupMember.getUser().getUsername())
                .collect(Collectors.toSet());

        if(!groupMemberUserNames.isEmpty()){
            redisTemplate.opsForSet().add(redisKey, groupMemberUserNames.toArray(new String[0]));
            redisTemplate.expire(redisKey, CACHE_TTL);
        }

        return groupMemberUserNames;


    }





}
