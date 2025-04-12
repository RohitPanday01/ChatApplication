package com.rohit.ChatApplication.dao;

import com.rohit.ChatApplication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {


     User findByUsername(String Username);

}
