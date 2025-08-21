package com.rohit.ChatApplication.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingEvent {

    private String channelId;
    private String to;
    private String from;
    private boolean isTyping;
    private long ts;


}
