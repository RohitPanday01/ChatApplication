package com.rohit.ChatApplication.service;

import com.rohit.ChatApplication.data.SliceList;
import com.rohit.ChatApplication.data.channel.profile.PrivateChannelProfile;
import com.rohit.ChatApplication.exception.UserDoesNotExist;
import com.rohit.ChatApplication.service.Typing.ChannelSubscriberForTyping;
import com.rohit.ChatApplication.service.Typing.TypingSubscriber;
import com.rohit.ChatApplication.service.channel.PrivateChannelServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SessionSubscriptionManager {
    private final ChannelSubscriberForTyping channelSubscriberForTyping;
    private final PrivateChannelServiceImpl privateChannelService;

    public SessionSubscriptionManager(ChannelSubscriberForTyping channelSubscriberForTyping,
                                      PrivateChannelServiceImpl privateChannelService) {
       this.channelSubscriberForTyping = channelSubscriberForTyping;
        this.privateChannelService = privateChannelService;
    }

    public void subscribeUserChannels(String userId) throws UserDoesNotExist {
        int page = 0;
        int size = 10;

        while (true) {
            SliceList<PrivateChannelProfile> privateChannelProfileSliceList =
                    privateChannelService.getAllChannel(userId, page, size);

            for (PrivateChannelProfile profile : privateChannelProfileSliceList.getList()) {
                channelSubscriberForTyping.subscribePrivateChannel(profile.getId());
            }

            if (!privateChannelProfileSliceList.isHasNext()) {
                break;
            }
            page++;
        }
    }

    public void unsubscribeUserChannels(String userId) throws UserDoesNotExist {
        int page = 0;
        int size = 10;

        while (true) {
            SliceList<PrivateChannelProfile> privateChannelProfileSliceList =
                    privateChannelService.getAllChannel(userId, page, size);

            for (PrivateChannelProfile profile : privateChannelProfileSliceList.getList()) {
                channelSubscriberForTyping.unsubscribePrivateChannel(profile.getId());
            }

            if (!privateChannelProfileSliceList.isHasNext()) {
                break;
            }
            page++;
        }
    }

    public void subscribeGroup(String groupId) {
        channelSubscriberForTyping.subscribeGroup(groupId);
    }

    public void unsubscribeGroup(String groupId) {
        channelSubscriberForTyping.unsubscribeGroup(groupId);
    }
}
