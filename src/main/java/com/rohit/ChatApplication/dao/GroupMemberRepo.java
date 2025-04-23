package com.rohit.ChatApplication.dao;

import com.rohit.ChatApplication.entity.Group;
import com.rohit.ChatApplication.entity.GroupMember;
import com.rohit.ChatApplication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GroupMemberRepo extends JpaRepository<GroupMember , UUID> {

     List<GroupMember> findByUser(User user);

     List<GroupMember> findByGroup(Group group);

}
