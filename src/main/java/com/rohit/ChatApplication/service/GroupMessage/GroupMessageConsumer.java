package com.rohit.ChatApplication.service.GroupMessage;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.message.GroupMessageDto;
import com.rohit.ChatApplication.entity.GroupChannel;
import com.rohit.ChatApplication.entity.GroupMember;
import com.rohit.ChatApplication.exception.ChannelDoesNotExist;
import com.rohit.ChatApplication.service.RegisterUserSession;
import com.rohit.ChatApplication.service.channel.GroupChannelServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class GroupMessageConsumer {

    private final GroupChannelServiceImpl groupChannelService;
    private final RegisterUserSession registerUserSession;
    private final RedisTemplate<String , Object >redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${chat.topics.group-delivery}",
            groupId = "group-message-consumerGroup",
            containerFactory = "GroupMessageDeliveryContainer"
    )
    public void onMessage(GroupMessageDto groupMessageDto, Acknowledgment ack){

        String channel = groupMessageDto.getChannel().toString();
        String sender = groupMessageDto.getFrom().toString();

        try{
            Optional<GroupChannel> groupChannelOptional = groupChannelService.getChannelById(groupMessageDto.getChannel());

            if(groupChannelOptional.isEmpty()){
                return;
            }

           Set<GroupMember>groupMembers = groupChannelOptional.get().getActiveMembers();

            for(GroupMember member : groupMembers){


                String memberUsername  = member.getUser().getUsername();

                String dedupKey = String.format("Delivered%s%s",memberUsername, groupMessageDto.getId() );

                Double lastSeenScore = redisTemplate.opsForZSet().score("online_users_lastPing", memberUsername);

                boolean isOnline = lastSeenScore != null && lastSeenScore >= (System.currentTimeMillis() - 10_000);


                if(isOnline){
                    WebSocketSession session = registerUserSession.getUserSessionInLocalNodeMap(memberUsername);
                    if(session!= null && session.isOpen()) {

                        Long added = redisTemplate.opsForSet().add(dedupKey, groupMessageDto.getId().toString());
                        redisTemplate.expire(dedupKey, Duration.ofDays(1));

                        if(added > 0)  session.sendMessage(new TextMessage(objectMapper.writeValueAsString(groupMessageDto)));


                    }else {

                        //internode
                    }


                }else{
                    // notification

                }
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }




    }
}
