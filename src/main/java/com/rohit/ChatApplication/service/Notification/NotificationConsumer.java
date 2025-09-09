package com.rohit.ChatApplication.service.Notification;

import com.rohit.ChatApplication.data.NotificationEvent;
import com.rohit.ChatApplication.data.ReadReceipt;
import com.rohit.ChatApplication.data.ReceiptType;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;


import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private  final NotificationPushService notificationPushService;
    private final RedisTemplate<String , Object> redisTemplate;
    private final ReadReceiptProducer readReceiptProducer;
    private  static final String DEDUP_KEY_PREFIX = "notification:processed:";
    private static final Duration DEDUP_TTL = Duration.ofDays(1);

    @KafkaListener(
            topics = "${chat.topics.dm-notify}",
            groupId = "notification-group",
            containerFactory = "notificationContainerFactory"
    )
    public  void onMessage(NotificationEvent event , Acknowledgment ack){

        String dedupKey = DEDUP_KEY_PREFIX + event.getEventId();
        Boolean isAlready = redisTemplate.hasKey(dedupKey);
        if(Boolean.TRUE.equals(isAlready)){
            ack.acknowledge();
            return;
        }

        try{
            Boolean set = redisTemplate.opsForValue().setIfAbsent(dedupKey ,"1", DEDUP_TTL);
            if (Boolean.FALSE.equals(set)) {
                ack.acknowledge();
                return;
            }

            CompletableFuture<Void> work = dispatch(event).thenCompose((v)->
                    readReceiptProducer.sendReadReceipt(event.getMessageId() ,event.getChannelId() , event.getFromUser(),
                            event.getToUser(), ReceiptType.DELIVERED , Instant.now() ))
                    .whenComplete((v,ex)->{
                        if(ex != null){
                            ack.acknowledge();
                        }else{
                            try {
                                redisTemplate.delete(dedupKey);
                            } catch (Exception e) {
                                log.error("Failed deleting dedup key after failure", e);
                            }
                            // do not ack -> Kafka will retry or error handler will move to DLT
                            log.error("Dispatch failed for notification {}, will retry", event.getEventId(), ex);
                        }
                    });
        } catch (Exception e) {
            try {
                redisTemplate.delete(dedupKey);
            } catch (Exception ignore){}
            throw new RuntimeException(e); // let error handler handle retries
        }

    }

    private CompletableFuture<Void> dispatch(NotificationEvent event){
        return notificationPushService.push(event);
    }

}
