package com.rohit.ChatApplication.service.ReadReciept;


import com.rohit.ChatApplication.data.ReadReceipt;
import com.rohit.ChatApplication.data.ReceiptType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@Slf4j
public class ReadReceiptProducer {

    private final KafkaTemplate<String , Object> kafkaTemplate;

    private final String readReceiptTopic;

    public ReadReceiptProducer(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${chat.topics.read-receipt}")String readReceiptTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.readReceiptTopic = readReceiptTopic;
    }


    public void sendReadReceipt(ReadReceipt event){


          kafkaTemplate.send(readReceiptTopic, event.getChannelId(), event);


    }


}
