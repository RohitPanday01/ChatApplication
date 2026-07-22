package com.rohit.ChatApplication.controller.Websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;



import com.rohit.ChatApplication.data.message.NodeIdentity;

import com.rohit.ChatApplication.service.RegisterUserSession;
import com.rohit.ChatApplication.service.SessionSubscriptionManager;
import com.rohit.ChatApplication.service.Typing.TypingEventPublisher;

import com.rohit.ChatApplication.service.UserPresence.PresencePublisher;
import com.rohit.ChatApplication.service.channel.GroupChannelServiceImpl;


import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;


@Component
public class PresenceWSHandler extends TextWebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(PresenceWSHandler.class);

    private final RedisTemplate<String , String> redisTemplate;
    private final PresencePublisher presencePublisher;
    private final ObjectMapper mapper;
    private final TypingEventPublisher typingEventPublisher;

    private final RegisterUserSession registerUserSession;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

    private final ConcurrentMap<String , ScheduledFuture<?>> autoStopTimer = new ConcurrentHashMap<>();

    private final GroupChannelServiceImpl groupChannelService;


    private final ConcurrentMap<String , Set<String> > groupChannelsForUser = new ConcurrentHashMap<>();

    private  final SessionSubscriptionManager subscriptionManager;
    private final NodeIdentity nodeIdentity;



    public PresenceWSHandler(@Qualifier("redisStringTemplate") RedisTemplate<String, String> redisTemplate ,
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




    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        UserDetail userDetail = (UserDetail) session.getAttributes().get("userDetail");
//        if (userDetail == null) {
//            log.error("->>>>>>>>>>>WebSocket handshake failed: user not authenticated");
//            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
//            return;
//        }
//
//        log.info("->>>>>>>>>>>>>User connected via WS: {}", userDetail.getUsername());

       String username = (String) session.getAttributes().get("username");
       String userId = (String) session.getAttributes().get("userid");

       WebSocketSession safeSession =
               new ConcurrentWebSocketSessionDecorator(session , 1000, 1024 *64);
//        session.getAttributes().put("userid", userId);
//       session.getAttributes().put("username", username);

       String thisServerNodeId = nodeIdentity.getNodeId();

//       redisTemplate.opsForValue().set("nodeId:"+username , thisServerNodeId);


       try{
           registerUserSession.registerUserSessionInLocalNodeMap(username, safeSession, userId);

//           if(registerUserSession.getUserSessionInLocalNodeMapSize() == 1){
//               subscriptionManager.subscribeUserTypingChannel(thisServerNodeId);
//           }

           Set<String> groupChannelProfiles =  groupChannelService.findAllGroupsForUser(userId);
           groupChannelsForUser.put(username, groupChannelProfiles);

           for(String id : groupChannelProfiles ){

               registerUserSession.registerUserSessionsInTheirGroups(id, safeSession);
               if(registerUserSession.getUserSessionsInTheirGroups(id).size() == 1){
                   subscriptionManager.subscribeGroup(id);
               }
           }

//           redisTemplate.opsForZSet().add("online_users_lastPing",  username , System.currentTimeMillis());


           long now = Instant.now().toEpochMilli();
           redisTemplate.executePipelined((RedisCallback<?>) connection -> {
               byte[] nodeKey = ("nodeId:" + username).getBytes(StandardCharsets.UTF_8);
               byte[] nodeValue = thisServerNodeId.getBytes(StandardCharsets.UTF_8);
               byte[] zsetKey = "online_users_lastPing".getBytes(StandardCharsets.UTF_8);
               byte[] userBytes = username.getBytes(StandardCharsets.UTF_8);

               // Command 1: SET nodeId:username
               connection.stringCommands().set(nodeKey, nodeValue);
               // Command 2: ZADD online_users_lastPing
               connection.zSetCommands().zAdd(zsetKey, now, userBytes);

               return null;
           });
           presencePublisher.publish(username , "online");

           log.info("->>>>>>>>> published user is Online to redis Stream publisher: {}", username);

       }catch (Exception e) {

               log.error("Critical failure during WS connection setup for user: {}. Initiating rollback...", username, e);

               // ROLLBACK STATE ON FAILURE to prevent zombie registrations
               try {
                   registerUserSession.unregisterUserSessionInLocalNodeMap(username, session);
                   redisTemplate.delete("nodeId:" + username);
                   safeSession.close(CloseStatus.SERVER_ERROR.withReason("Connection hydration failed"));
               } catch (Exception rollbackEx) {
                   log.error("Error during connection failure rollback for user: {}", username, rollbackEx);
               }

       }

    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {

         JsonNode node = mapper.readTree(message.getPayload());
         String type = node.path("type").asText(null);

         if("typing".equalsIgnoreCase(type)){

             handleTyping(node, session );

         }else if("ping".equalsIgnoreCase(type)){
             String username = (String) session.getAttributes().get("username");

            redisTemplate.opsForZSet().add("online_users_lastPing",  username , System.currentTimeMillis());
//            redisTemplate.opsForValue().set("nodeId:"+ username , nodeIdentity.getNodeId(), Duration.ofSeconds(60));
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, Throwable exception) {
        log.error("error in handleTransport");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status)  {
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
                registerUserSession.unregisterUserSessionInLocalNodeMap(username , storedSession );
                if(registerUserSession.getUserSessionInLocalNodeMapSize() == 0){
                    subscriptionManager.unsubscribeUserTypingChannel(userId);
                }
            }

//            String nodeID = (String) redisTemplate.opsForValue().get("nodeId:"+ username);
//
//            if(nodeIdentity.getNodeId().equals(nodeID)){
//                redisTemplate.delete("nodeId:"+ username);
//            }
            Set<String> groupChannelProfiles = groupChannelsForUser
                    .getOrDefault(username , Collections.emptySet() );

            for(String id : groupChannelProfiles ){

                Set<WebSocketSession> sessions =
                        registerUserSession.unregisterUserSessionInTheirGroups(id ,storedSession);

                if (sessions.isEmpty()) {
                    subscriptionManager.unsubscribeGroup(id );
                }
            }

            groupChannelsForUser.remove(username);

        } catch (Exception e) {
            log.error("not able to user websocketSession remove from userSessions map ",  e);
        }

//        autoStopTimer.entrySet().removeIf(e ->{
//            if (e.getKey().contains(username)) {
//                e.getValue().cancel(false);
//                return true;
//            }
//            return false;
//        });
//
//        presencePublisher.publish(username , "offline");
//
//        redisTemplate.opsForZSet().remove("online_users_lastPing", username);

    }

    private void handleTyping(JsonNode node , WebSocketSession session)
            throws JsonProcessingException {
//        UserDetail userDetail = (UserDetail) session.getAttributes().get("userDetail");
//
//
//        String username = userDetail.getUsername();
        String username = (String) session.getAttributes().get("username");
//        String userId = userDetail.getId();
        String channelId =  node.path("channelId").asText();
        String to = node.path("to").asText(null);
        boolean typing =  node.path("isTyping").asBoolean();

//        if (typing) armAutoStop(channelId, username, to);

        typingEventPublisher.publishTypingEvent(username ,to,channelId,
                   typing);
    }


//    private void armAutoStop(String channelId , String from ,String to){
//        String key = from + "|" + channelId;
//        var prev = autoStopTimer.get(key);
//
//        if(prev != null ) prev.cancel(false);
//
//
//        autoStopTimer.put(key , executorService.schedule(()->{
//            try {
//                typingEventPublisher.publishTypingEvent(from ,to,channelId,
//                        false);
//            } catch ( Exception e) {
//                throw new RuntimeException(e.getMessage());
//            }
//            autoStopTimer.remove(key);
//
//        }, 3, TimeUnit.SECONDS));
//
//    }

}
