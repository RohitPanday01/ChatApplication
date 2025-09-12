package com.rohit.ChatApplication.Batch.Message.PrivateMessage;

import com.rohit.ChatApplication.entity.PrivateMessage;
import com.rohit.ChatApplication.repository.message.PrivateMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Slf4j
@RequiredArgsConstructor
public class PrivateMessageBatcher {

    private final PrivateMessageRepository  messageRepository;

    private final  ConcurrentLinkedQueue<PrivateMessage> buffer = new ConcurrentLinkedQueue<>();

    private static final int BATCH_SIZE = 500;

    public void addMessages(List<PrivateMessage>messages){
        buffer.addAll(messages);
        if(buffer.size() >= BATCH_SIZE){
            flush();
        }
    }

    @Scheduled(fixedRate = 5000)
    public void scheduledFlush() {
        flush();
    }

    private void flush(){
        List<PrivateMessage> batch = new ArrayList<>();
        while (!buffer.isEmpty() && batch.size() < BATCH_SIZE) {
            PrivateMessage msg = buffer.poll();
            if (msg != null) {
                batch.add(msg);
            }
        }

        if (batch.isEmpty()) return;

        try{
            List<PrivateMessage> toSave = batch.stream()
                    .filter(message -> !messageRepository.existsByMessageId(message.getMessageId()))
                    .toList();

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
