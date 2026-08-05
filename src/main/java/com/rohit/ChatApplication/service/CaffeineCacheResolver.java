package com.rohit.ChatApplication.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CaffeineCacheResolver {

    private final Cache<UUID ,Long> caffeineCacheForPrevMessageSeq ;
    private final JdbcTemplate jdbcTemplate;


    public CaffeineCacheResolver(@Qualifier("channelPrevMessageSeq") Cache<UUID ,Long> caffeineCacheForPrevMessageSeq,
                                 JdbcTemplate jdbcTemplate){
        this.caffeineCacheForPrevMessageSeq = caffeineCacheForPrevMessageSeq;
        this.jdbcTemplate = jdbcTemplate;

    }


    public Long handlePrevMessageSeq(UUID channelId , Long currMessageSeq){

       Long prevMessageSeq = caffeineCacheForPrevMessageSeq.get(channelId , this::fetchLatestPrevSeqFromDb);

        if (currMessageSeq != null && currMessageSeq > prevMessageSeq) {
            caffeineCacheForPrevMessageSeq.put(channelId, currMessageSeq);
        }
        return prevMessageSeq;
    }

    private Long fetchLatestPrevSeqFromDb(UUID channelId){


        String sql = "SELECT Max(prev_msg_seq) from private_message where channel_id = ?";
        return jdbcTemplate.queryForObject(sql , Long.class , channelId );

    }
}
