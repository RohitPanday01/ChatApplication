package com.rohit.ChatApplication.service.UserPresence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Service
public class PresencePublisher {

    private final Logger log = LoggerFactory.getLogger(PresencePublisher.class);

    private final RedisTemplate<String, String> redisTemplate;

    public PresencePublisher(@Qualifier("redisStringTemplate") RedisTemplate<String, String> redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    public void publish(String username , String status){
        Map<String , String > data = new HashMap<>();
        data.put("username", username);
        data.put("status",status);
        data.put("TimeStamp" , LocalDateTime.now().toString());
        
        MapRecord<String, String, String> record = StreamRecords
                .newRecord()
                .in("presence_stream")
                .ofMap(data);

        log.info("->>>> data going to push in stream is; {}", record.getValue());
        try {
            RecordId id = redisTemplate.opsForStream()
                    .add(record, RedisStreamCommands.XAddOptions.maxlen(1000).approximateTrimming(true));

            log.info(">>>>>>>>>>>>>>>Published event with id to stream: {}", id);

        } catch (Exception e) {
            log.error(">>>>>>>>>>>>>>> unable to Publish Online event in Stream for username ,{}: " , username);
            throw new RuntimeException(e);
        }


    }
}
