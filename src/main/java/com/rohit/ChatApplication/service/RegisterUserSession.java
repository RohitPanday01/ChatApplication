package com.rohit.ChatApplication.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RegisterUserSession implements RegisterUserSessionManager {

    private final ConcurrentMap<String , WebSocketSession> userSessions =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<ByteBuffer, WebSocketSession> userUUIDSession =
            new ConcurrentHashMap<>();
    private  final Map<String , Set<WebSocketSession>> groupSessions =
            new ConcurrentHashMap<>();



    @Override
    public void registerUserSessionInLocalNodeMap(String username, WebSocketSession session , ByteBuffer sessionKey) {
        userSessions.put(username , session);
        userUUIDSession.put(sessionKey, session);


    }

    public int getUserSessionInLocalNodeMapSize(){
        return this.userSessions.size();
    }

    @Override
    public WebSocketSession getUserSessionInLocalNodeMap(String username) {
        return userSessions.getOrDefault(username , null);

    }
    public WebSocketSession getUserSessionInLocalNode(ByteBuffer sessionKey){
        return userUUIDSession.get(sessionKey);
    }

    @Override
    public void unregisterUserSessionInLocalNodeMap(String username ,WebSocketSession session ) {
        try{
            userSessions.remove(username ,  session );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void unregisterUserSessionInLocalNodeMap(ByteBuffer sessionKey ,WebSocketSession session ) {
        try{
            userUUIDSession.remove(sessionKey ,  session );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void registerUserSessionsInTheirGroups(String groupId , WebSocketSession session){

        groupSessions.computeIfAbsent(groupId , g -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public Set<WebSocketSession>  unregisterUserSessionInTheirGroups(String groupId, WebSocketSession session){
        Set<WebSocketSession> sessions =
                groupSessions.get(groupId);

        if (sessions == null) {
            return Set.of();
        }

        sessions.remove(session);

        if (sessions.isEmpty()) {
            groupSessions.remove(groupId);
        }

        return sessions;
    }

    @Override
    public Set<WebSocketSession> getUserSessionsInTheirGroups(String groupId){
        return groupSessions.getOrDefault(groupId ,Set.of());
    }


}
