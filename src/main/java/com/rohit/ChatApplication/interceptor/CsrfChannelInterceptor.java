package com.rohit.ChatApplication.interceptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

import java.nio.channels.Channel;

@Component
public class CsrfChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String csrfHeader = accessor.getFirstNativeHeader("X-CSRF-TOKEN");
            CsrfToken csrfToken = (CsrfToken) accessor.getHeader(CsrfToken.class.getName());

            if (csrfToken == null || csrfHeader == null || !csrfHeader.equals(csrfToken.getToken())) {
                throw new AccessDeniedException("Invalid CSRF token on WebSocket CONNECT");
            }
        }
        return message;
    }
}
