package com.rohit.ChatApplication.service.Notification;

import com.rohit.ChatApplication.data.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service

@RequiredArgsConstructor
public class NotificationPushService {

    private final Logger log = LoggerFactory.getLogger(NotificationPushService.class);

    @Async("notificationExecutor")
    public CompletableFuture<Void> push(NotificationEvent event){

        try {
            // TODO: real push implementation

            log.info(" ->>>>>>>>>>>>.Pushing to user " + event.getFromUser() + ": " + event.getNotificationType());
            return CompletableFuture.completedFuture(null);


        } catch (Exception ex) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            log.info(" ->>>>>>>>>>>>failed in sending Notification in Notification executo " + event.getFromUser() + ": " + event.getNotificationType());
            return failed;
        }

    }

}
