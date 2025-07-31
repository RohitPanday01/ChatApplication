package com.rohit.ChatApplication.config.WebSocket;

import com.rohit.ChatApplication.data.UserLastSeen;
import com.rohit.ChatApplication.repository.UserRepo;
import com.rohit.ChatApplication.repository.channel.PrivateChannelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class WebSocketPresenceEventListener {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private UserRepo userRepo;

    private SimpMessagingTemplate simpMessagingTemplate;

    @EventListener
    public void handleSessionConnectEvent(SessionConnectEvent event){

       StompHeaderAccessor sha =  StompHeaderAccessor.wrap(event.getMessage());
       String username = sha.getUser() != null ? sha.getUser().getName() : null;

      if(username != null){
          redisTemplate.opsForValue().set("user:" + username + ":online",
                  true , Duration.ofSeconds(20));
          simpMessagingTemplate.convertAndSend("/queue/user-status/" + username ,
                  new UserLastSeen(username , true , null));
      }
    }

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event){

        StompHeaderAccessor sha =  StompHeaderAccessor.wrap(event.getMessage());
        String username = sha.getUser() != null ? sha.getUser().getName() : null;

        if(username != null){
            redisTemplate.opsForValue().set("user:" + username + ":online", false);
            redisTemplate.opsForValue().set("user:" + username + ":lastSeen", LocalDateTime.now().toString());

            simpMessagingTemplate.convertAndSend("/topic/user-status/" + username,
                    new UserLastSeen(username, false,LocalDateTime.now() ));
        }
    }


}
