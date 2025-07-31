package com.rohit.ChatApplication.data;

import lombok.Data;

@Data
public class TypingEvent {

    private String channelId;
    private String senderUsername;
    private boolean isTyping;


}
