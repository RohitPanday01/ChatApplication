package com.rohit.ChatApplication.service.DirectMessage;


import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class DmDeliveryOrchestratorService {

    Logger log = LoggerFactory.getLogger(DmDeliveryOrchestratorService.class);


    private final Executor executor;
    private final DmDeliveryBusinessLogic deliveryBusinessLogic;


    public DmDeliveryOrchestratorService(@Qualifier("dmDeliveryExecutor") Executor executor,
                                         DmDeliveryBusinessLogic deliveryBusinessLogic){
        this.executor = executor;
        this.deliveryBusinessLogic = deliveryBusinessLogic;
    }

    public void process(PrivateMessageDto messageDto , Acknowledgment ack){

//        CompletableFuture.runAsync(()-> deliveryBusinessLogic.handle(messageDto) ,executor)
//                .thenRun(ack::acknowledge)
//                .exceptionally(ex -> {
//                    log.error("Delivery failed for message {}",
//                            messageDto.getId(), ex);
//                    // do NOT ack → Kafka retry
//                    return null;
//                });
        return;

    }

}
