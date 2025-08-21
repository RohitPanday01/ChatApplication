package com.rohit.ChatApplication.service.UserPresence;

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

    private final RedisTemplate<String, Object> redisTemplate;

    public PresencePublisher(RedisTemplate<String, Object> redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    public void publish(String userName , String status){
        Map<String , String > data = new HashMap<>();
        data.put("userName", userName);
        data.put("status",status);
        data.put("TimeStamp" , LocalDateTime.now().toString());

        MapRecord<String, String, String> record = StreamRecords
                .newRecord()
                .in("presence_stream")
                .ofMap(data);

        RecordId id = redisTemplate.opsForStream()
                .add(record, RedisStreamCommands.XAddOptions.maxlen(1000).approximateTrimming(true));

        System.out.println("Published event with id: " + id);
    }
}
