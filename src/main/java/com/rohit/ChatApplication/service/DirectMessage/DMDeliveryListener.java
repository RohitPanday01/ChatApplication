package com.rohit.ChatApplication.service.DirectMessage;



import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.service.Notification.NotificationProducer;
import com.rohit.ChatApplication.service.ReadReciept.ReadReceiptProducer;
import com.rohit.ChatApplication.service.RegisterUserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component

public class DMDeliveryListener {

    private final Logger log = LoggerFactory.getLogger(DMDeliveryListener.class);

    private final DmDeliveryBusinessLogic dmDelivery;


    public DMDeliveryListener(DmDeliveryBusinessLogic dmDelivery){
        this.dmDelivery = dmDelivery;
    }

    @KafkaListener(
            topics = "${chat.topics.dm-delivery}",
            groupId = "private-message-group",
            containerFactory = "deliveryContainerFactory"
    )
    public void onMessage(@Payload PrivateMessageDto messageDto,
                          Acknowledgment ack) {

        try{
            dmDelivery.handle(messageDto);
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Delivery failed messageId={}", messageDto.getId(), e);
            throw e;
        }
  }

 }

