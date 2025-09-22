package com.rohit.ChatApplication.data.message;

import com.rohit.ChatApplication.data.UserPublicProfile;
import com.rohit.ChatApplication.entity.GroupMessage;
import com.rohit.ChatApplication.entity.MessageType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@NoArgsConstructor
@Data
public class GroupMessageDto  {

    protected UUID id;

    protected UUID channel;

    protected MessageType messageType;

    protected UserPublicProfile from;

    protected UserPublicProfile to;

    protected  String content;

    protected  String createAt;

    public GroupMessageDto(GroupMessage message  ){
        this.id = message.getGroupMessageId();
        this.channel = message.getGroupChannel().getGroupId();
        this.messageType = message.getMessageType();
        this.from = UserPublicProfile.builder()
                .id(message.getFrom().getGroupMembersId().toString())
                .username(message.getFrom().getUser().getUsername())
                .build();
        this.content = message.getContent();
        this.createAt = message.getSentAt().toString();
    }

//    public GroupMessageDto(GroupMessage message){
//        super(message.getGroupMessageId(), message.getGroupChannel().getGroupId(),
//                message.getMessageType()
//                , UserPublicProfile.builder()
//                        .id(message.getFrom().getGroupMembersId().toString())
//                        .username(message.getFrom().getUser().getUsername())
//                        .build(),
//                message.getContent(),
//                message.getSentAt().toString()
//        );


}

