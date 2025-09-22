package com.rohit.ChatApplication.service.DirectMessage;

import com.rohit.ChatApplication.Batch.Message.PrivateMessage.PrivateMessageBatcher;
import com.rohit.ChatApplication.data.message.MessageDto;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.entity.PrivateMessage;
import com.rohit.ChatApplication.repository.message.PrivateMessageRepository;
import com.rohit.ChatApplication.service.GroupMessage.FanOutService;
import com.rohit.ChatApplication.service.message.PrivateMessageServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
public class DMPersistenceListener {
    private final Logger log = LoggerFactory.getLogger(DMPersistenceListener.class);
    private final PrivateMessageRepository privateMessageRepository;
    private final PrivateMessageServiceImpl privateMessageService;
    private final PrivateMessageBatcher batcher;

    public DMPersistenceListener(PrivateMessageRepository privateMessageRepository, PrivateMessageServiceImpl privateMessageService, PrivateMessageBatcher batcher) {
        this.privateMessageRepository = privateMessageRepository;
        this.privateMessageService = privateMessageService;
        this.batcher = batcher;
    }

    @KafkaListener(
            topics = "${chat.topics.dm-persist}",
            groupId = "dm-persistence-svc",
            containerFactory = "persistContainerFactory"
    )
    public  void onMessage(@Payload List<PrivateMessageDto> messages , Acknowledgment ack) {

        try{

           // List<PrivateMessage> entities = messages.stream() .map(privateMessageService::toEntity) .flatMap(Optional::stream) .toList();

            List<PrivateMessage> entities = messages.stream()
                    .map(privateMessageService::toEntity)
                    .flatMap(Optional::stream)
                    .toList();

            log.info("Persisting: {}", entities);
            batcher.addMessages(entities);

            ack.acknowledge();

        }catch(Exception exception){
            log.error("Failed to buffer messages, Kafka will retry", exception);
        }
    }

}
