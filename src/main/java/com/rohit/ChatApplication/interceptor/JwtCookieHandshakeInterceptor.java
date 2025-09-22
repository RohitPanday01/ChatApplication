package com.rohit.ChatApplication.interceptor;

import com.rohit.ChatApplication.data.UserDetail;
import com.rohit.ChatApplication.service.JwtService;
import com.rohit.ChatApplication.service.UsersDetailsServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

@Component
public class JwtCookieHandshakeInterceptor implements HandshakeInterceptor {

    private final Logger log = LoggerFactory.getLogger(JwtCookieHandshakeInterceptor.class);

    private final JwtService jwtService;
    private final UsersDetailsServiceImpl usersDetailsService;

    public JwtCookieHandshakeInterceptor(JwtService jwtService, UsersDetailsServiceImpl usersDetailsService) {
        this.jwtService = jwtService;
        this.usersDetailsService = usersDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if(request instanceof ServletServerHttpRequest){
            HttpServletRequest servletRequest =  ((ServletServerHttpRequest) request).getServletRequest();
            String jwtToken  = getJwtFromCookie(servletRequest);

            log.info("->>>>>>>>>>>>>>>>>> jwtToken in JwtCookieHandshakeInterceptor: {}", jwtToken);

            if (jwtToken != null) {
                String username =  jwtService.extractUserName(jwtToken);
                UserDetail userDetails = usersDetailsService.loadUserByUsername(username);
                if (jwtService.validateToken(jwtToken, userDetails)){
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    attributes.put("userDetail", userDetails);

                    response.setStatusCode(HttpStatus.ACCEPTED);
                    return true;
                }
            }
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }

    private String getJwtFromCookie(HttpServletRequest request){
        if(request.getCookies() == null) return null;

        for (Cookie cookie :request.getCookies()){
            if("access_token".equals(cookie.getName())){
                return cookie.getValue();
            }
        }
        return null;
    }

}
