package com.rohit.ChatApplication.service.MessageSequencing;

import com.rohit.ChatApplication.repository.message.PrivateMessageRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RedisSequenceService implements SequenceService{

    private final RedisTemplate<String, Long> redisTemplate;
    private final PrivateMessageRepository privateMessageRepository;

    public RedisSequenceService(RedisTemplate<String, Long> redisTemplate,
                                PrivateMessageRepository privateMessageRepository) {
        this.redisTemplate = redisTemplate;
        this.privateMessageRepository = privateMessageRepository;
    }

    @Override
    public long nextSequence(UUID channelId) {
        String key = "conv_seq:" + channelId;

        try {
            Long seq = redisTemplate.opsForValue().increment(key);

            if (seq == null || seq <= 1) {
                // suspicious → Redis might have restarted
                return recoverFromDb(channelId, key);
            }
            return seq;

        } catch (Exception ex) {
            //  Redis down → fallback
            return recoverFromDb(channelId, key);
        }
    }



    private long recoverFromDb(UUID channelId, String key ){
        // double check if redis is still down
        Long existing = redisTemplate.opsForValue().get(key);
        if (existing != null && existing > 1) {
            return redisTemplate.opsForValue().increment(key);
        }

        //  rebuild from DB
        Long maxSeq = privateMessageRepository.findMessageSeqByChannelId(channelId);

        long base = (maxSeq != null) ? maxSeq : 0;

        redisTemplate.opsForValue().set(key, base);

        return redisTemplate.opsForValue().increment(key);
    }
}
