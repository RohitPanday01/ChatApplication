package com.rohit.ChatApplication.service.GroupMessage;

import com.rohit.ChatApplication.data.message.GroupMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Component
@Slf4j
public class GroupMessageProducer {

    private final String groupMessageDeliveryTopic;
    private final KafkaTemplate<String ,Object> kafkaTemplate;

    public GroupMessageProducer(@Value("${chat.topics.group-delivery}")String groupMessageDeliveryTopic ,
                                KafkaTemplate<String, Object> kafkaTemplate){
        this.groupMessageDeliveryTopic = groupMessageDeliveryTopic;
        this.kafkaTemplate = kafkaTemplate;
    }


    public CompletableFuture<Void> sendMessage(GroupMessageDto groupMessageDto){

        String key = groupMessageDto.getChannel().toString();

        return kafkaTemplate.send(groupMessageDeliveryTopic , key , groupMessageDto)
                .thenAccept((result)->{
                    log.debug("Successfully sent group message to topic {}",  groupMessageDto.getId() );
                }).exceptionally((ex) -> {
                    log.error("failed to sent group message to topic {}",  groupMessageDto.getId() );
                    throw new CompletionException(ex);
                });


    }

}
