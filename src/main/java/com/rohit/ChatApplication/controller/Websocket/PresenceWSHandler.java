package com.rohit.ChatApplication.controller.Websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.rohit.ChatApplication.data.GroupMemberProfile;
import com.rohit.ChatApplication.data.TypingEvent;
import com.rohit.ChatApplication.data.UserDetail;
import com.rohit.ChatApplication.data.channel.profile.GroupChannelProfile;
import com.rohit.ChatApplication.data.channel.profile.PrivateChannelProfile;
import com.rohit.ChatApplication.service.RegisterUserSession;
import com.rohit.ChatApplication.service.SessionSubscriptionManager;
import com.rohit.ChatApplication.service.Typing.TypingEventPublisher;
import com.rohit.ChatApplication.service.Typing.TypingSubscriber;
import com.rohit.ChatApplication.service.UserPresence.PresencePublisher;
import com.rohit.ChatApplication.service.channel.GroupChannelServiceImpl;
import com.rohit.ChatApplication.service.channel.PrivateChannelServiceImpl;
import com.rohit.ChatApplication.util.AuthUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;


@Component
@Slf4j
public class PresenceWSHandler extends TextWebSocketHandler {

    private final RedisTemplate<String , Object> redisTemplate;
    private final PresencePublisher presencePublisher;
    private final ObjectMapper mapper;
    private final TypingEventPublisher typingEventPublisher;


    private final ConcurrentMap<String , WebSocketSession> userSessions =
            new ConcurrentHashMap<>();

    private final RegisterUserSession registerUserSession;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

    private final ConcurrentMap<String , ScheduledFuture<?>> autoStopTimer = new ConcurrentHashMap<>();

    private final GroupChannelServiceImpl groupChannelService;

    private Set<GroupChannelProfile>groupChannelProfiles ;

    private  final SessionSubscriptionManager subscriptionManager;




    public PresenceWSHandler(RedisTemplate<String, Object> redisTemplate ,
                             PresencePublisher presencePublisher,  ObjectMapper mapper,
                             TypingEventPublisher typingEventPublisher ,
                             GroupChannelServiceImpl groupChannelService,
                             RegisterUserSession registerUserSession,
                             SessionSubscriptionManager subscriptionManager){
        this.redisTemplate = redisTemplate;
        this.presencePublisher = presencePublisher;
        this.mapper = mapper;
        this.typingEventPublisher = typingEventPublisher;
        this.groupChannelService = groupChannelService;
        this.registerUserSession = registerUserSession;
        this.subscriptionManager = subscriptionManager;

    }

    public WebSocketSession getSession(String username) {
        return userSessions.get(username);
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

       UserDetail userDetail =  AuthUtil.currentUserDetail();
       String username = userDetail.getUsername();
        String userId = userDetail.getId();
        session.getAttributes().put("userid", userId);
       session.getAttributes().put("username", username);

       registerUserSession.registerUserSessionInLocalNodeMap(username , session, userId);
       subscriptionManager.subscribeUserChannels(userId);

       groupChannelProfiles =  groupChannelService.findAllGroupsForUser(userId);

       for(GroupChannelProfile groupChannelProfile : groupChannelProfiles ){

           registerUserSession.registerUserSessionsInTheirGroups(groupChannelProfile.getId(), session);
           if(registerUserSession.getUserSessionsInTheirGroups(groupChannelProfile.getId()).size() == 1){
               subscriptionManager.subscribeGroup(groupChannelProfile.getId());
           }

       }

       redisTemplate.opsForZSet().add("online_users_lastPing",  username , System.currentTimeMillis());
       presencePublisher.publish(username , "online");
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {

         JsonNode node = mapper.readTree(message.getPayload());
         String type = String.valueOf(node.path("type"));

         if("typing".equalsIgnoreCase(type)){

             handleTyping(node, session );

         }else if("ping".equalsIgnoreCase(type)){

            String username =  AuthUtil.currentUserDetail().getUsername();
            redisTemplate.opsForZSet().add("online_users_lastPing",  username , System.currentTimeMillis());
            session.sendMessage(new TextMessage("pong"));

        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, Throwable exception) throws Exception {
        exception.printStackTrace();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        String userId = (String)session.getAttributes().get("userid");
        String sessionId = session.getId();


        try{

            WebSocketSession storedSession = registerUserSession.getUserSessionInLocalNodeMap(username);
            if (storedSession != null && storedSession.getId().equals(sessionId)) {
                registerUserSession.unregisterUserSessionInLocalNodeMap(username , session , userId);
                subscriptionManager.unsubscribeUserChannels(userId);
            }

            for(GroupChannelProfile groupChannelProfile : groupChannelProfiles ){

                Set<WebSocketSession> sessions = registerUserSession.unregisterUserSessionInTheirGroups(groupChannelProfile.getId() ,session);

                if (sessions.isEmpty()) {
                    subscriptionManager.unsubscribeGroup(groupChannelProfile.getId() );
                }

            }

        } catch (Exception e) {
            log.error("not able to user websocketSession remove from userSessions map ",  e);
        }


        autoStopTimer.entrySet().removeIf(e ->{
            if (e.getKey().contains(username)) {
                e.getValue().cancel(false);
                return true;
            }
            return false;
        });


        presencePublisher.publish(username , "offline");
        redisTemplate.opsForZSet().remove("online_users_lastPing", username);

    }

    private void handleTyping(JsonNode node , WebSocketSession session)
            throws JsonProcessingException {
        String username =  AuthUtil.currentUserDetail().getUsername();
        String channelId =  node.path("channelId").asText();
        String to = node.path("to").asText(null);
        boolean typing =  node.path("isTyping").asBoolean();

        if (typing) armAutoStop(channelId, username, to);



        typingEventPublisher.publishTypingEvent(username ,to,channelId,
                   typing);

    }


    private void armAutoStop(String channelId , String from ,String to){
        String key = from + "|" + channelId;
        var prev = autoStopTimer.get(key);

        if(prev != null ) prev.cancel(false);


        autoStopTimer.put(key , executorService.schedule(()->{
            try {
                typingEventPublisher.publishTypingEvent(from ,to,channelId,
                        false);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            autoStopTimer.remove(key);

        }, 3, TimeUnit.SECONDS));

    }

}
