package com.rohit.ChatApplication.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadReceipt {
    private String messageId;
    private String channelId;
    private String sender;
    private ReceiptType type;
    private String receiver;
    private Instant timestamp;

}
