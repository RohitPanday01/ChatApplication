package com.rohit.ChatApplication.service.DirectMessage;

import com.rohit.ChatApplication.Batch.Message.PrivateMessage.PrivateMessageBatcher;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.entity.PrivateMessage;
import com.rohit.ChatApplication.repository.message.PrivateMessageRepository;
import com.rohit.ChatApplication.service.message.PrivateMessageServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DMPersistenceListener {
    private final PrivateMessageRepository privateMessageRepository;
    private final PrivateMessageServiceImpl privateMessageService;
    private final PrivateMessageBatcher batcher;

    @KafkaListener(
            topics = "${chat.topics.dm-persist}",
            groupId = "dm-persistence-svc",
            containerFactory = "persistContainerFactory"
    )
    public  void onMessage(List<PrivateMessageDto> messages , Acknowledgment ack) {

        try{

            List<PrivateMessage> entities  = messages.stream()
                    .map(privateMessageService::toEntity)
                    .flatMap(Optional::stream)
                    .toList();

            batcher.addMessages(entities);

            ack.acknowledge();

        }catch(Exception exception){
            log.error("Failed to buffer messages, Kafka will retry", exception);
        }
    }

}
