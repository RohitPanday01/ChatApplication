package com.rohit.ChatApplication.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@BatchSize(size = 128)
@Table(name = "private_message")
public class PrivateMessage extends  TimeStampBase{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "message_id")
    private UUID messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id",  nullable = false)
    private User from;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User to;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "private_channel_id",  nullable = false)
    private PrivateChannel privateChannel;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private MessageType messageType;

    private Instant sentAt;

    private Instant seenAt;

    private Instant deliveredAt;

    @Enumerated(EnumType.STRING)
    private PrivateMessageStatus status;

    @Column(columnDefinition = "Text", nullable = false)
    private String content;



    public PrivateMessage(PrivateChannel privateChannel , User from , User to ,MessageType messageType, String content ){
        if(Objects.equals(from, to)){
            throw new IllegalArgumentException("From cannot Be Same as to ");
        }
        this.privateChannel = privateChannel;
        this.from = from;
        this.to = to;
        this.messageType = messageType;
        this.content =  content;
        this.sentAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PrivateMessage message)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(messageId, message.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), messageId);
    }

    @Override
    public String toString() {
        return "PrivateMessage{" +
                "from=" + from +
                ", message_id=" + messageId +
                ", to=" + to +
                ", messageType=" + messageType +
                ", privateChannel=" + privateChannel +
                ", content='" + content + '\'' +
                '}';
    }
}
