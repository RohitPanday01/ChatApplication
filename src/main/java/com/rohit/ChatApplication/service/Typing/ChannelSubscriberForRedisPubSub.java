package com.rohit.ChatApplication.service.Typing;


public interface ChannelSubscriberForRedisPubSub {
    void  subscribePrivateChannel(String nodeId);
    void subscribeGroup(String groupId);
    void unsubscribeGroup(String groupId);
    void unsubscribePrivateChannel(String nodeID);
}
