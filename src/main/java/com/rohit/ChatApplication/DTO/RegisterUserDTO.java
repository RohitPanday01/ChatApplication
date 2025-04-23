package com.rohit.ChatApplication.DTO;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterUserDTO {

    private String userName;
    private String password;
    private String email;
    private String fullName;


}
