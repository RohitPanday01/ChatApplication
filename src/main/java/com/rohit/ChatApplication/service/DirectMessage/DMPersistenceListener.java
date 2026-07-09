package com.rohit.ChatApplication.service.DirectMessage;

import com.rohit.ChatApplication.Batch.Message.PrivateMessage.PrivateMessageBatcher;
import com.rohit.ChatApplication.data.ErrorMessageResponse;
import com.rohit.ChatApplication.data.message.MessageDto;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.entity.PrivateMessage;
import com.rohit.ChatApplication.exception.ChannelDoesNotExist;
import com.rohit.ChatApplication.exception.UserDoesNotExist;
import com.rohit.ChatApplication.repository.message.PrivateMessageRepository;
import com.rohit.ChatApplication.service.GroupMessage.FanOutService;
import com.rohit.ChatApplication.service.MessageSequencing.SequenceService;
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
import java.util.*;
import java.util.stream.Collectors;


@Component
public class DMPersistenceListener {
    private final Logger log = LoggerFactory.getLogger(DMPersistenceListener.class);
    private final PrivateMessageRepository privateMessageRepository;
    private final PrivateMessageServiceImpl privateMessageService;
    private final PrivateMessageBatcher batcher;
    private final SequenceService sequenceService;

    public DMPersistenceListener(PrivateMessageRepository privateMessageRepository,
                                 PrivateMessageServiceImpl privateMessageService,
                                 PrivateMessageBatcher batcher,
                                 SequenceService sequenceService) {
        this.privateMessageRepository = privateMessageRepository;
        this.privateMessageService = privateMessageService;
        this.batcher = batcher;
        this.sequenceService = sequenceService;
    }

    @KafkaListener(
            topics = "${chat.topics.dm-delivery}",
            groupId = "dm-persistence-cg",
            containerFactory = "persistContainerFactory"
    )
    public  void onMessage(@Payload List<PrivateMessageDto> messages , Acknowledgment ack) {

        try{

            List<PrivateMessage> batches = new ArrayList<>();
            for(PrivateMessageDto messageDto : messages){
               PrivateMessage message =
                       privateMessageService.toEntity(messageDto);
               batches.add(message);
            }

            if (!batches.isEmpty()) {
                privateMessageRepository.saveAll(batches);
            }

            ack.acknowledge();

        } catch (UserDoesNotExist | ChannelDoesNotExist e) {
           log.error("Poison message", e);
            ack.acknowledge();

        } catch (Exception e) {
            throw e;
        }
    }

}
