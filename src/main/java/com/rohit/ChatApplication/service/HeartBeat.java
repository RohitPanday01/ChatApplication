package com.rohit.ChatApplication.service;

import com.rohit.ChatApplication.data.message.NodeIdentity;
import com.rohit.ChatApplication.service.UserPresence.PresencePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@Service
public class HeartBeat {

    private final Logger log = LoggerFactory.getLogger(HeartBeat.class);
    private final RedisTemplate<String , String> redisTemplate;
    private final PresencePublisher presencePublisher;
    private final RegisterUserSession registerUserSession;


    public HeartBeat(@Qualifier("redisStringTemplate")RedisTemplate<String , String> redisTemplate , PresencePublisher presencePublisher,
                     RegisterUserSession registerUserSession){
        this.redisTemplate = redisTemplate ;
        this.presencePublisher = presencePublisher;
        this.registerUserSession = registerUserSession;

    }

    @Scheduled(fixedRate =  20000)
    public void checkHeartBeats(){

        long now = System.currentTimeMillis();
        long staleTime = now - 60000;

        Set<String> staleUsers  =  redisTemplate.opsForZSet().rangeByScore("online_users_lastPing", 0 , staleTime);
        log.info(">>>>>> Stale user from online userList:{} " , staleUsers);

        if(staleUsers == null) return;

        for( String staleUser : staleUsers ){


            presencePublisher.publish(staleUser, "offline");
            redisTemplate.opsForZSet().remove("online_users_lastPing", staleUser);

            log.info(">>>>>> Removed user from online userList:{} " , staleUser);


            redisTemplate.delete("nodeId:"+ staleUser);


        }
    }
}
