package com.rohit.ChatApplication.entity;

import com.rohit.ChatApplication.exception.InvalidOperation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@BatchSize(size = 64)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "private_channel",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_private_channel_users", columnNames = {"user1_id", "user2_id"})
        },
        indexes = {
                @Index(name = "idx_private_channels_user1", columnList = "user1_id"),
                @Index(name = "idx_private_channels_user2", columnList = "user2_id")
        }
)
public class PrivateChannel extends  TimeStampBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID privateChannelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id")
    private User user1;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "user2_id")
    private User user2;

    @OneToMany( mappedBy = "privateChannel",cascade = CascadeType.ALL, orphanRemoval = true )
    private List<PrivateMessage> messages;

//    @OneToOne
//    @JoinColumn(name = "last_message_id")
//    private PrivateMessage lastMessage;

    @Column(nullable = false)
    private boolean user1BlockedUser2 = false;

    @Column(nullable = false)
    private boolean user2BlockedUser1 = false;


    public PrivateChannel(User u1, User u2) {
        // Deterministic Sorting: Ensures user1_id is ALWAYS less than user2_id
        if (u1.getUserId().toString().compareTo(u2.getUserId().toString()) < 0) {
            this.user1 = u1;
            this.user2 = u2;
        } else {
            this.user1 = u2;
            this.user2 = u1;
        }
    }

    public  boolean isBlocked(){
        return user1BlockedUser2 || user2BlockedUser1;
    }

    public void block(User blocker){
        if(blocker.equals(user1)){
            user1BlockedUser2 = true;
         }else{
            user2BlockedUser1 = true;
        }
    }

    public void unblock(User unBlocker){
        if (unBlocker.equals(user1)) {
            if (!user1BlockedUser2) {
                throw new IllegalArgumentException("You cannot unblock someone you haven't blocked.");
            }
            user1BlockedUser2 = false;
        } else if (unBlocker.equals(user2)) {
            if (!user2BlockedUser1) {
                throw new IllegalArgumentException("You cannot unblock someone you haven't blocked.");
            }
            user2BlockedUser1 = false;
        }
    }

    public User anotherMember(User member){
        if(member.getUserId().equals(user1.getUserId())){
            return user2;
        }

        return user1;
    }

    public void  sendMessage(User messageSender , User messageReceiver , MessageType messageType , String content ,
                             long messageSeq){
        if(isBlocked()){
            throw new IllegalStateException("Messaging is blocked in this chat.");
        }

        PrivateMessage message = new PrivateMessage(null ,this , messageSender , messageReceiver ,messageType, content
        ,messageSeq);
        messages.add(message);
//        lastMessage = message;
    }

    public PrivateMessage addMessage(User from , User to , MessageType messageType, String content, long messageSeq )
            throws InvalidOperation{
        if (!user1.getUserId().equals(from.getUserId())
                && !user2.getUserId().equals(from.getUserId())) {
            throw new InvalidOperation("sender can't send message in this channel");
        }

        PrivateMessage privateMessage = new PrivateMessage(null,this, from, to ,
                messageType , content, messageSeq );
        
        messages.add(privateMessage);
//        lastMessage = privateMessage;

        return privateMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PrivateChannel that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(privateChannelId, that.privateChannelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), privateChannelId);
    }

    @Override
    public String toString() {
        return "PrivateChannel{" +
                "privateChannelId=" + privateChannelId +
                ", user1=" + user1 +
                ", user2=" + user2 +
                ", messages=" + messages +
                ", user1BlockedUser2=" + user1BlockedUser2 +
                ", user2BlockedUser1=" + user2BlockedUser1 +
                '}';
    }
}
