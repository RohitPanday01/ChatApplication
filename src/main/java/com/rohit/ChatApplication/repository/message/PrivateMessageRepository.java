package com.rohit.ChatApplication.repository.message;

import com.rohit.ChatApplication.entity.PrivateMessage;
import com.rohit.ChatApplication.entity.PrivateMessageId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage , PrivateMessageId> {

    Optional<PrivateMessage> findById(PrivateMessageId messageId);


    boolean existsByMessageIdMessageSeq(Long messageSeq);

//    @Query("""
//        SELECT pm
//        FROM PrivateMessage pm
//        WHERE pm.to.id = :userId OR pm.from.id = :userId
//        ORDER BY pm.createAt DESC
//    """)
//    Slice<PrivateMessage> getAllByUser(@Param("userId") UUID userId, Pageable pageable);


    @Query("""
        SELECT pm
        FROM PrivateMessage pm
        WHERE pm.messageId.privateChannelId = :channelId
        ORDER BY pm.messageId.messageSeq DESC
    """)
    Slice<PrivateMessage> getAllByChannel(@Param("channelId") UUID channelId, Pageable pageable);

    @Query("""
            SELECT MAX(p.messageSeq)
            FROM PrivateMessage p
            WHERE p.messageID.privateChannelId = :channelId
            """)
    Long findMessageSeqByChannelId(@Param("channelId") UUID channelId);



}
