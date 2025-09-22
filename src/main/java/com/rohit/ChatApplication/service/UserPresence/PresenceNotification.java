package com.rohit.ChatApplication.service.UserPresence;

import com.rohit.ChatApplication.controller.Websocket.PresenceWSHandler;
import com.rohit.ChatApplication.repository.channel.PrivateChannelRepository;
import com.rohit.ChatApplication.service.RegisterUserSession;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private final RedisTemplate<String, String> redisTemplate;
    private final PresenceWSHandler presenceWSHandler;
    private final PrivateChannelRepository privateChannelRepository;
    private final RegisterUserSession registerUserSession;

    public PresenceNotification( @Qualifier("redisStringTemplate") RedisTemplate<String, String> redisTemplate,
                                PresenceWSHandler presenceWSHandler,
                                PrivateChannelRepository privateChannelRepository, RegisterUserSession registerUserSession) {
        this.redisTemplate = redisTemplate;
        this.presenceWSHandler = presenceWSHandler;
        this.privateChannelRepository = privateChannelRepository;
        this.registerUserSession = registerUserSession;
    }



    public void processPresenceUpdate(String username, String status, String timestamp) {

        Set<String> interestedUsers = privateChannelRepository.findUsersWhoCare(username);

        if(interestedUsers.isEmpty()){
            log.error("interestedUsers is empty for username: {}", username );
            return;
        }

        if ("online".equals(status) ) {
            notifyInterestedUsers(interestedUsers, username + " is Online"+ timestamp);
        } else {
            String lastSeenKey = "user:" + username + ":lastSeen";
            try {
                redisTemplate.opsForValue().set(lastSeenKey, timestamp, Duration.ofMinutes(20));
            } catch (Exception e) {
                log.error("not able to update in redis lastSeenKey {}", username, e);
            }

            notifyInterestedUsers(interestedUsers, username + " is Offline"+ timestamp);
        }
    }

    private void notifyInterestedUsers(Set<String> interestedUsers, String messageText) {
        for (String user : interestedUsers) {
            log.info("->>>>>notifying online user in method notifyInterestedUsers: {}",  user);
            WebSocketSession session = registerUserSession.getUserSessionInLocalNodeMap(user);

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
