package com.rohit.ChatApplication.Batch.Message.PrivateMessage;

import com.rohit.ChatApplication.entity.PrivateChannel;
import com.rohit.ChatApplication.entity.PrivateMessage;
import com.rohit.ChatApplication.repository.channel.PrivateChannelRepository;
import com.rohit.ChatApplication.repository.message.PrivateMessageRepository;
import jakarta.transaction.Transactional;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class PrivateMessageBatcher {
    private final Logger log = LoggerFactory.getLogger(PrivateMessageBatcher.class);

    private final PrivateMessageRepository  messageRepository;
    private final PrivateChannelRepository privateChannelRepository;

    private final  ConcurrentLinkedQueue<PrivateMessage> buffer = new ConcurrentLinkedQueue<>();

    private static final int BATCH_SIZE = 1;


    public PrivateMessageBatcher(PrivateMessageRepository messageRepository , PrivateChannelRepository privateChannelRepository){
        this.messageRepository = messageRepository;
        this.privateChannelRepository = privateChannelRepository;
    }

    public void addMessages(List<PrivateMessage>messages){
        buffer.addAll(messages);
        log.info("---->>>> buffer size in private message batcher :{}" , buffer.size());
        if(buffer.size() >= BATCH_SIZE){
            flush();
        }
    }

    @Scheduled(fixedRate = 5000)
    public void scheduledFlush() {
        flush();
    }

    @Transactional
    private void flush(){
        List<PrivateMessage> batch = new ArrayList<>();
        while (!buffer.isEmpty() && batch.size() < BATCH_SIZE) {
            PrivateMessage msg = buffer.poll();
            if (msg != null) {
                batch.add(msg);
            }
        }

        log.info("Batch size inside flush in Batcher: {}", batch.size());
        if (batch.isEmpty()) return;

        try{
            List<PrivateMessage> toSave = batch.stream()
                    .filter(message -> !messageRepository.existsByMessageId(message.getMessageId()))
                    .toList();
            log.info("saving message to db: {}", toSave.size());
            if (!toSave.isEmpty()) {

                messageRepository.saveAll(toSave);
                log.info(" Flushed {} messages to DB", toSave.size());
            }
        } catch (Exception e) {
            log.error(" Failed to flush messages, re-adding to buffer", e);
            buffer.addAll(batch);
        }
    }


}
