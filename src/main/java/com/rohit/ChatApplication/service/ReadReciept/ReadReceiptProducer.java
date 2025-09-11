package com.rohit.ChatApplication.service.ReadReciept;


import com.rohit.ChatApplication.data.ReadReceipt;
import com.rohit.ChatApplication.data.ReceiptType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReadReceiptProducer {

    private KafkaTemplate<String , ReadReceipt> kafkaTemplate;

    @Value("${chat.topics.read-receipt}")
    private String readReceiptTopic;


    public CompletableFuture<Void> sendReadReceipt(String messageId , String  channelId, String sender ,
                                             String receiver , ReceiptType type, Instant timestamp ){

        ReadReceipt event = new ReadReceipt(messageId, channelId,sender,type ,receiver,timestamp);


        return kafkaTemplate.send(readReceiptTopic, receiver, event)
                .thenAccept(result -> {
                    log.debug("Successfully sent read receipt to topic {}", messageId);


                }).exceptionally(ex ->{
                    log.error("Failed to send read receipt for {}. Will bubble up.", messageId, ex);
                    throw new CompletionException(ex);
                });


        // to send to readReceipt Persistence topic so the read receipt should save in db async way
//        return f1.thenCompose(result1 ->
//                kafkaTemplate.send(readReceiptPersistenceTopic, receiver, event)
//        ).thenAccept(result2 -> {
//            log.debug("Successfully sent read receipt {} to BOTH topics", messageId);
//        }).exceptionally(ex -> {
//            log.error("Failed to send read receipt {} to both topics. Will bubble up", messageId, ex);
//            throw new CompletionException(ex);
//        });

    }


}
