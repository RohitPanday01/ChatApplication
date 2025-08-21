package com.rohit.ChatApplication.service.Typing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.controller.Websocket.PresenceWSHandler;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TypingEventPublisher {

    private final RedisTemplate<String , Object> redisTemplate;
    private final ObjectMapper objectMapper ;


    public TypingEventPublisher(RedisTemplate<String, Object> redisTemplate , ObjectMapper objectMapper){
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishTypingEvent(String from , String to , String channelId , boolean isTyping) throws JsonProcessingException {

        var payload = Map.of(
                "event", "typing",
                "from", from,
                "channelId", channelId,
                "to", to == null ? "" : to,
                "typing", String.valueOf(isTyping),
                "ts", String.valueOf(System.currentTimeMillis())
        );

        String channel = to == null ? "group:" + channelId + ":typing" : "direct:" +channelId + ":typing" ;


        redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(payload) );
    }




}
