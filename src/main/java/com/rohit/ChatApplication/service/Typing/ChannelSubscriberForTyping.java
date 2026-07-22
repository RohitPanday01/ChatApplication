package com.rohit.ChatApplication.service.Typing;


public interface ChannelSubscriberForTyping {
    void subscribePrivateChannel(String nodeId);
    void subscribeGroup(String groupId);
    void unsubscribeGroup(String groupId);
    void unsubscribePrivateChannel(String nodeID);
}
