package com.rohit.ChatApplication.controller.Websocket;


import com.rohit.ChatApplication.data.*;
import com.rohit.ChatApplication.data.channel.SliceOfPrivateChannel;
import com.rohit.ChatApplication.data.channel.profile.PrivateChannelProfile;
import com.rohit.ChatApplication.data.channel.request.ChannelRequest;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.data.message.request.PublishMessageRequest;
import com.rohit.ChatApplication.entity.MessageType;
import com.rohit.ChatApplication.entity.User;
import com.rohit.ChatApplication.exception.ChannelDoesNotExist;
import com.rohit.ChatApplication.exception.InvalidOperation;
import com.rohit.ChatApplication.exception.UserDoesNotExist;
import com.rohit.ChatApplication.service.channel.PrivateChannelServiceImpl;
import com.rohit.ChatApplication.service.message.PrivateMessageServiceImpl;
import com.rohit.ChatApplication.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/channel/private")
public class PrivateChannelController {


    private final RedisTemplate<String, Object> redisTemplate;
    private final PrivateChannelServiceImpl privateChannelService;
    private final PrivateMessageServiceImpl privateMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public PrivateChannelController(
            RedisTemplate<String, Object> redisTemplate,
            PrivateChannelServiceImpl privateChannelService,
            PrivateMessageServiceImpl privateMessageService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.privateChannelService = privateChannelService;
        this.privateMessageService = privateMessageService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping(path = "{channelId}/profile")
    public ResponseEntity<Object> profile(@PathVariable String channelId){
        try {
            PrivateChannelProfile privateChannelProfile =
                    privateChannelService.getChannelProfile(channelId,
                            AuthUtil.currentUserDetail().getId());

        return ResponseEntity.ok(privateChannelProfile);
        } catch (InvalidOperation e) {
            return new ResponseEntity<>
                    (new ErrorMessageResponse(e.getMessage()), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>
                    (new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(path = "/")
    public ResponseEntity<SliceList<PrivateChannelProfile>>
    getAllPrivateChannelForUser(@RequestParam int page, @RequestParam int size)
            throws UserDoesNotExist {
        PageRequest pageRequest = PageRequest.of(page, size);
        String user = AuthUtil.currentUserDetail().getId();

        SliceList<PrivateChannelProfile> privateChannelProfileSliceList  =
                privateChannelService.getAllChannel(user,
                        pageRequest.getPageNumber(), pageRequest.getPageSize());

        return ResponseEntity.ok( privateChannelProfileSliceList);
    }

    @GetMapping(path = "{channelId}/messages")
    public ResponseEntity<SliceList<PrivateMessageDto>>
     getAllMessages(@PathVariable String channelId , @RequestParam int page, @RequestParam int size )
            throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {

        String userId = AuthUtil.currentUserDetail().getId();
        PageRequest pageRequest = PageRequest.of(page, size);
        SliceList<PrivateMessageDto> privateMessageDtoSliceList = privateMessageService.getAllMessages(userId, channelId, pageRequest);

        return ResponseEntity.ok(privateMessageDtoSliceList);
    }

    @GetMapping(path = "{channelId}/status")
    public Map<String, Object> getUserStatus(@PathVariable String channelId)
            throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {
        String userId = AuthUtil.currentUserDetail().getId();
        String userName = AuthUtil.currentUserDetail().getUsername();
        UserPublicProfile loggedInUser = new UserPublicProfile(userId,userName);


        PrivateChannelProfile privateChannelProfile = privateChannelService.getChannelProfile(channelId , userId);
        UserPublicProfile otherPerson = privateChannelProfile.getMembers().stream()
                .filter(userPublicProfile -> !userPublicProfile.equals(loggedInUser))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Other user not found in channel"));

       Boolean online =  (Boolean) redisTemplate.opsForValue().get("user:"+ otherPerson.getUsername() + ":online");
       String lastSeen = (String) redisTemplate.opsForValue().get("user:" + otherPerson.getUsername() + ":lastSeen");

       Map<String, Object > response = new HashMap<>();
        response.put("username", otherPerson.getUsername());
        response.put("online", online != null ? online : false);
        response.put("lastSeen", lastSeen);

        return response;

    }

    @PostMapping("/blockage/{channelId}" )
    public ResponseEntity<Object> setBlockage(@PathVariable String channelId)
    throws  IllegalArgumentException, ChannelDoesNotExist,  InvalidOperation,
            UserDoesNotExist{
        try {
            privateChannelService.block(
                    AuthUtil.currentUserDetail().getId(), channelId);
            return ResponseEntity.ok().build();
        }catch (IllegalArgumentException e) {
            return new ResponseEntity<>
                    (new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    @MessageMapping("private/publishMessage")
    public void handlePrivateMessage(PublishMessageRequest request)
            throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {

        String senderId = AuthUtil.currentUserDetail().getId();

        if (!(request.getFrom().getId()).equals(senderId)) {
            throw new InvalidOperation("Sender of this message is not same as LoggedIn User");
        }

        String channelId = request.getChannelId();

        if(channelId == null || channelId.isBlank()){
            PrivateChannelProfile privateChannelProfile =
                    privateChannelService.createChannelBetween(senderId, request.getTo().getId());

            channelId = privateChannelProfile.getId();
        }

        PrivateMessageDto privateMessageDto = privateMessageService.createMessage(
                senderId,
                request.getChannelId(),
                request.getMessageContent(),
                request.getMessageType()
        );
        privateMessageService.deliverMessage(privateMessageDto);
    }

    @MessageMapping("/typing")
    public void handleTyping(TypingEvent typingEvent ){
       String username =  AuthUtil.currentUserDetail().getUsername();
       typingEvent.setSenderUsername(username);

       String channelId = typingEvent.getChannelId();

       messagingTemplate.convertAndSend("/topic/private-channel/"+ channelId +"/typing" ,
               typingEvent);

    }

    

}
