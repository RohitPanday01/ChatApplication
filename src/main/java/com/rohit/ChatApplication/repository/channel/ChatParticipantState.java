package com.rohit.ChatApplication.repository.channel;

import com.rohit.ChatApplication.entity.ChatParticipantStateId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatParticipantState  extends JpaRepository<com.rohit.ChatApplication.entity.ChatParticipantState,
        ChatParticipantStateId>{

}
