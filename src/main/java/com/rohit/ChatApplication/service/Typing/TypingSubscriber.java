package com.rohit.ChatApplication.service.Typing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.controller.Websocket.PresenceWSHandler;
import com.rohit.ChatApplication.data.TypingEvent;
import com.rohit.ChatApplication.service.RegisterUserSession;
import com.rohit.ChatApplication.service.RegisterUserSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Component
@Slf4j
public class TypingSubscriber implements ChannelSubscriberForTyping {


    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer container;
    private  final RegisterUserSessionManager registerUserSessionManager;
    private final  RedisTemplate<String , Object> redisTemplate ;
    private final  Map<String , MessageListener> groupListeners = new ConcurrentHashMap<>();
    private final Map<String, MessageListener> privateChannelListeners = new ConcurrentHashMap<>();

    public TypingSubscriber(ObjectMapper objectMapper, RedisMessageListenerContainer container,
                            RegisterUserSessionManager registerUserSessionManager, RedisTemplate<String , Object> redisTemplate){
        this.objectMapper = objectMapper;
        this.container = container;
        this.registerUserSessionManager = registerUserSessionManager;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void subscribePrivateChannel(String channelId){

        String channel = "direct:" + channelId + ":typing";
        MessageListener listener = (message, pattern) -> {
            try {
//                String body = new String(message.getBody(), StandardCharsets.UTF_8);
//
//                TypingEvent event = objectMapper.readValue(body, TypingEvent.class);
                TypingEvent event = (TypingEvent) redisTemplate.getValueSerializer().deserialize(message.getBody());
                WebSocketSession session  = registerUserSessionManager.getUserSessionInLocalNodeMap(event.getTo());

                if(session != null){
                    tryAndSend(event , session);
                }

            } catch (Exception e) {
                log.error("unable to forward typing event to local node private channel member in MessageListener", e);
            }
        };

        container.addMessageListener(listener, new ChannelTopic(channel));
        privateChannelListeners.put(channelId , listener);

    }
    private void  tryAndSend(TypingEvent typingEvent , WebSocketSession session){
        try{
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(typingEvent)));
            }
        }catch(Exception e){
            log.warn("not able to send the typing event" ,e );
        }
    }
    @Override
    public void subscribeGroup(String groupId) {
        String channel = "group" + groupId + "typing";

        MessageListener listener = (message, pattern) -> {
            try {
//                String body = new String(message.getBody(), StandardCharsets.UTF_8);
//
//
//                TypingEvent event = objectMapper.readValue(body, TypingEvent.class);
                TypingEvent event = (TypingEvent) redisTemplate.getValueSerializer().deserialize(message.getBody());

                Set<WebSocketSession> sessions = registerUserSessionManager.getUserSessionsInTheirGroups(groupId);

                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
                    }
                }

            } catch (Exception e) {
                log.error("unable to forward typing event to local node group member in MessageListener", e);
            }

        };
        container.addMessageListener(listener, new ChannelTopic(channel));
        groupListeners.put(groupId, listener);

    }

    @Override
    public void unsubscribeGroup(String groupId){

       MessageListener listener =  groupListeners.remove(groupId);
       if(listener != null){
           container.removeMessageListener(listener);
       }

    }

    @Override
    public void unsubscribePrivateChannel(String channelId){

        MessageListener listener =  privateChannelListeners.remove(channelId);
        if(listener != null){
            container.removeMessageListener(listener);
        }
    }



}
