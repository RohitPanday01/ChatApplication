package com.rohit.ChatApplication.exception;

public class DatabaseRuntimeException extends RuntimeException {
    public DatabaseRuntimeException(String message) {
        super(message);
    }
}
