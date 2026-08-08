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
import com.rohit.ChatApplication.repository.message.ChatSyncRepository;
import com.rohit.ChatApplication.service.DirectMessage.DMDeliveryListener;
import com.rohit.ChatApplication.service.DirectMessage.DirectMessageProducer;
import com.rohit.ChatApplication.service.GroupMessage.FanOutService;
import com.rohit.ChatApplication.service.channel.PrivateChannelServiceImpl;
import com.rohit.ChatApplication.service.message.PrivateMessageServiceImpl;
import com.rohit.ChatApplication.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/channel/private")
public class PrivateChannelController {
    private final Logger log = LoggerFactory.getLogger(PrivateChannelController.class);


    private final RedisTemplate<String, Object> redisTemplate;
    private final PrivateChannelServiceImpl privateChannelService;
    private final PrivateMessageServiceImpl privateMessageService;

    private DirectMessageProducer directMessageProducer;
    private ChatSyncRepository chatSyncRepository;


    @Autowired
    public PrivateChannelController(
            RedisTemplate<String, Object> redisTemplate,
            PrivateChannelServiceImpl privateChannelService,
            PrivateMessageServiceImpl privateMessageService, DirectMessageProducer directMessageProducer,
            ChatSyncRepository chatSyncRepository) {
        this.redisTemplate = redisTemplate;
        this.privateChannelService = privateChannelService;
        this.privateMessageService = privateMessageService;
        this.directMessageProducer = directMessageProducer;
        this.chatSyncRepository = chatSyncRepository;
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
    public ResponseEntity<?>
    getAllPrivateChannelForUser(@RequestParam int page, @RequestParam int size)
            {
                try{
                    PageRequest pageRequest = PageRequest.of(page, size);
                    String user = AuthUtil.currentUserDetail().getId();

                    SliceList<PrivateChannelProfile> privateChannelProfileSliceList  =
                            privateChannelService.getAllChannel(user,
                                    pageRequest.getPageNumber(), pageRequest.getPageSize());

                    return ResponseEntity.ok( privateChannelProfileSliceList);

                } catch (UserDoesNotExist e) {
                    return ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(new ErrorMessageResponse(
                                    e.getMessage()));
                }

    }

    @GetMapping(path = "{channelId}/messages")
    public ResponseEntity<?>
     getAllMessages(@PathVariable String channelId , @RequestParam int page, @RequestParam int size ){


        try {

            String userId = AuthUtil.currentUserDetail().getId();
            PageRequest pageRequest = PageRequest.of(page, size);
            SliceList<PrivateMessageDto> privateMessageDtoSliceList =
                    privateMessageService.getAllMessages(userId, channelId, pageRequest);

            return ResponseEntity.ok(privateMessageDtoSliceList);

        } catch (UserDoesNotExist e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessageResponse(
                            e.getMessage()));

        } catch (ChannelDoesNotExist e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessageResponse(
                            e.getMessage()));

        } catch (InvalidOperation e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorMessageResponse(
                            e.getMessage()));
        }



    }



    @PostMapping("/blockage/{channelId}" )
    public ResponseEntity<Object> setBlockage(@PathVariable String channelId) {
        try {
            privateChannelService.block(
                    AuthUtil.currentUserDetail().getId(), channelId);
            return ResponseEntity.ok().build();
        }catch (IllegalArgumentException | UserDoesNotExist | ChannelDoesNotExist | InvalidOperation e) {
            return new ResponseEntity<>
                    (new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }


    @PostMapping("private/publishMessage")
    public ResponseEntity<?> handlePrivateMessage(@RequestBody  PublishMessageRequest request)
            {

        try{
            String senderId = request.getFrom().getId();
            String receiverId = request.getTo().getId();
            String senderUsername = request.getFrom().getUsername();
            String receiverUsername = request.getTo().getUsername();

//            if (!(request.getFrom().getId()).equals(senderId)) {
//                throw new InvalidOperation("Sender of this message is not same as LoggedIn User");
//            }
//
            String channelId = request.getChannelId();

            if(channelId == null || channelId.isBlank()){
                PrivateChannelProfile privateChannelProfile =
                        privateChannelService.createChannelBetween(senderId, request.getTo().getId());

                channelId = privateChannelProfile.getId();
            }

            PrivateMessageDto privateMessageDto = privateMessageService.createMessage(
                    senderId,
                    senderUsername,
                    receiverId,
                    receiverUsername,
                    channelId,
                    request.getMessageContent(),
                    request.getMessageType()
            );

            if (privateMessageDto == null) {
                throw new IllegalStateException("Failed to create message DTO");
            }
    //        privateMessageDto.setIngressTimestampNanos(System.currentTimeMillis());


                directMessageProducer.sendDirectMessage(privateMessageDto);
                return ResponseEntity.ok("Message Sent");
        } catch (ChannelDoesNotExist | InvalidOperation | UserDoesNotExist e) {
            return new ResponseEntity<>
                    (new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
        }

//        messageProducer.sendDirectMessage(dm); // fire & forget
//        return ResponseEntity.accepted().body("Message sending...");

    }

    @GetMapping("/sync")
    public ResponseEntity<?> syncMessages(
            @RequestParam("channelId") String channelId,
            @RequestParam("afterMessageId") long afterMessageId,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {

        // 1. Security Check: Prevent massive DB pull requests
        int safeLimit = Math.min(limit, 100);

        try{
            // 2. Fast Path DB Fetch
            List<PrivateMessageDto> missingMessages =
                    chatSyncRepository.fetchMessagesAfter(channelId, afterMessageId, safeLimit);

            // 3. Return 200 OK with the array of messages
            return ResponseEntity.ok(missingMessages);
        }catch (DataAccessException e){
            return new ResponseEntity<>
                    (new ErrorMessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
        }

    }

    

}
