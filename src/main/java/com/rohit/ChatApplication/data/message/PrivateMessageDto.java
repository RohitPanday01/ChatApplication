package com.rohit.ChatApplication.data.message;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.rohit.ChatApplication.data.UserPublicProfile;
import com.rohit.ChatApplication.entity.MessageType;
import com.rohit.ChatApplication.entity.PrivateMessage;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;


@NoArgsConstructor
@Jacksonized
@Data
public class PrivateMessageDto   {

    protected UUID id;

    protected UUID channel;

    protected MessageType messageType;

    protected UserPublicProfile from;

    protected UserPublicProfile to;

    protected  String content;

    protected  String sentAt;


    public PrivateMessageDto (PrivateMessage message){
        this.id  = message.getMessageId();
        this.channel = message.getPrivateChannel().getPrivateChannelId();
        this.messageType = message.getMessageType();
        this.from = UserPublicProfile.builder()
                        .id(message.getFrom().getUserId().toString())
                        .username(message.getFrom().getUsername())
                        .build();
        this.to = UserPublicProfile.builder()
                 .id(message.getTo().getUserId().toString())
                  .username(message.getTo().getUsername())
                  .build();
        this.content = message.getContent();
        this.sentAt =  message.getSentAt().toString();

    }



}
