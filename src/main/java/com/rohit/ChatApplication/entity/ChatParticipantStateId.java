package com.rohit.ChatApplication.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatParticipantStateId implements Serializable {

    private UUID userId;

    private UUID privateChannelId;
}
