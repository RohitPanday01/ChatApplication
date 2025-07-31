package com.rohit.ChatApplication.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
public class UserLastSeen {
    String username;
    boolean isOnline;
    LocalDateTime lastSeen;

    public UserLastSeen(String username, Boolean isOnline, LocalDateTime lastSeen) {
        this.username = username;
        this.isOnline = isOnline;
        this.lastSeen = lastSeen;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserLastSeen that)) return false;
        return Objects.equals(username, that.username) && Objects.equals(lastSeen, that.lastSeen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, lastSeen);
    }
}
