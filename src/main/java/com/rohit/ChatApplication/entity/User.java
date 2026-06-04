package com.rohit.ChatApplication.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;


import java.time.LocalDateTime;
import java.util.HashSet;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name ="Users")
@AllArgsConstructor
@NoArgsConstructor
@BatchSize(size = 64)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id",columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID userId;

    @Column(name ="user_name", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name ="email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false ,length = 255)
    private String password;

    @Column(name ="full_name", nullable = false,length = 50)
    private String FullName;

    @Transient
    private Set<PrivateChannel> privateChannels = new HashSet<>();

    @OneToMany(mappedBy = "user1")
    private Set<PrivateChannel> blockedChannel = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<GroupMember> memberships = new HashSet<>();




    @Column(name = "profile_photo_path", length = 250)
    private String profilePhotoPath;

    @Transient
    private Set<PrivateChannel> getAllPrivateChannels(){
       return privateChannels;
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User user)) return false;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}

