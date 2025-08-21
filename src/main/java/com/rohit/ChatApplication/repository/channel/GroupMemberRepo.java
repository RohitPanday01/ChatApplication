package com.rohit.ChatApplication.repository.channel;

import com.rohit.ChatApplication.entity.GroupChannel;
import com.rohit.ChatApplication.entity.GroupMember;
import com.rohit.ChatApplication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface GroupMemberRepo extends JpaRepository<GroupMember , UUID> {

     List<GroupMember> findByUser(User user);



}
