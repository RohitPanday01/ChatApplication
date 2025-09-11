package com.rohit.ChatApplication.service.GroupMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.message.GroupMessageDto;
import com.rohit.ChatApplication.data.message.NodeIdentity;
import com.rohit.ChatApplication.service.RegisterUserSession;
import com.rohit.ChatApplication.service.channel.GroupChannelServiceImpl;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.util.Set;

@Component
public class InterNodeListener {
    private final Logger log = LoggerFactory.getLogger(InterNodeListener.class);

    private final RegisterUserSession registerUserSession;
    private final RedisTemplate<String ,Object> redisTemplate;
    private final GroupChannelServiceImpl groupChannelService;
    private final NodeIdentity nodeIdentity ;
    private final String interNodeIdTopic;
    private  final ObjectMapper objectMapper;
    private final KafkaTemplate<String , GroupMessageDto> kafkaTemplate;

    public InterNodeListener(RedisTemplate<String ,Object> redisTemplate , RegisterUserSession registerUserSession,
                         GroupChannelServiceImpl groupChannelService , NodeIdentity nodeIdentity,
                         @Value("${chat.topics.inter-node") String interNodeIdTopic , ObjectMapper objectMapper,
                         KafkaTemplate<String , GroupMessageDto> kafkaTemplate){
        this.redisTemplate = redisTemplate ;
        this.registerUserSession = registerUserSession;
        this.groupChannelService = groupChannelService;
        this.nodeIdentity = nodeIdentity;
        this.interNodeIdTopic = interNodeIdTopic;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "${chat.topics.groupMessage-inter-node}")
    public void onInterNode(ConsumerRecord<String, GroupMessageDto> record,
                            Acknowledgment ack) {

        String targetNode = record.key();
        GroupMessageDto msg = record.value();
        String thisNodeId = nodeIdentity.getNodeId();

        if (!thisNodeId.equals(targetNode)) {
            ack.acknowledge();
            return;
        }
        try{
            String channelId = msg.getChannel().toString();
            String msgId = msg.getId().toString();
            String deliveredKey = "delivered:"+ channelId + ":" + thisNodeId;
            Boolean already = redisTemplate.opsForSet().isMember(deliveredKey, msgId);

            if (Boolean.TRUE.equals(already)) {
                ack.acknowledge();
                return;
            }

            Set<String> groupMembers = groupChannelService.getAllGroupMembersOfChannel(channelId);

            if (groupMembers != null) {
                for (String username : groupMembers) {
                    WebSocketSession s =
                            registerUserSession.getUserSessionInLocalNodeMap(username);

                    if (s != null && s.isOpen()) {
                        s.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
                    } else {
                        // session got missing
                        redisTemplate.delete("nodeId:"+ username );

                        // send notification he must got offline
                    }

                }

            }
            redisTemplate.opsForSet().add(deliveredKey, msgId);
            redisTemplate.expire(deliveredKey, Duration.ofDays(1) );

            ack.acknowledge();

        } catch (Exception e) {
            log.error("error consuming message from kafka group-inter-node kafka will retry");
        }


    }

}
