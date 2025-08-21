package com.rohit.ChatApplication.data.channel.profile;

import com.rohit.ChatApplication.data.GroupMemberProfile;
import com.rohit.ChatApplication.data.UserPublicProfile;
import com.rohit.ChatApplication.entity.GroupChannel;
import com.rohit.ChatApplication.entity.GroupMember;
import com.rohit.ChatApplication.util.TimeUtil;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public class GroupChannelProfile {

    private String id;

    private String name;

    private List<GroupMemberProfile> members;

    private String createAt;

    private String updateAt;

    public GroupChannelProfile(GroupChannel channel) {
        this.id = channel.getGroupId().toString();
        this.name = channel.getGroupName();
        this.members = channel.getActiveMembers().stream()
                .map(GroupMemberProfile::new)
                .collect(Collectors.toList());

        this.createAt = TimeUtil.formatInstant(channel.getCreateAt());
        this.updateAt = TimeUtil.formatInstant(channel.getUpdatedAt());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupChannelProfile that)) return false;
        return id.equals(that.id) && members.equals(that.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
