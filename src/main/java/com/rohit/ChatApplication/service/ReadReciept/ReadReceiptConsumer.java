package com.rohit.ChatApplication.service.ReadReciept;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.ReadReceipt;
import com.rohit.ChatApplication.service.RegisterUserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class ReadReceiptConsumer {

    private final RegisterUserSession  registerUserSession;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String , ReadReceipt > kafkaTemplate;

    public ReadReceiptConsumer(RegisterUserSession registerUserSession, ObjectMapper objectMapper,
                               KafkaTemplate<String, ReadReceipt> kafkaTemplate){
        this.registerUserSession = registerUserSession;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }
    @KafkaListener(
            topics = "${chat.topics.read-receipt}",
            groupId = "readReceipt-group",
            containerFactory = "readReceiptContainerFactory"
    )
    public void onMessage(ReadReceipt readReceipt , Acknowledgment ack){

        try{
            WebSocketSession session = registerUserSession.getUserSessionInLocalNodeMap(readReceipt.getSender());


            if(session.isOpen()){
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(readReceipt)));
                ack.acknowledge();
            }else{

                kafkaTemplate.send("chat.topics.inter-node-read-receipt", readReceipt.getSender() , readReceipt)
                        .whenComplete((result ,ex)->{
                            if(ex == null){
                                ack.acknowledge();
                            }else{
                                log.error("failed to send to interNode topic {}, will retry", readReceipt.getMessageId() , ex);
                            }
                        });

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
