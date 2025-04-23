package com.rohit.ChatApplication.dao;

import com.rohit.ChatApplication.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupRepo extends JpaRepository<Group , UUID> {


}
