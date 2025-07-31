package com.rohit.ChatApplication.config;

import com.rohit.ChatApplication.data.UserDetail;
import com.rohit.ChatApplication.service.UsersDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class ChatAppUsernamePwdAuthenticationProvider implements AuthenticationProvider {

    private final UsersDetailsServiceImpl usersDetailsServiceImpl;
    private final PasswordEncoder passwordEncoder;

    public ChatAppUsernamePwdAuthenticationProvider(UsersDetailsServiceImpl usersDetailsServiceImpl,
                                                    PasswordEncoder passwordEncoder) {
        this.usersDetailsServiceImpl = usersDetailsServiceImpl;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userName = authentication.getName();
        String pwd = authentication.getCredentials().toString();


        UserDetail userDetails = usersDetailsServiceImpl.loadUserByUsername(userName);

        System.out.println("Username input: " + userName);
        System.out.println("Password input (plain): " + pwd);
        System.out.println("Password stored (hashed): " + userDetails.getPassword());
        System.out.println("Password match: " + passwordEncoder.matches(pwd, userDetails.getPassword()));


        if( passwordEncoder.matches(pwd , userDetails.getPassword())){
           return new UsernamePasswordAuthenticationToken(userName,pwd, userDetails.getAuthorities());
       }else{
           throw new BadCredentialsException("Invalid Password");
       }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
