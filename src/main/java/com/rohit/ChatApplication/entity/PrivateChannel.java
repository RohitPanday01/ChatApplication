package com.rohit.ChatApplication.entity;

import com.rohit.ChatApplication.exception.InvalidOperation;
import jakarta.persistence.*;
import lombok.Getter;
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
public class PrivateChannel extends TimeStampBase{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID privateChannelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id")
    private User user1;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "user2_id")
    private User user2;

    @OneToMany( mappedBy = "privateChannel", cascade = CascadeType.ALL )
    private List<PrivateMessage> messages;

    @OneToOne
    @JoinColumn(name = "last_message_id")
    private PrivateMessage lastMessage;


//    // user1 blocked user user2 in 0 indexed and user2 blocked user1 in 1 index
//    private final boolean[] blockedUsers = new boolean[2];

    @Column(nullable = false)
    private boolean user1BlockedUser2 = false;

    @Column(nullable = false)
    private boolean user2BlockedUser1 = false;


    public PrivateChannel(User user1 ,User user2){
        this.user1 = user1;
        this.user2 = user2;
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
        if(member.getUserId() == user1.getUserId()){
            return user1;
        }

        return user2;
    }

    public void  sendMessage(User messageSender , User messageReceiver , MessageType messageType , String content  ){
        if(isBlocked()){
            throw new IllegalStateException("Messaging is blocked in this chat.");
        }

        PrivateMessage message = new PrivateMessage(this , messageSender , messageReceiver ,messageType, content );
        messages.add(message);
        lastMessage = message;
    }

    public void addMessage(User from ,MessageType messageType, String content ) throws InvalidOperation{
        if(user1.getUserId() != from.getUserId() || user2.getUserId() != from.getUserId()){
            throw new InvalidOperation("sender can't send message in this channel");
        }

        PrivateMessage privateMessage = new PrivateMessage(this, from, anotherMember(from) , messageType , content );
        
        messages.add(privateMessage);
        lastMessage = privateMessage;
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
                ", lastMessage=" + lastMessage +
                ", user1BlockedUser2=" + user1BlockedUser2 +
                ", user2BlockedUser1=" + user2BlockedUser1 +
                '}';
    }
}
