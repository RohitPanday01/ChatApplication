package com.rohit.ChatApplication.repository.channel;

import com.rohit.ChatApplication.data.SliceList;
import com.rohit.ChatApplication.entity.PrivateChannel;
import com.rohit.ChatApplication.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PrivateChannelRepository extends JpaRepository<PrivateChannel , UUID> {
    Optional<PrivateChannel> findById(UUID id);

//    Optional<PrivateChannel> findByUniqueUserIds(String id);



    @Query("""
    SELECT pc FROM PrivateChannel pc
    WHERE pc.user1.id = :userId OR pc.user2.id = :userId
    ORDER BY pc.updatedAt DESC
    """)
    Slice<PrivateChannel> findByUserIdOrderByUpdateAtDesc(@Param("userId") UUID userId , Pageable pageable);

    @Query("""
    SELECT pc FROM PrivateChannel pc
    WHERE pc.user1.id = :userId OR pc.user2.id = :userId
    """)
    List<PrivateChannel> findAllChannelForUser(@Param("userId") UUID userId);


    @Query("""
        SELECT CASE
            WHEN p.user1.username = :username THEN p.user2.username
            ELSE p.user1.username
        END
        FROM PrivateChannel p
        WHERE p.user1.username = :username OR p.user2.username = :username
    """)
    Set<String> findUsersWhoCare(@Param("username") String username);

    @Query("""
    SELECT pc FROM PrivateChannel pc
    WHERE pc.user1.id = :userId1 AND pc.user2.id = :userId2
""")
    Optional<PrivateChannel> findChannelBetweenUsers(@Param("userId1") UUID userId1,
                                                     @Param("userId2") UUID userId2);


    default boolean isPrivateChannelExistsBetween(User user1, User user2) {
        UUID id1 = user1.getUserId();
        UUID id2 = user2.getUserId();

        // Enforce consistent order
        if (id1.compareTo(id2) > 0) {
            UUID temp = id1;
            id1 = id2;
            id2 = temp;
        }

        return findChannelBetweenUsers(id1, id2).isPresent();
    }
}
