package com.rohit.ChatApplication.data;

import com.rohit.ChatApplication.entity.GroupMember;
import com.rohit.ChatApplication.entity.User;
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
public class UserPublicProfile {

    String id;

    String username;

    public  UserPublicProfile(User user){
        this.id = user.getUserId().toString();
        this.username = getUsername();
    }

//    public  UserPublicProfile(GroupMember member){
//        this.id = member.getUser().getUserId().toString();
//        this.username = member.getUser().getUsername();
//    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // same object
        if (!(o instanceof UserPublicProfile)) return false;
        UserPublicProfile that = (UserPublicProfile) o;
        return Objects.equals(id, that.id); // compare by ID
    }

    @Override
    public int hashCode() {
        return Objects.hash(id); // hash only by ID
    }
}
