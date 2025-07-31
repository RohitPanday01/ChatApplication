package com.rohit.ChatApplication.data.message;

import com.rohit.ChatApplication.data.UserPublicProfile;
import com.rohit.ChatApplication.entity.GroupMessage;


public class GroupMessageDto extends  MessageDto {

    public GroupMessageDto(GroupMessage message){
        super(message.getGroupMessageId(), message.getGroupChannel().getGroupId(),
                message.getMessageType()
                , UserPublicProfile.builder()
                        .id(message.getFrom().getGroupMembersId().toString())
                        .username(message.getFrom().getUser().getUsername())
                        .build(),
                message.getContent(),
                message.getSentAt().toString()
        );
    }
}
