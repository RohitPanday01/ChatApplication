package com.rohit.ChatApplication.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name ="Users")
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    private UUID userid;

    private String role = "USER";

    @Column(name ="user_name", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name ="email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false ,length = 255)
    private String password;

    @Column(name ="full_name", nullable = false,length = 50)
    private String FullName;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "is_online")
    private boolean isOnline;

    @Column(name = "profile_photo_path", length = 250)
    private String profilePhotoPath;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void OnCreate(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void OnUpdate(){
        this.updatedAt = LocalDateTime.now();
    }


}
