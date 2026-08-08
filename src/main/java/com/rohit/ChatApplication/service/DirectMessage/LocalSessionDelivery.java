package com.rohit.ChatApplication.service.DirectMessage;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.service.RegisterUserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LocalSessionDelivery {

    private final Logger log = LoggerFactory.getLogger(LocalSessionDelivery.class);

    private final RegisterUserSession registerUserSession;
    private final HandleOfflineUser handleOfflineUser;
    private final ObjectMapper objectMapper;

    public LocalSessionDelivery(RegisterUserSession registerUserSession,
                                HandleOfflineUser handleOfflineUser,
                                ObjectMapper objectMapper){
        this.registerUserSession =registerUserSession;
        this.handleOfflineUser = handleOfflineUser;
        this.objectMapper = objectMapper;
    }

    public void handleLocalDelivery(Map<String, List<PrivateMessageDto>> messages){

        if (messages == null || messages.isEmpty()) return;



        messages.forEach((receiver, userMessages)-> {
            Thread.ofVirtual().name("ws-deliver-" + receiver).start(() ->{
                WebSocketSession session = registerUserSession.getUserSessionInLocalNodeMap(receiver);

                if (session == null || !session.isOpen()) {
                    userMessages.forEach(handleOfflineUser::handleOfflineMessagesForUser);
                    return;
                }

                // Sequentially write messages ONLY for this specific user
                for (PrivateMessageDto messageDto : userMessages) {
                    try {
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(messageDto)));
                        log.debug("Delivered messageId: {} to user: {}", messageDto.getId(), receiver);
                    } catch (Exception e) {
                        log.warn("WebSocket send blocked/failed for slow client: {}. Closing session.", receiver, e);

                        // Close the dead/slow socket and route remaining messages to offline storage
                        closeSessionSafely(session);
                        handleOfflineUser.handleOfflineMessagesForUser(messageDto);
                    }
                }
            });
        });

    }

    private void closeSessionSafely(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SESSION_NOT_RELIABLE);
            }
        } catch (Exception ignored) {}
    }


}
