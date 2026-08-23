package com.rohit.ChatApplication.service;

import com.rohit.ChatApplication.exception.UserDoesNotExist;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

public interface RegisterUserSessionManager {
    void registerUserSessionInLocalNodeMap(String username, WebSocketSession session , ByteBuffer sessionKey)throws UserDoesNotExist;
    WebSocketSession getUserSessionInLocalNodeMap(String username);
    void unregisterUserSessionInLocalNodeMap(String username ,WebSocketSession session ) throws UserDoesNotExist;
    void registerUserSessionsInTheirGroups(String groupId , WebSocketSession session);
    Set<WebSocketSession>  unregisterUserSessionInTheirGroups(String groupId, WebSocketSession session);
    Set<WebSocketSession> getUserSessionsInTheirGroups(String groupId);

}
