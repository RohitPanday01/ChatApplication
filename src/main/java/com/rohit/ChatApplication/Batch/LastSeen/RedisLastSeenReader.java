package com.rohit.ChatApplication.Batch.LastSeen;

import com.rohit.ChatApplication.data.UserLastSeen;
import jakarta.annotation.PostConstruct;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

@Component
public class RedisLastSeenReader implements ItemReader<UserLastSeen> {

    private RedisTemplate<String , Object> redisTemplate;
    private Iterator<String > keysIterator;

    public RedisLastSeenReader(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init(){
        Set<String> keys = redisTemplate.keys("user:*:lastSeen");
        keysIterator = keys.iterator();
    }

    @Override
    public UserLastSeen read() throws Exception,
            UnexpectedInputException, ParseException, NonTransientResourceException {
        if(keysIterator.hasNext()){
            String key = keysIterator.next();
            String username = key.split(":")[1];
             LocalDateTime lastSeen = (LocalDateTime) redisTemplate.opsForValue().get(key);
//            Boolean isOnline = (Boolean) redisTemplate.opsForValue().get("user:" + username + ":online");
             return new UserLastSeen(username , false ,lastSeen);
        }
        return null;
    }
}
