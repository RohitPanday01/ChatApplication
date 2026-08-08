package com.rohit.ChatApplication.data.message;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.rohit.ChatApplication.data.UserPublicProfile;
import com.rohit.ChatApplication.entity.MessageType;
import com.rohit.ChatApplication.entity.PrivateChannel;
import com.rohit.ChatApplication.entity.PrivateMessage;
import com.rohit.ChatApplication.entity.User;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.UUID;


@NoArgsConstructor
@Data
public class PrivateMessageDto   {

    protected UUID id;

    protected UUID channel;

    protected  Long message_seq;

    protected Long prevMessage_seq;

    protected MessageType messageType;

    protected UserPublicProfile from;

    protected UserPublicProfile to;

    protected  String content;
    protected  String sentAt;
    private long ingressTimestampNanos;


    public PrivateMessageDto (PrivateChannel privateChannel, User from,
                              String senderUsername,  User to, String receiverUsername, MessageType messageType,
                              String content ){

        this.channel = privateChannel.getPrivateChannelId();
        this.messageType = messageType;
        this.from = UserPublicProfile.builder()
                        .id(from.getUserId().toString())
                        .username(senderUsername)
                        .build();
        this.to = UserPublicProfile.builder()
                 .id(to.getUserId().toString())
                  .username(receiverUsername)
                  .build();
        this.content = content;
        this.sentAt = Instant.now().toString();
        this.ingressTimestampNanos = Instant.now().toEpochMilli();

    }

    public PrivateMessageDto (PrivateMessage message){

        this.message_seq  = message.getMessageId().getMessageSeq();
        this.channel = message.getMessageId().getPrivateChannelId();
        this.messageType = message.getMessageType();
        this.from = UserPublicProfile.builder()
                .id(message.getFrom().getUserId().toString())
                .username(message.getFrom().getUsername())
                .build();
        this.to = UserPublicProfile.builder()
                .id(message.getTo().getUserId().toString())
                .username(message.getTo().getUsername())
                .build();
        this.content = message.getContent();
        this.sentAt =  message.getSentAt().toString();

    }

    public static RowMapper<PrivateMessageDto> privateMessageMapper = (rs, rowNum) -> {
        PrivateMessageDto dto = new PrivateMessageDto();

        // 1. Map primitive and direct field
        dto.setChannel(UUID.fromString(rs.getString("private_channel_id")));
        dto.setMessage_seq(rs.getLong("message_seq"));

        // Handle nullable column for prevMessage_seq
        long prevSeq = rs.getLong("prev_msg_seq");
        dto.setPrevMessage_seq(rs.wasNull() ? null : prevSeq);

        // Map Enum (Assumes stored as String like 'TEXT', 'IMAGE')
        String typeStr = rs.getString("message_type");
        dto.setMessageType(typeStr != null ? MessageType.valueOf(typeStr) : null);

        dto.setContent(rs.getString("content"));
        dto.setSentAt(rs.getString("sent_at"));

        // 2. Map the nested 'from' profile using only the ID from the row
        String fromIdStr = rs.getString("from_user_id");
        if (fromIdStr != null) {
            UserPublicProfile fromProfile = new UserPublicProfile();
            fromProfile.setId(fromIdStr); // Or whatever type your User ID uses
            dto.setFrom(fromProfile);
        }

        // 3. Map the nested 'to' profile using only the ID from the row
        String toIdStr = rs.getString("to_user_id");
        if (toIdStr != null) {
            UserPublicProfile toProfile = new UserPublicProfile();
            toProfile.setId(toIdStr);
            dto.setTo(toProfile);
        }

        return dto;
    };






}
