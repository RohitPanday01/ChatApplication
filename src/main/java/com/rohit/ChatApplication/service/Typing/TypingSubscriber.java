package com.rohit.ChatApplication.service.Typing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.TypingEvent;
import com.rohit.ChatApplication.service.RegisterUserSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Component
@Slf4j
public class TypingSubscriber implements ChannelSubscriberForTyping {


    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer container;
    private  final RegisterUserSessionManager registerUserSessionManager;
    private final  RedisTemplate<String , Object> redisTemplate ;
    private final ConcurrentMap<String , MessageListener> groupListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MessageListener> privateChannelListeners = new ConcurrentHashMap<>();

    public TypingSubscriber(ObjectMapper objectMapper,
                            @Qualifier("typingIndicatorMessageListener") RedisMessageListenerContainer container,
                            RegisterUserSessionManager registerUserSessionManager,
                            @Qualifier("typingIndicatorTemplate") RedisTemplate<String , Object> redisTemplate){
        this.objectMapper = objectMapper;
        this.container = container;
        this.registerUserSessionManager = registerUserSessionManager;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void subscribePrivateChannel(String nodeId){

        privateChannelListeners.computeIfAbsent(nodeId, user -> {

            String redisChannel =  "typing:"+nodeId ;

            MessageListener listener = (message, pattern) -> {

                try {

                    TypingEvent event =
                            objectMapper.readValue(message.getBody() , TypingEvent.class);


                    WebSocketSession session =
                            registerUserSessionManager.getUserSessionInLocalNodeMap(event.getTo());

                    if (session != null && session.isOpen()) {
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
                    }
                } catch (Exception e) {
                    log.error("DM typing listener failed", e);
                }
            };

            container.addMessageListener(
                    listener,
                    new ChannelTopic(redisChannel)
            );

            return listener;
        });

    }

    @Override
    public void subscribeGroup(String groupId) {
        groupListeners.computeIfAbsent(groupId, id -> {

            String redisChannel = "group:" + id + ":typing";

            MessageListener listener = (message, pattern) -> {

                try {

                    TypingEvent event =  objectMapper.readValue(message.getBody() , TypingEvent.class);
                    Set<WebSocketSession> sessions =
                            registerUserSessionManager.getUserSessionsInTheirGroups(id);

                    for (WebSocketSession session : sessions) {

                        if (session.isOpen()) {

                            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
                        }
                    }

                } catch (Exception e) {

                    log.error("Group typing listener failed", e);
                }
            };

            container.addMessageListener(listener, new ChannelTopic(redisChannel));
            return listener;
        });
    }

    @Override
    public void unsubscribeGroup(String groupId){

       MessageListener listener =  groupListeners.remove(groupId);
       if(listener != null){
           container.removeMessageListener(listener);
       }

    }

    @Override
    public void unsubscribePrivateChannel(String nodeID){

        MessageListener listener = privateChannelListeners.remove(nodeID);

        if (listener != null) {

            container.removeMessageListener(listener);
        }
    }



}
