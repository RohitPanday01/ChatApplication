package com.rohit.ChatApplication.service.DirectMessage;

import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HandleOfflineUser {

    private final Logger log = LoggerFactory.getLogger(HandleOfflineUser.class);

    public HandleOfflineUser(){

    }

    public void  handleOfflineMessagesForUser(PrivateMessageDto messageDto){

    }
}
