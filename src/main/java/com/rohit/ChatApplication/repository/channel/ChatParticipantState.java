package com.rohit.ChatApplication.repository.channel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatParticipantState  extends JpaRepository<com.rohit.ChatApplication.entity.ChatParticipantState,
        UUID>{

}
