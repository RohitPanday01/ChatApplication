package com.rohit.ChatApplication.service;

import com.rohit.ChatApplication.exception.UserDoesNotExist;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

public interface RegisterUserSessionManager {
    void registerUserSessionInLocalNodeMap(String username, WebSocketSession session , String userId)throws UserDoesNotExist;
    WebSocketSession getUserSessionInLocalNodeMap(String username);
    void unregisterUserSessionInLocalNodeMap(String username ,WebSocketSession session ) throws UserDoesNotExist;
    void registerUserSessionsInTheirGroups(String groupId , WebSocketSession session);
    Set<WebSocketSession>  unregisterUserSessionInTheirGroups(String groupId, WebSocketSession session);
    Set<WebSocketSession> getUserSessionsInTheirGroups(String groupId);

}
