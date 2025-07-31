package com.rohit.ChatApplication.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.Objects;

@Entity
@Data
@NoArgsConstructor
@Table(name = "group_invitation")
public class GroupInvitation {


    @EmbeddedId
    private GroupInvitationKey key;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @MapsId("channelId")
    @JoinColumn(name = "group_channel_id", nullable = false)
    private GroupChannel groupChannel;

    @OneToOne(optional = false ,fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn
    private GroupMessage invitationMessage;

    @CreationTimestamp
    private Instant createAt;

    public GroupInvitation(User user, GroupChannel groupChannel , GroupMessage invitationMessage){
        this.user = user;
        this.groupChannel = groupChannel;
        this.key = new GroupInvitationKey(user.getUserId() , groupChannel.getGroupId());

        if(!invitationMessage.getMessageType().equals(MessageType.INVITATION)){
            throw new IllegalArgumentException("message type must be INVITATION !");
        }
        this.invitationMessage = invitationMessage;
    }

    public GroupInvitation(User user, GroupChannel groupChannel) {
        this.user = user;
        this.groupChannel = groupChannel;
        this.key = new GroupInvitationKey(user.getUserId(), groupChannel.getGroupId());
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if (!(o instanceof GroupInvitation that)) return false;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }
}
