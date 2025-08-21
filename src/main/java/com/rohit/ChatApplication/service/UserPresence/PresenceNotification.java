package com.rohit.ChatApplication.service.UserPresence;

import com.rohit.ChatApplication.controller.Websocket.PresenceWSHandler;
import com.rohit.ChatApplication.repository.channel.PrivateChannelRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

@Service
@Slf4j
public class PresenceNotification {
    private final RedisTemplate<String, Object> redisTemplate;
    private final PresenceWSHandler presenceWSHandler;
    private final PrivateChannelRepository privateChannelRepository;

    public PresenceNotification(RedisTemplate<String, Object> redisTemplate, PresenceWSHandler presenceWSHandler,
                                PrivateChannelRepository privateChannelRepository) {
        this.redisTemplate = redisTemplate;
        this.presenceWSHandler = presenceWSHandler;
        this.privateChannelRepository = privateChannelRepository;
    }



    public void processPresenceUpdate(String username, String status, String timestamp) {

        Set<String> interestedUsers = privateChannelRepository.findUsersWhoCare(username);

        if ("online".equals(status)) {
            notifyInterestedUsers(interestedUsers, username + " is Online");
        } else {
            String lastSeenKey = "user:" + username + ":lastSeen";
            try {
                redisTemplate.opsForValue().set(lastSeenKey, timestamp, Duration.ofDays(2));
            } catch (Exception e) {
                log.error("not able to update in redis lastSeenKey {}", username, e);
            }

            notifyInterestedUsers(interestedUsers, timestamp);
        }
    }

    private void notifyInterestedUsers(Set<String> interestedUsers, String messageText) {
        for (String user : interestedUsers) {
            WebSocketSession session = presenceWSHandler.getSession(user);

            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(messageText));
                } catch (IOException e) {

                    log.error("Error sending to user {}", user, e);
                }
            }
        }
    }


}
