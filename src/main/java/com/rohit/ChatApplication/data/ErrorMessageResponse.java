package com.rohit.ChatApplication.data;

import lombok.Data;

@Data
public class ErrorMessageResponse {
    String message;

    public ErrorMessageResponse(String message) {
        this.message = message;
    }
}
