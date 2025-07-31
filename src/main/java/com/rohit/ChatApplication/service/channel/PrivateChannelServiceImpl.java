package com.rohit.ChatApplication.service.channel;

import com.rohit.ChatApplication.data.SliceList;
import com.rohit.ChatApplication.data.channel.profile.PrivateChannelProfile;
import com.rohit.ChatApplication.exception.ChannelDoesNotExist;
import com.rohit.ChatApplication.exception.InvalidOperation;
import com.rohit.ChatApplication.exception.UserDoesNotExist;
import com.rohit.ChatApplication.repository.channel.PrivateChannelRepository;
import com.rohit.ChatApplication.entity.PrivateChannel;
import com.rohit.ChatApplication.entity.User;
import com.rohit.ChatApplication.service.UsersDetailsServiceImpl;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PrivateChannelServiceImpl {

    private final UsersDetailsServiceImpl userService;
    private final PrivateChannelRepository privateChannelRepository;

    public PrivateChannelServiceImpl(UsersDetailsServiceImpl userService,
                                     PrivateChannelRepository privateChannelRepository) {
        this.userService = userService;
        this.privateChannelRepository = privateChannelRepository;
    }

    private PrivateChannel getChannelById(UUID channelId) throws ChannelDoesNotExist  {

            return privateChannelRepository
                    .findById(channelId)
                    .orElseThrow(
                            () -> new ChannelDoesNotExist(
                                            "channel with id=%s does not exist !".formatted(channelId)));
    }


    public PrivateChannelProfile createChannelBetween(String fromUserId , String toUserId) throws UserDoesNotExist ,ChannelDoesNotExist {
        if (fromUserId == null || toUserId == null) {
            throw new IllegalArgumentException("User IDs must not be null.");
        }

        if (fromUserId.equals(toUserId)) {
            throw new IllegalStateException("You cannot create a channel with yourself.");
        }

        UUID uuidA, uuidB;
        try {
            uuidA = UUID.fromString(fromUserId);
            uuidB = UUID.fromString(toUserId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid UUID format for user IDs.");
        }

        User userA = userService.getUserById(fromUserId);
        User userB = userService.getUserById(toUserId);


        if (uuidA.compareTo(uuidB) > 0) {
            User temp = userA;
            userA = userB;
            userB = temp;
        }

        if (privateChannelRepository.isPrivateChannelExistsBetween(userA, userB)) {
            throw new ChannelDoesNotExist("Private channel between '%s' and '%s' already exists.");
        }

        PrivateChannel channel = new PrivateChannel(userA, userB);
        privateChannelRepository.saveAndFlush(channel);

        return new PrivateChannelProfile(channel);
    }

    @Transactional
    public SliceList<PrivateChannelProfile> getAllChannel(String userID, int page , int size) throws
            UserDoesNotExist, IllegalArgumentException{

        UUID uuidA;
        try {
            uuidA = UUID.fromString(userID);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid UUID format for user IDs.");
        }

        User user = userService.getUserById(userID);
        Pageable pageable = PageRequest.of(page, size ,  Sort.by("createAt").descending());

        Slice<PrivateChannel> slice = privateChannelRepository.findByUserIdOrderByUpdateAtDesc(uuidA ,pageable);

        return new SliceList<>(
                slice.getNumber(),
                slice.getSize(),
                slice.stream()
                        .map(PrivateChannelProfile::new)
                        .collect(Collectors.toList()),
                slice.hasNext()
        );
    }

    @Transactional
    public PrivateChannelProfile getChannelProfile(String channelId , String userId) throws
            UserDoesNotExist , ChannelDoesNotExist , InvalidOperation {
        UUID userUuid , channelUuid ;
        try {
            userUuid = UUID.fromString(userId);
            channelUuid = UUID.fromString(channelId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid UUID format for user IDs.");
        }

        User user = userService.getUserById(userId);

        PrivateChannel channel = getChannelById(channelUuid);

        if(channel.getUser1().getUserId() != userUuid
                || channel.getUser2().getUserId() != userUuid){
            throw new InvalidOperation("user is not in members of the channel !");
        }

        return new PrivateChannelProfile(channel);
    }



    @Transactional
    public void block(String userId, String channelId) throws UserDoesNotExist , ChannelDoesNotExist, InvalidOperation ,IllegalArgumentException {
        UUID userUuid , channelUuid ;
        try {
            userUuid = UUID.fromString(userId);
            channelUuid = UUID.fromString(channelId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format for user IDs.");
        }

        User user = userService.getUserById(userId);
        PrivateChannel channel = getChannelById(channelUuid);
        if (channel.getUser1().getUserId() != userUuid
                || channel.getUser2().getUserId() != userUuid)
            throw new InvalidOperation("user is not in members of the channel !");

        channel.block(user);
        privateChannelRepository.save(channel);
    }




}
