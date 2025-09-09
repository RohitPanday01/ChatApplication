package com.rohit.ChatApplication.repository.channel;

import com.rohit.ChatApplication.entity.GroupChannel;
import com.rohit.ChatApplication.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface GroupRepo extends JpaRepository<GroupChannel, UUID> {

    @Override
    Optional<GroupChannel> findById(UUID uuid);


    @Query("""
    SELECT gm.group FROM GroupMember gm WHERE gm.user.userId = :userId
    """)
    List<GroupChannel> findAllByUserId(@Param("userId") UUID userId);

    @Query("""
    SELECT gm FROM GroupMember gm 
    WHERE gm.group.id = :groupId
    """)
    Set<GroupMember> getAllUserOfChannel(@Param("groupId") UUID groupId);


}
