package com.rohit.ChatApplication.service.Typing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.controller.Websocket.PresenceWSHandler;
import com.rohit.ChatApplication.data.TypingEvent;
import com.rohit.ChatApplication.service.UserRoutingService;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class TypingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TypingEventPublisher.class);

    private final RedisTemplate<String , Object> redisTypingTemplate;
    private ObjectMapper objectMapper;
    private UserRoutingService userRoutingService;



    public TypingEventPublisher(@Qualifier("typingIndicatorTemplate") RedisTemplate<String, Object> redisTemplate,
                                ObjectMapper objectMapper, UserRoutingService userRoutingService){
        this.redisTypingTemplate = redisTemplate;
        this.objectMapper =  objectMapper;
        this.userRoutingService = userRoutingService;
    }

    public void publishTypingEvent(String from , String to , String channelId , boolean isTyping)  {


        String nodeId =  userRoutingService.getUserLocation(to);
        TypingEvent event = new TypingEvent();
        event.setFrom(from);
        event.setTo(to);
        event.setChannelId(channelId);
        event.setTyping(isTyping);
        event.setTs(Instant.now().toEpochMilli());

        try {
            String payload = objectMapper.writeValueAsString(event);
            String channel = to == null ? "group:" + channelId + ":typing" : "typing:"+nodeId  ;

            // Step D: Fire & forget across the dedicated typing Pub/Sub socket
            redisTypingTemplate.convertAndSend(channel, payload );

        }catch (RedisConnectionFailureException | RedisCommandTimeoutException  e){

            log.error("Failed to send  typing payload for sender to sender {}", to, e);

        } catch  (JsonProcessingException e) {
            log.error("Failed to serialize typing payload for sender {}", to, e);
        }


    }




}
