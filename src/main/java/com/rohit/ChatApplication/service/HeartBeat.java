package com.rohit.ChatApplication.service;

import com.rohit.ChatApplication.data.message.NodeIdentity;
import com.rohit.ChatApplication.service.UserPresence.PresencePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

@Service
public class HeartBeat {

    private final Logger log = LoggerFactory.getLogger(HeartBeat.class);
    private final RedisTemplate<String , Object> redisTemplate;
    private final PresencePublisher presencePublisher;
    private final RegisterUserSession registerUserSession;


    public HeartBeat(RedisTemplate<String , Object> redisTemplate ,PresencePublisher presencePublisher,
                     RegisterUserSession registerUserSession){
        this.redisTemplate = redisTemplate ;
        this.presencePublisher = presencePublisher;
        this.registerUserSession = registerUserSession;

    }

    @Scheduled(fixedRate =  10000)
    public void checkHeartBeats(){

        long now = System.currentTimeMillis();
        long staleTime = now - 90000;

        Set<Object> staleUsers  =  redisTemplate.opsForZSet().rangeByScore("online_users_lastPing", 0 , staleTime);
        log.info(">>>>>> Stale usesr from online userList:{} " , staleUsers);

        if(staleUsers == null) return;

        for( Object obj : staleUsers ){
            String username = (String) obj;

            WebSocketSession session = registerUserSession.getUserSessionInLocalNodeMap(username);

            presencePublisher.publish( username, "offline");
            redisTemplate.opsForZSet().remove("online_users_lastPing", username);

            log.info(">>>>>> Removed user from online userList:{} " , username);

            if(session != null)  registerUserSession.unregisterUserSessionInLocalNodeMap(username,session);


            redisTemplate.delete("nodeId:"+ username );


        }
    }
}
