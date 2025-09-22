package com.rohit.ChatApplication.controller.Websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.rohit.ChatApplication.data.UserDetail;
import com.rohit.ChatApplication.data.channel.profile.GroupChannelProfile;

import com.rohit.ChatApplication.data.message.NodeIdentity;

import com.rohit.ChatApplication.service.RegisterUserSession;
import com.rohit.ChatApplication.service.SessionSubscriptionManager;
import com.rohit.ChatApplication.service.Typing.TypingEventPublisher;

import com.rohit.ChatApplication.service.UserPresence.PresencePublisher;
import com.rohit.ChatApplication.service.channel.GroupChannelServiceImpl;

import com.rohit.ChatApplication.util.AuthUtil;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;



import java.time.Duration;

import java.util.Set;
import java.util.concurrent.*;


@Component

public class PresenceWSHandler extends TextWebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(PresenceWSHandler.class);

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
    private final NodeIdentity nodeIdentity;



    public PresenceWSHandler(RedisTemplate<String, Object> redisTemplate ,
                             PresencePublisher presencePublisher,  ObjectMapper mapper,
                             TypingEventPublisher typingEventPublisher ,
                             GroupChannelServiceImpl groupChannelService,
                             RegisterUserSession registerUserSession,
                             SessionSubscriptionManager subscriptionManager,
                             NodeIdentity nodeIdentity){
        this.redisTemplate = redisTemplate;
        this.presencePublisher = presencePublisher;
        this.mapper = mapper;
        this.typingEventPublisher = typingEventPublisher;
        this.groupChannelService = groupChannelService;
        this.registerUserSession = registerUserSession;
        this.subscriptionManager = subscriptionManager;
        this.nodeIdentity = nodeIdentity;

    }

    public WebSocketSession getSession(String username) {
        return userSessions.get(username);
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UserDetail userDetail = (UserDetail) session.getAttributes().get("userDetail");
        if (userDetail == null) {
            log.error("->>>>>>>>>>>WebSocket handshake failed: user not authenticated");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            return;
        }

        log.info("->>>>>>>>>>>>>User connected via WS: {}", userDetail.getUsername());

       String username = userDetail.getUsername();
        String userId = userDetail.getId();
        session.getAttributes().put("userid", userId);
       session.getAttributes().put("username", username);

       String thisServerNodeId = nodeIdentity.getNodeId();

       redisTemplate.opsForValue().set("nodeId:"+ username , thisServerNodeId, Duration.ofMinutes(20));


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
       log.info("->>>>>>>>> published user is Online to redis Stream publisher: {}", username);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {

         JsonNode node = mapper.readTree(message.getPayload());
         String type = node.path("type").asText(null);

         if("typing".equalsIgnoreCase(type)){

             handleTyping(node, session );

         }else if("ping".equalsIgnoreCase(type)){
             String username = (String) session.getAttributes().get("username");
             String userId = (String)session.getAttributes().get("userid");

            redisTemplate.opsForZSet().add("online_users_lastPing",  username , System.currentTimeMillis());
            session.sendMessage(new TextMessage("pong"));

        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, Throwable exception) {
        log.error("error in handleTransport");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        String username = (String) session.getAttributes().get("username");
        String userId = (String)session.getAttributes().get("userid");
        String sessionId = session.getId();



        if (username == null || userId == null) {
            log.warn("afterConnectionClosed called but username or userId is null, skipping cleanup");
            return;
        }


        try{

            WebSocketSession storedSession = registerUserSession.getUserSessionInLocalNodeMap(username);
            if (storedSession != null && storedSession.getId().equals(sessionId)) {
                registerUserSession.unregisterUserSessionInLocalNodeMap(username , session );
                subscriptionManager.unsubscribeUserChannels(userId);
            }


            redisTemplate.delete("nodeId:"+ username );


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
        UserDetail userDetail = (UserDetail) session.getAttributes().get("userDetail");


        String username = userDetail.getUsername();
//        String userId = userDetail.getId();
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
