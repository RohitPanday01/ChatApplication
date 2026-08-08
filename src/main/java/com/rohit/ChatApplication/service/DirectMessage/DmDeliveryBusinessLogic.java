package com.rohit.ChatApplication.service.DirectMessage;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.NotificationType;
import com.rohit.ChatApplication.data.ReadReceipt;
import com.rohit.ChatApplication.data.message.NodeIdentity;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.exception.KafkaDeliveryException;
import com.rohit.ChatApplication.observability.metrics.KafkaMetrics;
import com.rohit.ChatApplication.repository.channel.PrivateMessageJdbcRepository;
import com.rohit.ChatApplication.service.CaffeineCacheResolver;
import com.rohit.ChatApplication.service.MessageSequencing.SnowFlakeIdGenerator;
import com.rohit.ChatApplication.service.Notification.NotificationProducer;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptEmitService;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptProducer;
import com.rohit.ChatApplication.service.RegisterUserSession;
import com.rohit.ChatApplication.service.UserRoutingService;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class DmDeliveryBusinessLogic {

    private final Logger log = LoggerFactory.getLogger(DmDeliveryBusinessLogic.class);

    private final RedisTemplate<String , Object> redisTemplate;
    private final KafkaTemplate<String, Object > kafkaTemplate;
    private final RegisterUserSession registerUserSession;
    private final ObjectMapper objectMapper;
    private final ReadReceiptProducer readReceiptProducer;
    private final NodeIdentity nodeIdentity;
    private final NotificationProducer notificationProducer;
    private final ReadReceiptEmitService readReceiptEmitService;
    private final KafkaMetrics kafkaMetrics;
    private final SnowFlakeIdGenerator snowFlakeIdGenerator;
    private final CaffeineCacheResolver caffeineCacheResolver;
    private final PrivateMessageJdbcRepository privateMessageJdbcRepository;
    private final UserRoutingService userRoutingService;
    private final LocalSessionDelivery localSessionDelivery;

    public DmDeliveryBusinessLogic(@Qualifier("chatPubSubTemplate") RedisTemplate<String , Object> redisTemplate,
                              KafkaTemplate<String, Object> kafkaTemplate,
                              RegisterUserSession registerUserSession,
                              ObjectMapper objectMapper,
                              NotificationProducer notificationProducer,
                              ReadReceiptProducer readReceiptProducer,NodeIdentity nodeIdentity,
                                   ReadReceiptEmitService readReceiptEmitService , KafkaMetrics kafkaMetrics,
                                   SnowFlakeIdGenerator snowFlakeIdGenerator,
                                   CaffeineCacheResolver caffeineCacheResolver,
                                   PrivateMessageJdbcRepository privateMessageJdbcRepository,
                                   UserRoutingService userRoutingService, LocalSessionDelivery localSessionDelivery){
        this.redisTemplate = redisTemplate ;
        this.kafkaTemplate = kafkaTemplate;
        this.registerUserSession = registerUserSession;
        this.objectMapper = objectMapper;
        this.notificationProducer = notificationProducer;
        this.readReceiptProducer = readReceiptProducer;
        this.nodeIdentity = nodeIdentity;
        this.readReceiptEmitService = readReceiptEmitService;
        this.kafkaMetrics = kafkaMetrics;
        this.snowFlakeIdGenerator = snowFlakeIdGenerator;
        this.caffeineCacheResolver = caffeineCacheResolver;
        this.privateMessageJdbcRepository = privateMessageJdbcRepository;
        this.userRoutingService = userRoutingService;
        this.localSessionDelivery = localSessionDelivery;
    }



    public void handle(List<PrivateMessageDto> messages)  {

        String currentServerNodeId = nodeIdentity.getNodeId();
        Map<String , List<PrivateMessageDto>> localDeliveries = new HashMap<>();
        Map<String, List<PrivateMessageDto>> remoteNodeMap = new HashMap<>();

        for(PrivateMessageDto message : messages){
            Long messageSeq = snowFlakeIdGenerator.generateId();
            message.setMessage_seq(messageSeq);

           Long prevMessageSeq =
                   caffeineCacheResolver.handlePrevMessageSeq(message.getChannel() ,messageSeq);

           message.setPrevMessage_seq(prevMessageSeq);

        }

        privateMessageJdbcRepository.consumeBatch(messages);
        log.debug("Successfully batch-inserted {} records to Database.", messages.size());



        for(PrivateMessageDto messageDto : messages){
           String receiverUsername =  messageDto.getTo().getUsername();
           String nodeId = userRoutingService.getUserLocation(receiverUsername);

           if(nodeId == null ){
               handleOfflineUser(messageDto);
           }

           if(currentServerNodeId.equals(nodeId)){
               localDeliveries.computeIfAbsent(receiverUsername, k->new ArrayList<>()).add(messageDto);
           }else{
               remoteNodeMap.computeIfAbsent(nodeId, k -> new ArrayList<>()).add(messageDto);
           }
        }
        localSessionDelivery.handleLocalDelivery(localDeliveries);

        pipelineRedisPubSub(remoteNodeMap);

    }



    private void pipelineRedisPubSub(Map<String, List<PrivateMessageDto>> remoteNodeMap) {
        if (remoteNodeMap.isEmpty()) return;

        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Map.Entry<String, List<PrivateMessageDto>> entry : remoteNodeMap.entrySet()) {
                    String targetNodeId = entry.getKey();
                    byte[] topicBytes = ("Dm:"+ targetNodeId).getBytes();

                    for (PrivateMessageDto dto : entry.getValue()) {
                        try {
                            byte[] payload = objectMapper.writeValueAsBytes(dto);
                            connection.publish(topicBytes, payload);
                        } catch (Exception e) {
                            log.error("Failed to serialize messageId: {} for Redis Pub/Sub", dto.getId(), e);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error executing Redis Pub/Sub pipeline", e);
        }
    }


    private void handleOfflineUser(PrivateMessageDto messageDto){

        log.info("User offline: {}", messageDto.getTo().getUsername());

        try{
            sendNotification(messageDto);
        }catch(Exception e){
            log.error("Trying to send notification to notification producer failed messageId={}", messageDto.getId(), e);

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


        try{
            ReadReceipt readReceipt =  readReceiptEmitService.emitDeliveredReceipt(messageDto);

            readReceiptProducer.sendReadReceipt(readReceipt);

            kafkaMetrics.incrementReadReceiptProduced();

            log.info("---->>>>>>>>>Read Reciept also sent");

        } catch (Exception e) {
            log.error("Read Receipt Delivered event  failed messageId={}", messageDto.getId(), e);
        }
    }

    public void interNodeDmDelivery(PrivateMessageDto messageDto , String receiverNodeId){

        String receiverName = messageDto.getTo().getUsername();

        try{

            kafkaTemplate.send("inter-node-dm-delivery",
                    receiverNodeId, messageDto).join();

            kafkaMetrics.incrementForwardProduced();


        } catch (Exception e) {

            log.error("Inter-node delivery failed", e);

            throw new KafkaDeliveryException("Inter-node delivery failed", e);
        }

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
