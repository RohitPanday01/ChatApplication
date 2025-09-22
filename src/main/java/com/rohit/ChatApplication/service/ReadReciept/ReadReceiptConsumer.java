package com.rohit.ChatApplication.service.ReadReciept;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.ReadReceipt;
import com.rohit.ChatApplication.service.RegisterUserSession;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Component
public class ReadReceiptConsumer {

    private final Logger log = LoggerFactory.getLogger(ReadReceiptConsumer.class);

    private final RegisterUserSession  registerUserSession;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String , Object > kafkaTemplate;

    public ReadReceiptConsumer(RegisterUserSession registerUserSession, ObjectMapper objectMapper,
                               KafkaTemplate<String, Object> kafkaTemplate){
        this.registerUserSession = registerUserSession;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }
    @KafkaListener(
            topics = "${chat.topics.read-receipt}",
            groupId = "readReceipt-group",
            containerFactory = "readReceiptContainerFactory"
    )
    public void onMessage(ReadReceipt readReceipt , Acknowledgment ack)  {

        try{
            WebSocketSession session = registerUserSession.getUserSessionInLocalNodeMap(readReceipt.getSender());

            log.info("->>>>>>>>>>inside ReadReceipt consumer user session we fetched is: {}", session);

            log.info("0000000000000  000000  session.isOpen(): {}", session.isOpen());
            if(session != null && session.isOpen() ){
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(readReceipt)));
                    log.info("->>>>>>>>>> 00000   0000 read Receipt is pushed to user websocket session: {}", session);
                    ack.acknowledge();
                } catch (Exception e) {

                    log.error("Failed to send read receipt over WebSocket", e);
                    throw new RuntimeException(e);
                }
            }else{

                kafkaTemplate.send("chat.topics.inter-node-read-receipt", readReceipt.getSender() , readReceipt)
                        .whenComplete((result ,ex)->{
                            if(ex == null){
                                ack.acknowledge();
                            }else{

                                log.error("failed to send to interNode topic {}, will retry", readReceipt.getMessageId() , ex);
                                throw new RuntimeException(ex.getMessage());
                            }
                        });

            }

        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

}
