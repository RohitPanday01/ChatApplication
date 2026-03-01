package com.rohit.ChatApplication.service.DirectMessage;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.NotificationType;
import com.rohit.ChatApplication.data.ReceiptType;
import com.rohit.ChatApplication.data.message.NodeIdentity;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.service.Notification.NotificationProducer;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptProducer;
import com.rohit.ChatApplication.service.RegisterUserSession;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Service

public class DmDeliveryBusinessLogic {

    private final Logger log = LoggerFactory.getLogger(DMDeliveryListener.class);

    private final RedisTemplate<String , Object> redisTemplate;
    private final KafkaTemplate<String, Object > kafkaTemplate;
    private final RegisterUserSession registerUserSession;
    private final ObjectMapper objectMapper;
    private final ReadReceiptProducer readReceiptProducer;
    private final NodeIdentity nodeIdentity;
    private final NotificationProducer notificationProducer;

    public DmDeliveryBusinessLogic(RedisTemplate<String , Object> redisTemplate,
                              KafkaTemplate<String, Object> kafkaTemplate,
                              RegisterUserSession registerUserSession,
                              ObjectMapper objectMapper,
                              NotificationProducer notificationProducer,
                              ReadReceiptProducer readReceiptProducer,NodeIdentity nodeIdentity){
        this.redisTemplate = redisTemplate ;
        this.kafkaTemplate = kafkaTemplate;
        this.registerUserSession = registerUserSession;
        this.objectMapper = objectMapper;
        this.notificationProducer = notificationProducer;
        this.readReceiptProducer = readReceiptProducer;
        this.nodeIdentity = nodeIdentity;
    }



    public void handle(PrivateMessageDto messageDto)  {

        String receiverName  = messageDto.getTo().getUsername();
        String receiverId = messageDto.getTo().getId();
        String dedupKey = String.format("Delivered%s%s",receiverId,
                messageDto.getId());

        long added = redisTemplate.opsForSet().add(dedupKey,
                messageDto.getId().toString());
        log.info("---->>>>>>>>>added dedupe info in DM " +
                "delivery Listerner {} ", added);

        redisTemplate.expire(dedupKey , Duration.ofHours(1));

        boolean isOnline = isUserOnline(receiverName);

        log.info(" ->>>>>>>>>>>user is Online will try to  send message ,{}",
                receiverName);

        if(isOnline){
            if(added > 0){
                sendWebSocketMessage(messageDto);
            }
            sendReadReceipt(messageDto);
        }else{
            //send notification
            sendNotification(messageDto);
        }

    }
    private void sendWebSocketMessage(PrivateMessageDto messageDto){

        String receiverName = messageDto.getTo().getUsername();


        WebSocketSession session = registerUserSession
                .getUserSessionInLocalNodeMap(receiverName);
        log.info(" ->>>>>>>>>>>user is Online in Local node with" +
                " session,{}", session);

        if(session!= null && session.isOpen()){

            try{
                session.sendMessage(new TextMessage(
                        objectMapper.writeValueAsString(messageDto)));
                log.info("---->>>>>>>>>message sent to {} ", receiverName);

            } catch (Exception e) {
                throw new RuntimeException("Unable to send message Dm message" +
                        "via websocket{}" + e.getMessage());
            }

        }else{
            interNodeDmDelivery(messageDto);
        }
    }

    private boolean isUserOnline(String username) {
        Double lastSeen = redisTemplate.opsForZSet()
                .score("online_users_lastPing", username);

        if (lastSeen == null) return false;

        long lastSeenMillis = lastSeen.longValue();
        return lastSeenMillis >= (System.currentTimeMillis() - 40_000);
    }

    private void sendReadReceipt(PrivateMessageDto messageDto){

        readReceiptProducer.sendReadReceipt(messageDto.getId().toString() ,messageDto.getChannel().toString(),
                        messageDto.getFrom().getUsername(), messageDto.getTo().getUsername(), ReceiptType.DELIVERED,
                        Instant.now()).join();

        log.info("---->>>>>>>>>Read Reciept also sent");

    }

    public void interNodeDmDelivery(PrivateMessageDto messageDto){

        String receiverName = messageDto.getTo().getUsername();

        String receiverNodeId = (String) redisTemplate.opsForValue()
                .get("nodeId:" + receiverName);
        log.info("->>>>>>>>user is online on other node; {} ", receiverNodeId);

        kafkaTemplate.send("inter-node-dm-delivery",
                receiverNodeId, messageDto).join();

    }

    public void sendNotification(PrivateMessageDto messageDto){
        notificationProducer.sendNotification(messageDto.getChannel(),
                        messageDto.getId().toString(),
                        NotificationType.PRIVATE_MESSAGE,
                        messageDto.getFrom() , messageDto.getTo(),
                        messageDto.getContent(), "dm-service" ,
                        messageDto.getSentAt() )
                .join();


    }


}
