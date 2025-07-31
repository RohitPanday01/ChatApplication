package com.rohit.ChatApplication.repository;

import com.rohit.ChatApplication.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {


     Optional<User> findByUsername(String username);

     Boolean existsByUsername(String username);

     Boolean existsByEmail(String email);

     @Transactional
     @Modifying
     @Query("UPDATE User u SET u.lastSeen = :localDateTime WHERE u.username = :username")
     void updateLastSeen(@Param("username") String username, @Param("localDateTime") LocalDateTime localDateTime);


}
