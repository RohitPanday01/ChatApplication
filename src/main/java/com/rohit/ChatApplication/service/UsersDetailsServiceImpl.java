package com.rohit.ChatApplication.service;
import com.rohit.ChatApplication.data.UserDetail;
import com.rohit.ChatApplication.exception.UserDoesNotExist;
import com.rohit.ChatApplication.repository.channel.GroupMemberRepo;
import com.rohit.ChatApplication.repository.UserRepo;
import com.rohit.ChatApplication.entity.GroupMember;
import com.rohit.ChatApplication.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UsersDetailsServiceImpl implements UserDetailsService {

    private final UserRepo userRepo;
    private final GroupMemberRepo groupMemberRepo;

    public UsersDetailsServiceImpl(UserRepo userRepo, GroupMemberRepo groupMemberRepo) {
        this.userRepo = userRepo;
        this.groupMemberRepo = groupMemberRepo;
    }

    @Transactional
    public User getUserById(String userId) throws
            IllegalArgumentException ,UserDoesNotExist {

        UUID uuid;
        try {
            uuid = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid UUID format for user IDs.");
        }

        return userRepo.findById(uuid)
                .orElseThrow(()-> new UserDoesNotExist("userDoesNot exist"));
    }

    @Override
    @Transactional
    public UserDetail loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));

        Set<GrantedAuthority> authorities = new HashSet<>();

        List<GroupMember> groupMembers = groupMemberRepo.findByUser(user);

        if (groupMembers != null && !groupMembers.isEmpty()) {
            for (GroupMember groupMember : groupMembers) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + groupMember.getGroupRole().name()));
            }
        } else {

            authorities.add(new SimpleGrantedAuthority("ROLE_Member"));
        }

        return new UserDetail(
                user.getUserId().toString(),
                user.getUsername(),
                user.getPassword(),
                authorities
                );
    }

}

