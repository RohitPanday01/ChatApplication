package com.rohit.ChatApplication.service.DirectMessage;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.ReadReceipt;
import com.rohit.ChatApplication.data.ReceiptType;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptEmitService;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptProducer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class DirectMessageProducer {

    private final  String deliveryTopic;
    private final String persistenceTopic;
    private final ReadReceiptProducer readReceiptProducer;

    private final KafkaTemplate<String, Object> transactionalKafkaTemplate;

    private final ReadReceiptEmitService receiptEmitService;

    public DirectMessageProducer(@Value("${chat.topics.dm-delivery}")String deliveryTopic ,
                                 @Value("${chat.topics.dm-persist}")String persistenceTopic ,
                                 @Qualifier("transactionalKafkaTemplate") KafkaTemplate<String, Object> transactionalKafkaTemplate,
                                 ReadReceiptProducer readReceiptProducer,
                                 ReadReceiptEmitService readReceiptEmitService){
        this.deliveryTopic = deliveryTopic ;
        this.persistenceTopic = persistenceTopic ;
        this.readReceiptProducer = readReceiptProducer;
        this.transactionalKafkaTemplate = transactionalKafkaTemplate;
        this.receiptEmitService = readReceiptEmitService;

    }

    public void sendDirectMessage(PrivateMessageDto dm ){
        String keyForDelivery = dm.getTo().getId();
        String keyForPersistence = dm.getChannel().toString();


        transactionalKafkaTemplate.executeInTransaction(operations -> {
                    operations.send(deliveryTopic, keyForDelivery, dm);
                    operations.send(persistenceTopic, keyForPersistence, dm);
                    return null;
                });
        ReadReceipt sentEvent = receiptEmitService.emitSentReceipt(dm);

         readReceiptProducer.sendReadReceipt(sentEvent);

//
    }



}
