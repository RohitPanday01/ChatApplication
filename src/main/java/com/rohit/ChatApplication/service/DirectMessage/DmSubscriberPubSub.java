package com.rohit.ChatApplication.service.DirectMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.service.RegisterUserSessionManager;
import com.rohit.ChatApplication.service.Typing.ChannelSubscriberForRedisPubSub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component("DmSubscriberPubSub")
public class DmSubscriberPubSub implements ChannelSubscriberForRedisPubSub {

    private static final Logger log = LoggerFactory.getLogger(DmSubscriberPubSub.class);

    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer container;
    private final RegisterUserSessionManager registerUserSessionManager;

    private final ConcurrentMap<String, MessageListener> privateChannelListeners = new ConcurrentHashMap<>();

    public DmSubscriberPubSub(ObjectMapper objectMapper ,
                              @Qualifier("DmMessageListener") RedisMessageListenerContainer messageListenerContainer,
                              RegisterUserSessionManager registerUserSessionManager){
        this.registerUserSessionManager = registerUserSessionManager;
        this.objectMapper = objectMapper;
        this.container = messageListenerContainer;
    }


    @Override
    public void subscribePrivateChannel(String nodeId) {



        String channel = "Dm:"+ nodeId;

        privateChannelListeners.computeIfAbsent(nodeId , val->{

            MessageListener listener = (message, pattern) -> {

                try {

                    PrivateMessageDto event =
                            objectMapper.readValue(message.getBody() , PrivateMessageDto.class);


                    WebSocketSession session =
                            registerUserSessionManager.getUserSessionInLocalNodeMap(event.getTo().getUsername());

                    if (session != null && session.isOpen()) {
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
                    }
                } catch (Exception e) {
                    log.error("DM typing listener failed", e);
                }
            };

            container.addMessageListener(
                    listener,
                    new ChannelTopic(channel)
            );

            return  listener;

        });



    }

    @Override
    public void subscribeGroup(String groupId) {

    }

    @Override
    public void unsubscribeGroup(String groupId) {

    }

    @Override
    public void unsubscribePrivateChannel(String nodeID) {
        MessageListener listener = privateChannelListeners.remove(nodeID);

        if (listener != null) {

            container.removeMessageListener(listener);
        }

    }
}
