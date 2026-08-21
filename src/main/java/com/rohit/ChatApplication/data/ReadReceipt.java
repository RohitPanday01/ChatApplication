package com.rohit.ChatApplication.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReadReceipt(
        String messageId,
        String channelId,
        String sender,
        ReceiptType type,
        String receiver,
        Instant timestamp // Kept as Instant object
) {
    // Compact Constructor for input validation
    public ReadReceipt {
        if (messageId == null || type == null) {
            throw new IllegalArgumentException("MessageId and ReceiptType cannot be null");
        }
    }

    // Overloaded constructor for quick instantiation
    public ReadReceipt( String messageId, String channelId, String sender, ReceiptType type, String receiver) {
        this( messageId, channelId, sender, type, receiver, Instant.now());
    }

}