package com.rohit.ChatApplication.Batch.LastSeen;

import com.rohit.ChatApplication.data.UserLastSeen;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

@Component
public class RedisLastSeenReader implements ItemReader<UserLastSeen> {

    private final Logger log = LoggerFactory.getLogger(RedisLastSeenReader.class);

    private final RedisTemplate<String , String> redisTemplate;
    private Iterator<String > keysIterator;

    public RedisLastSeenReader(@Qualifier("redisStringTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init(){
        Set<String> keys = redisTemplate.keys("user:*:lastSeen");
        log.info("-->>>>>>> lastSeen Key in RedisLastSeen Reader:{}",keys);
        keysIterator = keys.iterator();
        log.info("-->>>>>>> lastSeen KeyIterator in RedisLastSeen Reader:{}",keysIterator);
    }
 
    @Override
    public UserLastSeen read() throws Exception,
            UnexpectedInputException, ParseException, NonTransientResourceException {
        while(keysIterator.hasNext()) {
            String key = keysIterator.next();
            String username = key.split(":")[1];

            if(username == null || username.equals("null")) {
                log.warn("->>>>>Skipping invalid lastSeen key: {}", key);
                continue; // skip to next
            }

            String lastSeenStr = redisTemplate.opsForValue().get("user:" + username + ":lastSeen");
            LocalDateTime lastSeen = null;
            if(lastSeenStr != null) {
                try {
                    lastSeen = LocalDateTime.parse(lastSeenStr);
                } catch (DateTimeParseException e) {
                    log.error("Failed to parse lastSeen for user {} from value {}", username, lastSeenStr);
                }
            }

            return new UserLastSeen(username, false, lastSeen);
        }

        return null; // no more items
    }

}
