package com.rohit.ChatApplication.service.DirectMessage;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.ReadReceipt;
import com.rohit.ChatApplication.data.ReceiptType;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptEmitService;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptProducer;
import com.rohit.ChatApplication.service.RegisterUserSession;
import org.apache.kafka.common.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class DirectMessageProducer {

    Logger logger = LoggerFactory.getLogger(DirectMessageProducer.class);

    private final  String deliveryTopic;
    private final String persistenceTopic;
    private final ReadReceiptProducer readReceiptProducer;


    private final KafkaTemplate<String , Object> kafkaTemplate;

    private final ReadReceiptEmitService receiptEmitService;
    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final RegisterUserSession registerUserSession ;
    private final ObjectMapper objectMapper;

    public DirectMessageProducer(@Value("${chat.topics.dm-delivery}")String deliveryTopic ,
                                 @Value("${chat.topics.dm-persist}")String persistenceTopic ,
                                 KafkaTemplate<String, Object> kafkaTemplate,
                                 ReadReceiptProducer readReceiptProducer,
                                 ReadReceiptEmitService readReceiptEmitService,
                                 RegisterUserSession registerUserSession,
                                 ObjectMapper objectMapper){
        this.deliveryTopic = deliveryTopic ;
        this.persistenceTopic = persistenceTopic ;
        this.readReceiptProducer = readReceiptProducer;
        this.kafkaTemplate = kafkaTemplate;
        this.receiptEmitService = readReceiptEmitService;
        this.registerUserSession = registerUserSession;
        this.objectMapper = objectMapper;
    }

    public void sendDirectMessage(PrivateMessageDto dm ){

        String keyForDelivery = dm.getTo().getId();
        String keyForPersistence = dm.getChannel().toString();

//        transactionalKafkaTemplate.executeInTransaction(operations -> {
//                    operations.send(deliveryTopic, keyForDelivery, dm);
//                    operations.send(persistenceTopic, keyForPersistence, dm);
//                    return null;
//                });

        kafkaTemplate.send(deliveryTopic, keyForPersistence ,dm )
                .whenCompleteAsync((result , throwable)-> {

                    if(throwable == null){
                        PrivateMessageDto dto = (PrivateMessageDto)result.getProducerRecord().value();
                        int  partition = result.getRecordMetadata().partition();
                        long offset = result.getRecordMetadata().offset();


                        logger.info("Chat delivered to partition {} at offset {}", partition, offset);

                       String messageSeq = String.valueOf(dto.getMessage_seq());
                       String senderId = dto.getFrom().getUsername();

                       ReadReceipt readReceipt =
                               new ReadReceipt(null , messageSeq , dto.getChannel().toString(), senderId,
                                       ReceiptType.SENT, dto.getTo().getUsername() );

                       WebSocketSession session = registerUserSession.getUserSessionInLocalNodeMap(dto.getFrom().getUsername());

                        if (session != null && session.isOpen()) {
                            try{
                                String json = objectMapper.writeValueAsString(readReceipt);
                                session.sendMessage(new TextMessage(json));

                            } catch (Exception e) {
                                logger.info("exception occurred at while sending Sent Read receipt through DM-producer ");
                            }

                        } else {
                            logger.warn("Could not dispatch ACK. User {} disconnected while Kafka was processing.", senderId);
                        }

                    }

                    // FAILURE PATH: Unwrap CompletionException if wrapped by Java's reactive layers
                    Throwable cause = (throwable instanceof CompletionException) ? throwable.getCause() : throwable;
                    long failedId = dm.getMessage_seq();
                    String senderId = dm.getFrom().getUsername();

                    // CATEGORY 1 & 2: Transient Network, Timeouts, or ISR Collapses
                    if (cause instanceof TimeoutException ||
                            cause instanceof NetworkException ||
                            cause instanceof NotEnoughReplicasException) {

                        logger.error("[INFRA_FAILURE] Kafka tracking failed for message ID: {} and senderName: {} . Evacuating to local NVMe disk.", failedId, senderId, cause);

                        // Stash to local RocksDB or file system. Do NOT notify client; let client-side sync/timer handle it.
                    }

                    // CATEGORY 3: Structural Violations / Poison Pills
                    else if (cause instanceof RecordTooLargeException || cause instanceof InvalidTopicException) {
                        logger.error("[POISON_PILL] Message ID: {} and senderName: {} rejected due to structural configuration violation.", failedId, senderId, cause);



                        // Proactively kill the client's clock icon so they aren't waiting for a timeout

                    }

                    // CATEGORY 4: Security ACL / Authentication Disasters
                    else if (cause instanceof TopicAuthorizationException || cause instanceof SaslAuthenticationException) {
                        logger.error("[FATAL_AUTH] Monolith has lost credentials to write to Kafka cluster!", cause);

                    }

                    // CATCH-ALL: Unexpected Runtime Anomalies
                    else {
                        logger.error("[UNKNOWN_PRODUCER_ERROR] Message ID: {} and senderName:{} encountered untriaged failure.", failedId, senderId, cause);

                    }


                }, virtualThreadExecutor);

//        ReadReceipt sentEvent = receiptEmitService.emitSentReceipt(dm);
//
//         readReceiptProducer.sendReadReceipt(sentEvent);
//
    }



}
