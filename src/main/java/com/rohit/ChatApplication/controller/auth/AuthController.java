package com.rohit.ChatApplication.controller.auth;

import com.rohit.ChatApplication.DTO.LoginDTO;
import com.rohit.ChatApplication.DTO.RegisterUserDTO;
import com.rohit.ChatApplication.DTO.TokenDTO;
import com.rohit.ChatApplication.dao.UserRepo;
import com.rohit.ChatApplication.entity.User;
import com.rohit.ChatApplication.service.JwtService;
import com.rohit.ChatApplication.service.UsersDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsersDetailsServiceImpl usersDetailsService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepo userRepo;



    @PostMapping("/Signup")
    ResponseEntity<?> registerUser(@RequestBody RegisterUserDTO registerUserDTO){
        try{
            String hashPwd = passwordEncoder.encode(registerUserDTO.getPassword());

            if(userRepo.existsByUsername(registerUserDTO.getUserName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Username already taken");
            }

            User user = new User();
            user.setUsername(registerUserDTO.getUserName());
            user.setEmail(registerUserDTO.getEmail());
            user.setPassword(hashPwd);
            user.setFullName(registerUserDTO.getFullName());

            User savedUser = userRepo.save(user);

            if(savedUser.getUserId() != null){
                return ResponseEntity.status(HttpStatus.CREATED).
                        body("Given user details are successfully registered");
            }else{
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                        body("User registration failed");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).
                    body("An exception occurred: " + e.getMessage());
        }
    }

    @PostMapping("/Signin")
    ResponseEntity<?> authenticateUser(@RequestBody LoginDTO loginDTO) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getUserName(),
                            loginDTO.getPassword()
                    )
            );

            UserDetails userDetails = usersDetailsService.loadUserByUsername(loginDTO.getUserName());
            String token = jwtService.generateToken(userDetails);

            return ResponseEntity.ok(new TokenDTO(token));

        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong" + e.getMessage());
        }

    }
}
