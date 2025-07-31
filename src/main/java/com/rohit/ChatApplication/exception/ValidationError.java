package com.rohit.ChatApplication.exception;

public class ValidationError extends IllegalArgumentException {
    public ValidationError(String message) {
        super(message);
    }
}
