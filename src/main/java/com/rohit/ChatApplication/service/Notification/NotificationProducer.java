package com.rohit.ChatApplication.service.Notification;

import com.rohit.ChatApplication.data.NotificationEvent;
import com.rohit.ChatApplication.data.NotificationType;
import com.rohit.ChatApplication.data.UserPublicProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class NotificationProducer {

    private KafkaTemplate<String , NotificationEvent> kafkaTemplate;

    @Value("${chat.topics.dm-notify}")
    private String notificationTopic;

    public CompletableFuture<Void> sendNotification(UUID channelId , String messageId, NotificationType type , UserPublicProfile from,
                                 UserPublicProfile to ,String content ,String source ,String createAt){
        NotificationEvent event =  NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .messageId(messageId)
                .channelId(channelId.toString())
                .notificationType(type)
                .fromUser(from.getUsername())
                .toUser(to.getUsername())
                .content(content)
                .source(source)
                .createAt(createAt)
                .build();



       return kafkaTemplate.send(notificationTopic ,channelId.toString() ,event)
                .thenAccept((result)->{
                    log.debug(" Successfully sent read receipt for message {}", messageId);

                }).exceptionally((ex)->{
                    log.error("Failed to send notificationTopic for {}. Will bubble up.",
                            messageId, ex);
                    throw new CompletionException(ex);
                });


    }
}
