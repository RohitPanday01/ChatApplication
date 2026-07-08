package com.rohit.ChatApplication.service.DirectMessage;



import com.rohit.ChatApplication.data.message.PrivateMessageDto;

import com.rohit.ChatApplication.observability.metrics.KafkaMetrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;


@Component
public class DMDeliveryListener {

    private final Logger log = LoggerFactory.getLogger(DMDeliveryListener.class);

    private final DmDeliveryBusinessLogic dmDelivery;
    private final KafkaMetrics kafkaMetrics;


    public DMDeliveryListener(DmDeliveryBusinessLogic dmDelivery, KafkaMetrics kafkaMetrics){
        this.dmDelivery = dmDelivery;
        this.kafkaMetrics = kafkaMetrics;
    }

    @KafkaListener(
            topics = "${chat.topics.dm-delivery}",
            groupId = "private-message-cg",
            containerFactory = "deliveryContainerFactory"
    )
    public void onMessage(@Payload PrivateMessageDto messageDto,
                          Acknowledgment ack) {
        long start = Instant.now().toEpochMilli();

        try{
            dmDelivery.handle(messageDto);
            ack.acknowledge();
            kafkaMetrics.incrementConsumerSuccess();

        } catch (Exception e) {
            log.error("Delivery failed messageId={}", messageDto.getId(), e);
            kafkaMetrics.incrementConsumerFailure();
            throw e;
        }finally {
            kafkaMetrics.getKafkaConsumerProcessing().record(
                    Instant.now().toEpochMilli() - start, TimeUnit.MILLISECONDS );

        }
  }

 }

