package com.rohit.ChatApplication.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@MappedSuperclass
@Data
public class TimeStampBase {

   @CreationTimestamp Instant createAt;

   @UpdateTimestamp Instant updatedAt;

}
