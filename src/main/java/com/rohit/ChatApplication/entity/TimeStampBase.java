package com.rohit.ChatApplication.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@MappedSuperclass
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeStampBase {

   @CreationTimestamp Instant createAt;

   @UpdateTimestamp Instant updatedAt;

}
