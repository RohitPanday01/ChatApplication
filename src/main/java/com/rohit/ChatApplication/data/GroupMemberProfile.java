package com.rohit.ChatApplication.data;

import com.rohit.ChatApplication.entity.GroupMember;
import com.rohit.ChatApplication.entity.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@Builder
public class GroupMemberProfile  {

    private UserPublicProfile userProfile;
    private String groupChannelId;
    private boolean isBanned;
    private GroupRole groupRole;

    public GroupMemberProfile(GroupMember member) {
        this.userProfile = new UserPublicProfile(member.getUser());
        this.groupChannelId = member.getGroup().getGroupId().toString();
        this.isBanned = member.isBanned();
        this.groupRole = member.getGroupRole();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GroupMemberProfile that)) return false;
        return Objects.equals(groupChannelId, that.groupChannelId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupChannelId);
    }
}
