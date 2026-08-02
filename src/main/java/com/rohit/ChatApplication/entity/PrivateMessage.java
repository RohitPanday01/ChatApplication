package com.rohit.ChatApplication.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@BatchSize(size = 128)
@Table(name = "private_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrivateMessage extends  TimeStampBase{



    @EmbeddedId
    private PrivateMessageId messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id",  nullable = false)
    private User from;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User to;

    @ManyToOne(fetch = FetchType.LAZY )
    @JoinColumn(name = "private_channel_id", insertable = false, updatable = false)
    private PrivateChannel privateChannel;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private MessageType messageType;

//    @Column(name = "message_seq", nullable = false)
//    private Long messageSeq;


    @Column(name = "prev_msg_seq")
    private Long prevMsgSeq;


    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

//    private Instant seenAt;
//
//    private Instant deliveredAt;


    @Column(columnDefinition = "Text", nullable = false)
    private String content;



    public PrivateMessage(PrivateChannel privateChannel , User from , User to ,
                          MessageType messageType, String content ,Long messageSeq ){
        if (Objects.equals(from, to)) {
            throw new IllegalArgumentException("From user cannot be the same as To user");
        }
        if (privateChannel == null || privateChannel.getPrivateChannelId() == null) {
            throw new IllegalArgumentException("PrivateChannel and its ID cannot be null");
        }
        if (messageSeq == null) {
            throw new IllegalArgumentException("messageSeq (Snowflake ID) cannot be null");
        }
        this.messageId = new PrivateMessageId(privateChannel.getPrivateChannelId(), messageSeq);
        this.privateChannel = privateChannel;
        this.from = from;
        this.to = to;
        this.messageType = messageType;
        this.content =  content;
//        this.sentAt = Instant.now();
    }

    // --- DELEGATOR GETTERS ---
    public Long getMessageSeq() {
        return this.messageId != null ? this.messageId.getMessageSeq() : null;
    }

    public UUID getChannelId() {
        return this.messageId != null ? this.messageId.getPrivateChannelId() : null;
    }

    // --- SAFE EQUALS ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        PrivateMessage that = (PrivateMessage) o;
        return getMessageId() != null && Objects.equals(getMessageId(), that.getMessageId());
    }

    // --- SAFE HASHCODE ---
    @Override
    public int hashCode() {
        return Objects.hash(getMessageId());
    }

    // --- SAFE TOSTRING (Excludes LAZY entities: privateChannel, from, to) ---
    @Override
    public String toString() {
        return "PrivateMessage{" +
                "messageId=" + messageId +
                ", prevMsgSeq=" + prevMsgSeq +
                ", messageType=" + messageType +
                ", sentAt=" + sentAt +
                ", content='" + content + '\'' +
                '}';
    }
}
