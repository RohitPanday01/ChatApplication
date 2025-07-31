package com.rohit.ChatApplication.Batch.LastSeen;

import com.rohit.ChatApplication.data.UserLastSeen;
import com.rohit.ChatApplication.entity.User;
import com.rohit.ChatApplication.repository.UserRepo;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class LastSeenWriter implements ItemWriter<UserLastSeen> {

    private UserRepo userRepo;

    public LastSeenWriter(UserRepo userRepo){
        this.userRepo = userRepo;
    }

    @Override
    public void write(Chunk<? extends UserLastSeen> chunk) throws Exception {

       for( UserLastSeen item : chunk.getItems()){

           Optional<User> optionalUser = userRepo.findByUsername(item.getUsername());

           optionalUser.ifPresent((user)->{
               user.setLastSeen(item.getLastSeen());
               userRepo.save(user);
           });

       }
    }
}
