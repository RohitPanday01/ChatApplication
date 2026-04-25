package com.rohit.ChatApplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "chat_participant_state")
@Data
@AllArgsConstructor
public class ChatParticipantState {
    @Id
    private UUID id;

    private UUID userId;

    private UUID privateChannelId;

    private Long lastDeliveredSeq;

    private Long lastReadSeq;
}
