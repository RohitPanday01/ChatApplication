package com.rohit.ChatApplication.service.GroupMessage;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.message.GroupMessageDto;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class GroupMessageConsumer {

    private final FanOutService fanOutService;

    private final ThreadPoolExecutor groupWorkerPool;

    public GroupMessageConsumer(@Qualifier("groupWorkerPool") ThreadPoolExecutor groupWorkerPool ,
                                FanOutService fanOutService ){
        this.groupWorkerPool = groupWorkerPool;
        this.fanOutService = fanOutService;
    }



    @KafkaListener(
            topics = "${chat.topics.group-delivery}",
            groupId = "group-message-consumerGroup",
            containerFactory = "GroupMessageDeliveryContainer"
    )
    public void onMessage(ConsumerRecord<String, GroupMessageDto> record, Acknowledgment ack){

        GroupMessageDto msg = record.value();
        if (msg == null) {
            ack.acknowledge();
            return;
        }

        groupWorkerPool.submit(() -> {
            try {
                fanOutService.fanOutGroupMessage(msg);

                // ✅ only ack if success
                ack.acknowledge();

            } catch (Exception ex) {
                log.error("Exception in fanOutService, NOT acking. Kafka will retry.", ex);

                // optionally: push to DLQ here if retries exceed limit
            }
        });




    }
}
