package com.rohit.ChatApplication.service;

import com.rohit.ChatApplication.service.UserPresence.PresencePublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class HeartBeat {
    private final RedisTemplate<String , Object> redisTemplate;
    private final PresencePublisher presencePublisher;

    public HeartBeat(RedisTemplate<String , Object> redisTemplate ,PresencePublisher presencePublisher ){
        this.redisTemplate = redisTemplate ;
        this.presencePublisher = presencePublisher;
    }

    @Scheduled(fixedRate =  10000)
    public void checkHeartBeats(){

        long now = System.currentTimeMillis();
        long staleTime = now - 20000;

        Set<Object> staleUsers  =  redisTemplate.opsForZSet().rangeByScore("online_users_lastPing", 0 , staleTime);
        if(staleUsers == null) return;

        for( Object obj : staleUsers ){
            String username = (String) obj;

            presencePublisher.publish( username, "offline");
            redisTemplate.opsForZSet().remove("online_users_lastPing", username);


        }
    }
}
