package com.rohit.ChatApplication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "chat_participant_state")
@Getter
@Setter
@AllArgsConstructor
public class ChatParticipantState {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "private_channel_id", nullable = false)
    private UUID privateChannelId;

    @Column(name = "last_delivered_seq", nullable = false)
    private Long lastDeliveredSeq;

    @Column(name = "last_read_seq", nullable = false)
    private Long lastReadSeq;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatParticipantState)) return false;
        ChatParticipantState that = (ChatParticipantState) o;
        return Objects.equals(getUserId(), that.getUserId()) &&
                Objects.equals(getPrivateChannelId(), that.getPrivateChannelId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId(), getPrivateChannelId());
    }
}
