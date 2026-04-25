package com.rohit.ChatApplication.service.DirectMessage;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.NotificationType;
import com.rohit.ChatApplication.data.ReadReceipt;
import com.rohit.ChatApplication.data.message.NodeIdentity;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.service.Notification.NotificationProducer;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptEmitService;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptProducer;
import com.rohit.ChatApplication.service.RegisterUserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;


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
    private final ReadReceiptEmitService readReceiptEmitService;

    public DmDeliveryBusinessLogic(RedisTemplate<String , Object> redisTemplate,
                              KafkaTemplate<String, Object> kafkaTemplate,
                              RegisterUserSession registerUserSession,
                              ObjectMapper objectMapper,
                              NotificationProducer notificationProducer,
                              ReadReceiptProducer readReceiptProducer,NodeIdentity nodeIdentity,
                                   ReadReceiptEmitService readReceiptEmitService){
        this.redisTemplate = redisTemplate ;
        this.kafkaTemplate = kafkaTemplate;
        this.registerUserSession = registerUserSession;
        this.objectMapper = objectMapper;
        this.notificationProducer = notificationProducer;
        this.readReceiptProducer = readReceiptProducer;
        this.nodeIdentity = nodeIdentity;
        this.readReceiptEmitService = readReceiptEmitService;
    }



    public void handle(PrivateMessageDto messageDto)  {


//        String receiverId = messageDto.getTo().getId();
//        String channelId = messageDto.getChannel().toString();
//        String dedupKey = String.format("Delivered%s", channelId);
//
//        long added = redisTemplate.opsForSet().add(dedupKey,
//                messageDto.getId().toString());
//        log.info("---->>>>>>>>>added dedupe info in DM " +
//                "delivery Listerner {} ", added);
//
//        redisTemplate.expire(dedupKey , Duration.ofMinutes(2));
//
//        redisTemplate.opsForValue().setIfPresent(dedupKey ,  messageDto.getId().toString() , Duration.ofMinutes(2));

//        boolean isOnline = isUserOnline(receiverName);
//
//        log.info(" ->>>>>>>>>>>user is Online will try to  send message ,{}",
//                receiverName);

        String receiver = messageDto.getTo().getUsername();

        String receiverNodeId =
                (String) redisTemplate.opsForValue().get("nodeId:" + receiver);

        if (receiverNodeId == null) {
            handleOfflineUser(messageDto);
            return;
        }

        if (nodeIdentity.getNodeId().equals(receiverNodeId)) {
            deliverToLocalSession(messageDto);
        }else {
            interNodeDmDelivery(messageDto);
        }

    }

    private void deliverToLocalSession(PrivateMessageDto messageDto) {

        String receiver = messageDto.getTo().getUsername();

        WebSocketSession session =
                registerUserSession.getUserSessionInLocalNodeMap(receiver);

        if (session == null || !session.isOpen()) {

            handleOfflineUser(messageDto);
            return;
        }

        try {

            session.sendMessage(
                    new TextMessage(objectMapper.writeValueAsString(messageDto)));

            log.info("Message delivered to {}", receiver);

            sendReadReceipt(messageDto);

        } catch (Exception e) {

            log.error("WebSocket delivery failed", e);

            throw new RuntimeException("Delivery failed", e);
        }
    }

//    private void sendWebSocketMessage(PrivateMessageDto messageDto){
//
//        String receiverName = messageDto.getTo().getUsername();
//
//        String receiverNodeId = (String) redisTemplate.opsForValue()
//                .get("nodeId:" + receiverName);
//        log.info("->>>>>>>>user is online on this node; {} ", receiverNodeId);
//
//
//        try{
//            if(receiverNodeId == null) {
//                handleOfflineUser(messageDto);
//                return;
//            }
//
//            if(nodeIdentity.getNodeId().equals(receiverNodeId) ){
//                WebSocketSession session = registerUserSession
//                        .getUserSessionInLocalNodeMap(receiverName);
//                log.info(" ->>>>>>>>>>>user is Online in Local node with" +
//                        " session,{}", session);
//
//                if(session!= null && session.isOpen()){
//
//                        session.sendMessage(new TextMessage(
//                                objectMapper.writeValueAsString(messageDto)));
//                        log.info("---->>>>>>>>>message sent to {} ", receiverName);
//                   return;
//                }
//
//                sendReadReceipt(messageDto);
//            }
//
//            interNodeDmDelivery(messageDto);
//
//        } catch (Exception e) {
//            handleOfflineUser(messageDto);
//            log.warn("Realtime delivery failed for {}. Falling back.", receiverName);
//
//        }
//
//    }
    private void handleOfflineUser(PrivateMessageDto messageDto){

        log.info("User offline: {}", messageDto.getTo().getUsername());

        try{
            sendNotification(messageDto);
        }catch(Exception e){
            log.error("Trying to send notification to notification producer failed messageId={}", messageDto.getId(), e);
            throw new RuntimeException("trying to send notification to notification producer failed", e );
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

            log.info("---->>>>>>>>>Read Reciept also sent");

        } catch (Exception e) {
            log.error("Read Receipt Delivered event  failed messageId={}", messageDto.getId(), e);
            throw new RuntimeException("Delivery Read Receipt failed", e);
        }

    }

    public void interNodeDmDelivery(PrivateMessageDto messageDto){

        String receiverName = messageDto.getTo().getUsername();

        try{
            String receiverNodeId = (String) redisTemplate.opsForValue()
                    .get("nodeId:" + receiverName);
            log.info("->>>>>>>>user is online on other node; {} ", receiverNodeId);

            kafkaTemplate.send("inter-node-dm-delivery",
                    receiverNodeId, messageDto).join();

        } catch (Exception e) {

            log.error("Inter-node delivery failed", e);

            throw new RuntimeException("Inter-node delivery failed", e);
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
