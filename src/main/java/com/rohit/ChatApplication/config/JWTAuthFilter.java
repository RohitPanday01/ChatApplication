package com.rohit.ChatApplication.config;

import com.rohit.ChatApplication.service.GroupMessage.FanOutService;
import com.rohit.ChatApplication.service.JwtService;
import com.rohit.ChatApplication.service.UsersDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JWTAuthFilter extends OncePerRequestFilter {
    private final Logger log = LoggerFactory.getLogger(JWTAuthFilter.class);

    private final JwtService jwtService;
    private final UsersDetailsServiceImpl usersDetailsService;

    public JWTAuthFilter(JwtService jwtService, UsersDetailsServiceImpl usersDetailsService) {
        this.jwtService = jwtService;
        this.usersDetailsService = usersDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

//        final String authorizationHeader = request.getHeader("Authorization");
        log.info("just above getJwtFromCOokie");
        final String jwtToken = getJwtFromCookie(request);
        log.info("jwtToken: {}", jwtToken);


        final String username;

        if(jwtToken == null ){
            filterChain.doFilter(request , response);
            return ;
        }
//        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        jwtToken = authorizationHeader.substring(7);
        username = jwtService.extractUserName(jwtToken);
        log.info("extracted username from token: {}", username );

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = usersDetailsService.loadUserByUsername(username);
            if (jwtService.validateToken(jwtToken, userDetails)){

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                log.info("UsernamePasswordAuthenticationToken: {}", authenticationToken);
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                log.info("SecurityContextHolder  get context .g et authentication: {}", SecurityContextHolder.getContext().getAuthentication());


            }
        }
        filterChain.doFilter(request, response);
    }

    private String getJwtFromCookie(HttpServletRequest request){
        if(request.getCookies() == null){
            return null;
        }
       Cookie[] cookies = request.getCookies();
       for(Cookie cookie : cookies){
           if("access_token".equals(cookie.getName())){
               return  cookie.getValue();
           }
       }
       return null;
    }
}
