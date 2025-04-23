package com.rohit.ChatApplication.service;
import com.rohit.ChatApplication.dao.GroupMemberRepo;
import com.rohit.ChatApplication.dao.UserRepo;
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

@Service
public class UsersDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private GroupMemberRepo groupMemberRepo;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));

        Set<GrantedAuthority> authorities = new HashSet<>();

        List<GroupMember> groupMembers = groupMemberRepo.findByUser(user);

        for (GroupMember groupMember : groupMembers) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + groupMember.getGroupRole().name()));
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }

}

