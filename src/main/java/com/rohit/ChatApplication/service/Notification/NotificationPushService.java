package com.rohit.ChatApplication.service.Notification;

import com.rohit.ChatApplication.data.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationPushService {

    @Async("notificationExecutor")
    public CompletableFuture<Void> push(NotificationEvent event){

        try {
            // TODO: real push implementation
            System.out.println("Pushing to user " + event.getFromUser() + ": " + event.getNotificationType());
            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;
        }

    }

}
