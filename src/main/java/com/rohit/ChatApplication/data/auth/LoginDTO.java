package com.rohit.ChatApplication.data.auth;


import lombok.Getter;

@Getter
public class LoginDTO {
    private String userName;
    private String password;

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
