package com.rohit.ChatApplication.config.WebSocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

import static org.springframework.messaging.simp.SimpMessageType.MESSAGE;
import static org.springframework.messaging.simp.SimpMessageType.SUBSCRIBE;

@Configuration
@EnableWebSocketSecurity
public class WebSocketAuthorizationConfig {

        @Bean
        public AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
            messages
                    .nullDestMatcher().authenticated()
                    .simpSubscribeDestMatchers("/user/queue/errors").permitAll()
                    .simpTypeMatchers(MESSAGE, SUBSCRIBE).denyAll()
                    .anyMessage().denyAll();

            return messages.build();
        }
}
