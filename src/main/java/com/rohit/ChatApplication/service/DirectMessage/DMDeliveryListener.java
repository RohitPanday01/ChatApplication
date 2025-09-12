package com.rohit.ChatApplication.service.DirectMessage;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.NotificationType;
import com.rohit.ChatApplication.data.ReadReceipt;
import com.rohit.ChatApplication.data.ReceiptType;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.service.Notification.NotificationProducer;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptProducer;
import com.rohit.ChatApplication.service.RegisterUserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Component
@Slf4j
public class DMDeliveryListener {

    private final RedisTemplate<String , Object> redisTemplate;
    private final KafkaTemplate<String, Object > kafkaTemplate;
    private final RegisterUserSession registerUserSession;
    private final ObjectMapper objectMapper;
    private final ReadReceiptProducer readReceiptProducer;


    private final NotificationProducer notificationProducer;

    public DMDeliveryListener(RedisTemplate<String , Object> redisTemplate,
                              KafkaTemplate<String, Object> kafkaTemplate,
                              RegisterUserSession registerUserSession,
                              ObjectMapper objectMapper,
                              NotificationProducer notificationProducer,
                              ReadReceiptProducer readReceiptProducer){
        this.redisTemplate = redisTemplate ;
        this.kafkaTemplate = kafkaTemplate;
        this.registerUserSession = registerUserSession;
        this.objectMapper = objectMapper;
        this.notificationProducer = notificationProducer;
        this.readReceiptProducer = readReceiptProducer;
    }


    @KafkaListener(
            topics = "${chat.topics.dm-delivery}",
            groupId = "private-message-group",
            containerFactory = "deliveryContainerFactory"
    )
    public void onMessage(@Payload PrivateMessageDto messageDto,
                          Acknowledgment ack) {
        String receiverName  = messageDto.getTo().getUsername();
        String receiverId = messageDto.getTo().getId();
        String dedupKey = String.format("Delivered%s%s",receiverId, messageDto.getId());

        try{


            Double lastSeenScore = redisTemplate.opsForZSet().score("online_users_lastPing", receiverName);

            boolean isOnline = false ;
            if(lastSeenScore != null){
                long now = System.currentTimeMillis();
                long ttlMillis = 10_000;
                isOnline = lastSeenScore >= (now - ttlMillis);
            }

            if(isOnline){
                WebSocketSession session = registerUserSession.getUserSessionInLocalNodeMap(receiverName);
                if(session!= null && session.isOpen()){



                    long added = redisTemplate.opsForSet().add(dedupKey,messageDto.getId().toString());

                    redisTemplate.expire(dedupKey , Duration.ofDays(1));

                    if(added > 0 ){
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(messageDto)));
                    }

                    readReceiptProducer.sendReadReceipt(messageDto.getId().toString() ,messageDto.getChannel().toString(),
                            messageDto.getFrom().getUsername(), messageDto.getTo().getUsername(),ReceiptType.SEEN,
                            Instant.now())
                            .whenComplete((v, ex) -> {
                                if (ex == null) {
                                    ack.acknowledge(); // only ack after success
                                } else {
                                    log.error("ReadReceipt failed , Kafka will retry {}", messageDto.getId(), ex);

                                }
                             });

                }else {
                    // user is Online on But Other Node
                    kafkaTemplate.send("inter-node-dm-delivery", receiverName, messageDto)
                            .whenComplete((result, ex) -> {
                        if (ex == null) {
                            ack.acknowledge();
                        } else {
                            log.error("not  able to publish to itnernode delivery kafka will retry");
                        }
                    });
                }
            }else{
                // send to Notification Topic

                notificationProducer.sendNotification(messageDto.getChannel(), messageDto.getId().toString(),
                        NotificationType.PRIVATE_MESSAGE, messageDto.getFrom() , messageDto.getTo(),
                        messageDto.getContent(), "dm-service" , messageDto.getCreateAt() )
                        .whenComplete((v, ex) -> {
                            if (ex == null) {
                                ack.acknowledge(); // only ack after success
                            } else {
                                log.error(" failed  to send notification, Kafka dm delivery consumer" +
                                        " will retry {}", messageDto.getId(), ex);

                            }
                        });
            }

        } catch (Exception e) {
            redisTemplate.delete(dedupKey);
            log.error("Failed to deliver message, Kafka will retry", e);
        }
  }

 }

