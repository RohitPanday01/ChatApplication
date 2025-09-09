package com.rohit.ChatApplication.service.channel;

import com.rohit.ChatApplication.data.GroupMemberProfile;
import com.rohit.ChatApplication.data.channel.profile.GroupChannelProfile;
import com.rohit.ChatApplication.entity.GroupChannel;
import com.rohit.ChatApplication.entity.GroupMember;
import com.rohit.ChatApplication.entity.PrivateChannel;
import com.rohit.ChatApplication.exception.ChannelDoesNotExist;
import com.rohit.ChatApplication.repository.channel.GroupRepo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupChannelServiceImpl {

    private final GroupRepo groupRepo;
    private static final Duration CACHE_TTL = Duration.ofHours(3);
    private final RedisTemplate<String ,Object> redisTemplate;

    public GroupChannelServiceImpl(GroupRepo groupRepo, RedisTemplate<String , Object> redisTemplate){
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

    public Set<GroupChannelProfile> findAllGroupsForUser(String userId){

        UUID userUUID;
        try{
            userUUID = UUID.fromString(userId);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid UUID format for user IDs.");
        }

         List<GroupChannel> groupChannelList = groupRepo.findAllByUserId(userUUID);

        Set<GroupChannelProfile> groupChannelProfiles =  groupChannelList.stream()
                .map(GroupChannelProfile::new)
                .collect(Collectors.toSet());


        return groupChannelProfiles;

    }

    public Set<String>  getAllGroupMembersOfChannel(String groupChannelId){

        String redisKey = "group:members:" + groupChannelId;

        Set<Object> members = redisTemplate.opsForSet().members(redisKey);
        if (members != null && !members.isEmpty()) {
           return  members.stream()
                   .map(Objects::toString)
                   .collect(Collectors.toSet());
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
            redisTemplate.opsForSet().add(redisKey, groupMemberUserNames.toArray());
            redisTemplate.expire(redisKey, CACHE_TTL);
        }

        return groupMemberUserNames;


    }





}
