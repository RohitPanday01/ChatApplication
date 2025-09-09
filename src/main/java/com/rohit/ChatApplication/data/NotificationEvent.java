package com.rohit.ChatApplication.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.protocol.types.Field;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private String eventId;
    private String messageId;
    private String channelId;
    private NotificationType notificationType;
    private String fromUser;
    private String toUser;
    private String content;
    private String source;
    private String createAt;


}
