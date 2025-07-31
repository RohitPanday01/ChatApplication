package com.rohit.ChatApplication.data.channel.profile;

import com.rohit.ChatApplication.data.UserPublicProfile;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.entity.PrivateChannel;
import com.rohit.ChatApplication.entity.PrivateMessage;
import com.rohit.ChatApplication.util.TimeUtil;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class PrivateChannelProfile {

    private String id;

    private List<UserPublicProfile> members;

    private PrivateMessageDto lastMessage;

    private String createAt;

    private String updatedAt;

    private boolean isBlocked;

    public PrivateChannelProfile (PrivateChannel channel ){
        this.id = channel.getPrivateChannelId().toString();

        this.members = Stream.of(channel.getUser1(), channel.getUser2())
                .map(UserPublicProfile::new)
                .collect(Collectors.toList());
        this.lastMessage = new PrivateMessageDto(channel.getLastMessage());
        this.createAt = TimeUtil.formatInstant(channel.getCreateAt());
        this.updatedAt = TimeUtil.formatInstant(channel.getUpdatedAt());
        this.isBlocked = channel.isBlocked();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PrivateChannelProfile that)) return false;
        return isBlocked == that.isBlocked && Objects.equals(id, that.id) && Objects.equals(members, that.members) && Objects.equals(createAt, that.createAt) && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, members, createAt, updatedAt, isBlocked);
    }
}
