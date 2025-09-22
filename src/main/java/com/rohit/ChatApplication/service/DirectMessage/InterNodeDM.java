package com.rohit.ChatApplication.service.DirectMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.NotificationType;
import com.rohit.ChatApplication.data.ReceiptType;
import com.rohit.ChatApplication.data.message.NodeIdentity;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.service.Notification.NotificationProducer;


import com.rohit.ChatApplication.service.RegisterUserSession;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptProducer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Component
public class InterNodeDM {

    private final Logger log = LoggerFactory.getLogger(InterNodeDM.class);

    private final RedisTemplate<String , Object> redisTemplate;
    private final KafkaTemplate<String, Object > kafkaTemplate;
    private final RegisterUserSession registerUserSession;
    private final ObjectMapper objectMapper;
    private final ReadReceiptProducer readReceiptProducer;
    private  final NodeIdentity nodeIdentity;

    private final NotificationProducer notificationProducer;

    public InterNodeDM(RedisTemplate<String , Object> redisTemplate,
                       KafkaTemplate<String, Object > kafkaTemplate,
                       RegisterUserSession registerUserSession,
                       ObjectMapper objectMapper,ReadReceiptProducer readReceiptProducer,
                       NotificationProducer notificationProducer, NodeIdentity nodeIdentity){
        this.redisTemplate = redisTemplate ;
        this.kafkaTemplate = kafkaTemplate;
        this.registerUserSession = registerUserSession;
        this.readReceiptProducer = readReceiptProducer;
        this.objectMapper = objectMapper;
        this.notificationProducer = notificationProducer;
        this.nodeIdentity = nodeIdentity;

    }
    @KafkaListener(
            topics = "inter-node-dm-delivery",
            groupId = "#{T(java.util.UUID).randomUUID().toString()}", // unique group per node
            containerFactory = "deliveryContainerFactory"
    )
    public void onInterNodeDelivery(PrivateMessageDto messageDto,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String key, Acknowledgment ack ) {

        String receiverName  = messageDto.getTo().getUsername();
        String receiverId = messageDto.getTo().getId();
        String thisNodeId = nodeIdentity.getNodeId();

        log.info("->>>>>>>>>>>>> inside inter Node DM and this nodeid is: {}" , thisNodeId);

        if(!thisNodeId.equals(key)){
            ack.acknowledge();
            return;
        }

        String dedupKey = String.format("Delivered%s%s",receiverId, LocalDate.now());
        try{

            Double lastSeenScore = redisTemplate.opsForZSet().score("online_users_lastPing", receiverName);

            boolean isOnline = false ;
            if(lastSeenScore != null){
                isOnline = true;
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
                                    messageDto.getFrom().getUsername(), messageDto.getTo().getUsername(), ReceiptType.DELIVERED,
                                    Instant.now())
                            .whenComplete((v, ex) -> {
                                if (ex == null) {
                                    ack.acknowledge(); // only ack after success
                                } else {
                                    log.error("ReadReceipt failed , Kafka will retry {}", messageDto.getId(), ex);

                                }
                            });
                }
            }else{
                // send to Notification Topic

                notificationProducer.sendNotification(messageDto.getChannel(), messageDto.getId().toString(),
                                NotificationType.PRIVATE_MESSAGE, messageDto.getFrom() , messageDto.getTo(),
                                messageDto.getContent(), "dm-service" , messageDto.getSentAt() )
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
