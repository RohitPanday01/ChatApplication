package com.rohit.ChatApplication.service.DirectMessage;

import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.entity.PrivateMessage;
import com.rohit.ChatApplication.exception.ChannelDoesNotExist;
import com.rohit.ChatApplication.exception.InvalidOperation;
import com.rohit.ChatApplication.exception.UserDoesNotExist;
import com.rohit.ChatApplication.repository.message.PrivateMessageRepository;
import com.rohit.ChatApplication.service.message.PrivateMessageServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class DMPersistenceListener {
    private final PrivateMessageRepository privateMessageRepository;
    private final PrivateMessageServiceImpl privateMessageService;

    @KafkaListener(
            topics = "${chat.topics.dm-persist}",
            groupId = "dm-persistence-svc",
            containerFactory = "persistContainerFactory"
    )
    public  void onMessage(@Payload PrivateMessageDto dm , Acknowledgment ack) {

        try{
//            privateMessageService.createMessage(dm.getFrom().getId(),
//                    dm.getChannel().toString() ,
//                    dm.getContent(),
//                    dm.getMessageType());


            ack.acknowledge();

        }catch(Exception exception){
            log.error("not able to create and save message");
        }
    }

}
