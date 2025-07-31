package com.rohit.ChatApplication.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name ="group_message")
@BatchSize(size = 128)
public class GroupMessage extends TimeStampBase{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "group_message_id", columnDefinition = "uuid", nullable = false , updatable = false)
    private UUID groupMessageId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "group_channel_id")
    private GroupChannel groupChannel;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private MessageType messageType;
 
    @ManyToOne(optional = false , fetch = FetchType.LAZY)
    @JoinColumn(name = "group_member_id")
    private  GroupMember from;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column( nullable = false)
    private Instant sentAt;

    public GroupMessage(GroupMember from , String content, GroupChannel groupChannel) {
        this.from = from;
        this.content = content;
        this.groupChannel = groupChannel;
    }

    /** generate a message for someone inviting another to join the GroupChannel */
    public static GroupMessage invitation(GroupChannel groupChannel , GroupMember inviter , User invitee){
        GroupMessage message = new GroupMessage();
        message.groupChannel = groupChannel;
        message.from = inviter;
        message.content = "{\"id\":\"%s\" , \"username\" :\"%s\" }"
                .formatted(invitee.getUserId().toString() , invitee.getUsername());
        message.messageType = MessageType.INVITATION;
        return message;
    }


    public static GroupMessage joinMessage(GroupChannel channel, GroupMember joiner) {
        GroupMessage message = new GroupMessage();
        message.groupChannel = channel;
        message.from = joiner;
        message.content =
                "{\"id\":\"%s\", \"username\":\"%s\"}"
                        .formatted(joiner.getUser().getUserId().toString(), joiner.getUser().getUsername());
        message.messageType = MessageType.JOIN;
        return message;
    }

    /** generate a message for someone leaving or kicked off from the GroupChannel */
    public static GroupMessage leaveMessage(GroupChannel channel, GroupMember actor, GroupMember subject) {
        GroupMessage message = new GroupMessage();
        message.groupChannel = channel;
        message.from = actor;
        message.content =
                "{\"id\":\"%s\", \"username\":\"%s\"}"
                        .formatted(subject.getUser().getUserId().toString(), subject.getUser().getUsername());
        message.messageType = MessageType.LEAVE;
        return message;
    }

    private static GroupMessage banOrUnBanMessageFormat(
            GroupChannel channel, GroupMember actor, GroupMember subject) {
        GroupMessage message = new GroupMessage();
        message.groupChannel = channel;
        message.from = actor;
        message.content =
                "{\"id\":\"%s\", \"username\":\"%s\"}"
                        .formatted(subject.getUser().getUserId().toString(), subject.getUser().getUsername());
        return message;
    }

    /** generate a message for admin banning  user */
    public static GroupMessage banMessage(GroupChannel channel, GroupMember actor, GroupMember subject) {
        GroupMessage message = banOrUnBanMessageFormat(channel, actor, subject);
        message.setMessageType(MessageType.BAN);
        return message;
    }



    /** generate a message for admin unbanning  user */
    public static GroupMessage unbanMessage(GroupChannel channel, GroupMember actor, GroupMember subject) {
        GroupMessage message = banOrUnBanMessageFormat(channel, actor, subject);
        message.setMessageType(MessageType.UNBAN);
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GroupMessage message)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(groupMessageId, message.groupMessageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), groupMessageId);
    }

    @Override
    public String toString() {
        return "GroupMessage{" +
                "groupMessageId=" + groupMessageId +
                ", groupChannel=" + groupChannel +
                ", messageType=" + messageType +
                ", from=" + from +
                ", content='" + content + '\'' +
                '}';
    }
}
