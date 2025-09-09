package com.rohit.ChatApplication.service.DirectMessage;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.ReceiptType;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptProducer;
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
    private final ReadReceiptProducer  readReceiptProducer;

    private final KafkaTemplate<String ,Object> template;

    public DirectMessageProducer(@Value("${chat.topics.dm-delivery}")String deliveryTopic ,
                                 @Value("${chat.topics.dm-persist}")String persistenceTopic ,
                                 KafkaTemplate<String, Object> template, ReadReceiptProducer readReceiptProducer){
        this.deliveryTopic = deliveryTopic ;
        this.persistenceTopic = persistenceTopic ;
        this.readReceiptProducer = readReceiptProducer;
        this.template = template;

    }

    public CompletableFuture<Void> sendDirectMessage(PrivateMessageDto dm ){
        String keyForDelivery = dm.getTo().getId();
        String keyForPersistence = dm.getChannel().toString();


//        template.executeInTransaction(operations -> {
//            operations.send(deliveryTopic, keyForDelivery, dm);
//            operations.send(persistenceTopic, keyForPersistence, dm);
//            return true;
//        });

        CompletableFuture<Void> realTime = template.send(deliveryTopic, keyForDelivery, dm)
                .thenAccept((result)-> {

                    readReceiptProducer.sendReadReceipt(dm.getId().toString() ,dm.getChannel().toString(),
                            dm.getFrom().getUsername(), dm.getTo().getUsername(), ReceiptType.SENT,
                            Instant.now());

                })
                .exceptionally(ex -> {

                    readReceiptProducer.sendReadReceipt(dm.getId().toString() ,dm.getChannel().toString(),
                            dm.getFrom().getUsername(), dm.getTo().getUsername(), ReceiptType.SENT,
                            Instant.now());

                    throw new CompletionException(ex);
                });



        CompletableFuture<Void> persistence = template.send(persistenceTopic ,keyForPersistence ,dm )
                .thenAccept((result)->{

        }).exceptionally(ex -> {throw new CompletionException(ex);});


        return CompletableFuture.allOf(realTime ,persistence);
    }


}
