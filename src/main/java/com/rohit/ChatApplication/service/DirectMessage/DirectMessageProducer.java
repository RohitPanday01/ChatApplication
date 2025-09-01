package com.rohit.ChatApplication.service.DirectMessage;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DirectMessageProducer {

    private final  String deliveryTopic;
    private final String persistenceTopic;

    private final KafkaTemplate<String ,Object> template;

    public DirectMessageProducer(@Value("${chat.topics.dm-delivery}")String deliveryTopic ,
                                 @Value("${chat.topics.dm-persist}")String persistenceTopic ,
                                 KafkaTemplate<String, Object> template){
        this.deliveryTopic = deliveryTopic ;
        this.persistenceTopic = persistenceTopic ;

        this.template = template;

    }

    public void sendDirectMessage(PrivateMessageDto dm ){
        String keyForDelivery = dm.getTo().getId();
        String keyForPersistence = dm.getChannel().toString();


        template.executeInTransaction(operations -> {
            operations.send(deliveryTopic, keyForDelivery, dm);
            operations.send(persistenceTopic, keyForPersistence, dm);
            return true;
        });
    }




}
