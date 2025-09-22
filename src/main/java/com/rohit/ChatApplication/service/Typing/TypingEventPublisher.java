package com.rohit.ChatApplication.service.Typing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.controller.Websocket.PresenceWSHandler;
import com.rohit.ChatApplication.data.TypingEvent;
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

        TypingEvent event = new TypingEvent();
        event.setFrom(from);
        event.setTo(to);
        event.setChannelId(channelId);
        event.setTyping(isTyping);
        event.setTs(System.currentTimeMillis());

        String channel = to == null ? "group:" + channelId + ":typing" : "direct:" +channelId + ":typing" ;


        redisTemplate.convertAndSend(channel, event );
    }




}
