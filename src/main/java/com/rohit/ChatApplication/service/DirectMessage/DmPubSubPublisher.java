package com.rohit.ChatApplication.service.DirectMessage;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.service.UserRoutingService;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class DmPubSubPublisher {

    private static final Logger log = LoggerFactory.getLogger(DmPubSubPublisher.class);

    private final RedisTemplate<String , Object> redisChatTemplate;
    private ObjectMapper objectMapper;
    private UserRoutingService userRoutingService;



    public DmPubSubPublisher(@Qualifier("chatPubSubTemplate") RedisTemplate<String, Object> redisTemplate,
                                ObjectMapper objectMapper, UserRoutingService userRoutingService){
        this.redisChatTemplate = redisTemplate;
        this.objectMapper =  objectMapper;
        this.userRoutingService = userRoutingService;
    }

    public void publishDm(PrivateMessageDto privateMessage){

        String nodeId = userRoutingService.getUserLocation(privateMessage.getTo().getUsername());

        String from = privateMessage.getFrom().getUsername();
        String to = privateMessage.getTo().getUsername();

        try{
            String payload = objectMapper.writeValueAsString(privateMessage);

            String channel = "Dm:"+ nodeId;

            redisChatTemplate.convertAndSend(channel , payload);
        }catch (RedisConnectionFailureException | RedisCommandTimeoutException e){
            log.error("Failed to send Dm payload for sender to  receiver {} {}, on redis server pub/sub", to, from,  e);
            throw new RedisException("failed to send message to redis server for node:id" + nodeId,  e );

        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }


}
