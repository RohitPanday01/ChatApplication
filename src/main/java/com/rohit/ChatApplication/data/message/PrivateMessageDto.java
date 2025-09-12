package com.rohit.ChatApplication.data.message;

import com.rohit.ChatApplication.data.UserPublicProfile;
import com.rohit.ChatApplication.entity.PrivateMessage;
import lombok.NoArgsConstructor;


@NoArgsConstructor
public class PrivateMessageDto  extends  MessageDto  {

    public PrivateMessageDto (PrivateMessage message){
        super(message.getMessageId(), message.getPrivateChannel().getPrivateChannelId(),
                message.getMessageType()
                , UserPublicProfile.builder()
                        .id(message.getFrom().getUserId().toString())
                        .username(message.getFrom().getUsername())
                        .build(),
                UserPublicProfile.builder()
                        .id(message.getTo().getUserId().toString())
                        .username(message.getTo().getUsername())
                        .build(),
                message.getContent(),
                message.getSentAt().toString()
                );
    }

}
