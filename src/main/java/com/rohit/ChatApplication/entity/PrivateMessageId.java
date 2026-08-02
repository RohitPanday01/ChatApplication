package com.rohit.ChatApplication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode // MANDATORY for JPA composite primary keys
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessageId implements Serializable {

    @Column(name = "private_channel_id", nullable = false)
    private UUID privateChannelId;

    @Column(name = "message_seq", nullable = false)
    private Long messageSeq; // 64-bit Snowflake ID
}