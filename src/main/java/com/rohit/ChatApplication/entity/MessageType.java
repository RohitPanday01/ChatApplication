package com.rohit.ChatApplication.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    TEXT("TEXT"),
    IMAGE("IMAGE"),
    VIDEO("VIDEO"),
    INVITATION("INVITATION"),
    JOIN("JOIN"),
    LEAVE("LEAVE"),
    BAN("BAN"),
    UNBAN("UNBAN");

    private final String value;

    MessageType(String role) {
        this.value = role;
    }

    @JsonCreator
    public static MessageType fromJson(String value) {
        for (MessageType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid message type: " + value);
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }

}
