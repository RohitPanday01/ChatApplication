package com.rohit.ChatApplication.service.GroupMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.message.GroupMessageDto;
import com.rohit.ChatApplication.data.message.NodeIdentity;
import com.rohit.ChatApplication.service.RegisterUserSession;
import com.rohit.ChatApplication.service.channel.GroupChannelServiceImpl;
import jakarta.persistence.SecondaryTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionException;

@Service
public class FanOutService {
    private final Logger log = LoggerFactory.getLogger(FanOutService.class);

    private final RegisterUserSession registerUserSession;
    private final RedisTemplate<String ,Object> redisTemplate;
    private final GroupChannelServiceImpl groupChannelService;
    private final NodeIdentity nodeIdentity;
    private final String interNodeIdTopic;
    private  final ObjectMapper objectMapper;
    private final KafkaTemplate<String , Object> kafkaTemplate;

    public FanOutService(RedisTemplate<String ,Object> redisTemplate , RegisterUserSession registerUserSession,
                         GroupChannelServiceImpl groupChannelService , NodeIdentity nodeIdentity,
                         @Value("${chat.topics.groupMessage-inter-node}") String interNodeIdTopic ,ObjectMapper objectMapper,
                         KafkaTemplate<String , Object> kafkaTemplate){
        this.redisTemplate = redisTemplate ;
        this.registerUserSession = registerUserSession;
        this.groupChannelService = groupChannelService;
        this.nodeIdentity = nodeIdentity;
        this.interNodeIdTopic = interNodeIdTopic;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }


    public void fanOutGroupMessage(GroupMessageDto messageDto)  {

        try{
            String channelId = messageDto.getChannel().toString();
            String messageId = messageDto.getId().toString();

            Set<String> members = groupChannelService.getAllGroupMembersOfChannel(channelId);

            if(members == null || members.isEmpty() )return;

            List<String> membersList = new ArrayList<>(members);

            List<Object> presenceResults = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (String user : membersList) {
                    connection.stringCommands().get(("nodeId:"+ user).getBytes(StandardCharsets.UTF_8));
                }
                return null;
            });

            String thisNodeId = nodeIdentity.getNodeId();

            List<String> offlineUsers = new ArrayList<>();
            List<String> localUsers = new ArrayList<>();
            Map<String, List<String>> remoteByNode = new HashMap<>();

            for (int i = 0; i < membersList.size(); i++) {
                Object obj = presenceResults.get(i);

                if (obj != null) {
                    String nodeId = new String((byte[]) obj, StandardCharsets.UTF_8);

                    if (nodeId.equals(thisNodeId)) {
                        localUsers.add(membersList.get(i));
                    } else {
                        remoteByNode
                                .computeIfAbsent(nodeId, k -> new ArrayList<>())
                                .add(membersList.get(i));
                    }
                } else {
                    offlineUsers.add(membersList.get(i));
                }
            }

            String deliveredKeyThis = "delivered:"+ channelId + ":" + thisNodeId;
            Boolean isAlreadyDelivered = redisTemplate.opsForSet().isMember(deliveredKeyThis ,messageId);

            if(!Boolean.TRUE.equals(isAlreadyDelivered)){
                if(!localUsers.isEmpty()){
                    for(String username : localUsers){
                        try{
                            WebSocketSession session =
                                    registerUserSession.getUserSessionInLocalNodeMap(username);

                            if(session != null && session.isOpen()){
                                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(messageDto)));
                            }else {
                                // session got missing
                                redisTemplate.delete("nodeId:"+ username );

                                // send notification he must got offline
                            }

                        } catch (IOException exception ) {
                            log.error("local delivery failed for {}", username, exception);
                            throw exception;

                        }
                    }
                }
                redisTemplate.opsForSet().add(deliveredKeyThis, messageId);
                redisTemplate.expire(deliveredKeyThis, Duration.ofDays(1) );

            }

            Set<String> nodesToNotify = remoteByNode.keySet();
            for(String node : nodesToNotify){
                String deliveredKeyRemote = "delivered:"+ channelId + ":" + node;
                Boolean isAlreadyDeliveredRemote = redisTemplate.opsForSet().isMember(deliveredKeyRemote ,messageId);

                if (Boolean.TRUE.equals(isAlreadyDeliveredRemote)) continue;

                try{
                    kafkaTemplate.send(interNodeIdTopic, node, messageDto).toCompletableFuture().join();
                } catch (Exception e) {
                    throw e;
                }

            }

        } catch (Exception e) {
            log.error("FanOut failed for group message {}", messageDto.getId(), e);
            throw new RuntimeException(e.getMessage());
        }


    }
}
