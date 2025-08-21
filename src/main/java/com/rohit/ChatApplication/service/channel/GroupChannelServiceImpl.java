package com.rohit.ChatApplication.service.channel;

import com.rohit.ChatApplication.data.GroupMemberProfile;
import com.rohit.ChatApplication.data.channel.profile.GroupChannelProfile;
import com.rohit.ChatApplication.entity.GroupChannel;
import com.rohit.ChatApplication.entity.PrivateChannel;
import com.rohit.ChatApplication.exception.ChannelDoesNotExist;
import com.rohit.ChatApplication.repository.channel.GroupRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GroupChannelServiceImpl {

    private final GroupRepo groupRepo;

    public GroupChannelServiceImpl(GroupRepo groupRepo){
        this.groupRepo = groupRepo;
    }

    private GroupChannel getChannelById(UUID channelId) throws ChannelDoesNotExist {

        return groupRepo.findById(channelId).orElseThrow(
                        () -> new ChannelDoesNotExist(
                                "channel with id=%s does not exist !".formatted(channelId)));
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
}
