package com.rohit.ChatApplication.service;

import com.rohit.ChatApplication.data.SliceList;
import com.rohit.ChatApplication.data.channel.profile.PrivateChannelProfile;
import com.rohit.ChatApplication.exception.UserDoesNotExist;
import com.rohit.ChatApplication.service.Typing.TypingSubscriber;
import com.rohit.ChatApplication.service.channel.PrivateChannelServiceImpl;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RegisterUserSession implements RegisterUserSessionManager {

    private final ConcurrentMap<String , WebSocketSession> userSessions =
            new ConcurrentHashMap<>();
    private  final Map<String , Set<WebSocketSession>> groupSessions =
            new ConcurrentHashMap<>();



    @Override
    public void registerUserSessionInLocalNodeMap(String username, WebSocketSession session , String userId) throws UserDoesNotExist {
        userSessions.putIfAbsent(username , session);


    }

    @Override
    public WebSocketSession getUserSessionInLocalNodeMap(String username) {
        return userSessions.getOrDefault(username , null);
    }

    @Override
    public void unregisterUserSessionInLocalNodeMap(String username ,WebSocketSession session , String userId) throws UserDoesNotExist {
        userSessions.remove(username ,  session );


    }

    @Override
    public void registerUserSessionsInTheirGroups(String groupId , WebSocketSession session){

        groupSessions.computeIfAbsent(groupId , g -> ConcurrentHashMap.newKeySet()).add(session);


    }

    @Override
    public Set<WebSocketSession>  unregisterUserSessionInTheirGroups(String groupId, WebSocketSession session){
        Set<WebSocketSession> sessions = groupSessions.getOrDefault(groupId ,Set.of());

        if(sessions != null){
            sessions.remove(session);

            if (sessions.isEmpty()) {
                groupSessions.remove(groupId);

            }
        }

        return sessions;
    }

    @Override
    public Set<WebSocketSession> getUserSessionsInTheirGroups(String groupId){
        return groupSessions.getOrDefault(groupId ,Set.of());
    }


}
