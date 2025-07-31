package com.rohit.ChatApplication.data.message.request;

import com.rohit.ChatApplication.data.UserPublicProfile;
import com.rohit.ChatApplication.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishMessageRequest {
    String channelId;
    MessageType messageType;
    String messageContent;
    UserPublicProfile from;
    UserPublicProfile to ;
}
