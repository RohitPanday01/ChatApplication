package com.rohit.ChatApplication.repository.channel;

import com.rohit.ChatApplication.entity.GroupChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GroupRepo extends JpaRepository<GroupChannel, UUID> {

    @Override
    Optional<GroupChannel> findById(UUID uuid);


}
