package com.rohit.ChatApplication.Batch.LastSeen;

import com.rohit.ChatApplication.data.UserLastSeen;
import com.rohit.ChatApplication.entity.User;
import com.rohit.ChatApplication.repository.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class LastSeenWriter implements ItemWriter<UserLastSeen> {
    private final Logger log = LoggerFactory.getLogger(LastSeenWriter.class);

    private final UserRepo userRepo;

    public LastSeenWriter(UserRepo userRepo){
        this.userRepo = userRepo;
    }

    @Override
    public void write(Chunk<? extends UserLastSeen> chunk) throws Exception {
        List<User> toUpdate = new ArrayList<>();

       for( UserLastSeen item : chunk.getItems()){

           log.info("->>>>> Writing LastSeen for user: {}, value: {}",
                   item.getUsername(), item.getLastSeen());

           Optional<User> optionalUser = userRepo.findByUsername(item.getUsername());

           optionalUser.ifPresent((user)->{
               user.setLastSeen(item.getLastSeen());
               toUpdate.add(user);
           });

       }

        if (!toUpdate.isEmpty()) {
            userRepo.saveAll(toUpdate);
            log.info("->>>>> Saved {} users with updated lastSeen", toUpdate.size());
        }

    }
}
