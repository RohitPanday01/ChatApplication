package com.rohit.ChatApplication.data.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.rohit.ChatApplication.data.UserPublicProfile;
import com.rohit.ChatApplication.entity.MessageType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.UUID;


@Data
@NoArgsConstructor
@Jacksonized
@Builder
public class MessageDto {

    protected UUID id;

    protected UUID channel;

    protected MessageType messageType;

    protected UserPublicProfile from;

    protected UserPublicProfile to;

    protected  String content;

    protected  String createAt;

    public MessageDto(UUID id , UUID channel , MessageType messageType,
                      UserPublicProfile from,  String content, String createAt  ){
        this.id = id;
        this.channel = channel;
        this.messageType = messageType;
        this.from = from;
        this.content = content;
        this.createAt = createAt;
    }

    public MessageDto(UUID id , UUID channel , MessageType messageType,
                      UserPublicProfile from, UserPublicProfile to, String content, String createAt  ){
        this.id = id;
        this.channel = channel;
        this.messageType = messageType;
        this.from = from;
        this.to  = to;
        this.content = content;
        this.createAt = createAt;

    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MessageDto that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(channel, that.channel)
                && messageType == that.messageType && Objects.equals(from, that.from)
                && Objects.equals(to, that.to)
                && Objects.equals(content, that.content)
                && Objects.equals(createAt, that.createAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, channel, messageType, from, to, content, createAt);
    }
}
