package com.rohit.ChatApplication.service.message;

import com.rohit.ChatApplication.data.SliceList;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import com.rohit.ChatApplication.entity.MessageType;
import com.rohit.ChatApplication.entity.PrivateChannel;
import com.rohit.ChatApplication.entity.PrivateMessage;
import com.rohit.ChatApplication.entity.User;
import com.rohit.ChatApplication.exception.ChannelDoesNotExist;
import com.rohit.ChatApplication.exception.InvalidOperation;
import com.rohit.ChatApplication.exception.UserDoesNotExist;
import com.rohit.ChatApplication.repository.channel.PrivateChannelRepository;
import com.rohit.ChatApplication.repository.message.PrivateMessageRepository;
import com.rohit.ChatApplication.service.UsersDetailsServiceImpl;
import com.rohit.ChatApplication.service.channel.PrivateChannelServiceImpl;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PrivateMessageServiceImpl{

    private final UsersDetailsServiceImpl usersDetailsService;
    private final PrivateMessageRepository privateMessageRepository;
    private final PrivateChannelRepository privateChannelRepository;


    public PrivateMessageServiceImpl(
            UsersDetailsServiceImpl usersDetailsService,
            PrivateChannelRepository privateChannelRepository,
            PrivateMessageRepository privateMessageRepository) {

        this.usersDetailsService = usersDetailsService;
        this.privateMessageRepository = privateMessageRepository;
        this.privateChannelRepository = privateChannelRepository;

    }

    private PrivateChannel getChannelById(UUID channelId) throws ChannelDoesNotExist {

        return privateChannelRepository
                .findById(channelId)
                .orElseThrow(
                        () -> new ChannelDoesNotExist(
                                "channel with id=%s does not exist !".formatted(channelId)));
    }


    public Optional<PrivateMessage> toEntity(PrivateMessageDto messageDto){

        try{
            PrivateChannel channel = getChannelById(messageDto.getChannel());
            User from = usersDetailsService.getUserById(messageDto.getFrom().getId());
            User to = channel.anotherMember(from);

            PrivateMessage message = new PrivateMessage(
                    channel,
                    from,
                    to,
                    messageDto.getMessageType(),
                    messageDto.getContent()
            );

            return Optional.of(message);

        } catch (ChannelDoesNotExist e) {
            System.err.println("Channel not found: " + e.getMessage());
            return Optional.empty();
        } catch (UserDoesNotExist e) {
            System.err.println("User not found: " + e.getMessage());
            return Optional.empty();
        }
    }



    public PrivateMessageDto createMessage(String fromUserId ,
                                           String channelId,
                                           String Content ,
                                           MessageType messageType) {

        try{
            UUID channelUUID = UUID.fromString(channelId);
            User user = usersDetailsService.getUserById(fromUserId);

            PrivateChannel privateChannel = getChannelById(channelUUID);
            PrivateMessage privateMessage = privateChannel.addMessage(user,messageType, Content);

            privateChannelRepository.saveAndFlush(privateChannel);
            return new PrivateMessageDto(privateMessage);

        } catch (ChannelDoesNotExist e) {
            System.err.println("Channel not found: " + e.getMessage());
            return null;
        } catch (UserDoesNotExist e) {
            System.err.println("User not found: " + e.getMessage());
            return null;
        } catch (InvalidOperation e) {
            System.err.println("Invalid operation: " + e.getMessage());
            return null;
        }

    }



    public SliceList<PrivateMessageDto> getAllMessages(String userId,
                                                       PageRequest pageRequest)throws UserDoesNotExist{
        User user  = usersDetailsService.getUserById(userId);


        Pageable pageable = PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize(),
                Sort.by("createAt").descending() );

        Slice<PrivateMessage> slice =
                privateMessageRepository.getAllByUser(user.getUserId() , pageable);


        Function<? super PrivateMessage, ?> PrivateMessageDto;
        return new SliceList<>(
                slice.getNumber(),
                slice.getSize(),
                slice.getContent().stream()
                        .sorted(Comparator.comparing(PrivateMessage::getCreateAt))
                        .map(PrivateMessageDto::new)
                        .collect(Collectors.toList()),
                slice.hasNext());
    }


    public SliceList<PrivateMessageDto> getAllMessages(
            String userId, String channelId, PageRequest pageRequest)
            throws UserDoesNotExist, ChannelDoesNotExist, InvalidOperation {

        UUID channelUUID = UUID.fromString(channelId);

        Pageable pageable = PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize(),
                Sort.by("createAt").descending() );
        User user = usersDetailsService.getUserById(userId);
        PrivateChannel channel = getChannelById(channelUUID);
        if (channel.getUser1() != user ||  channel.getUser2() != user )
            throw new InvalidOperation("user is not in members of the channel !");

        Slice<PrivateMessage> slice = privateMessageRepository.getAllByChannel(channelUUID, pageable);
        return new SliceList<>(
                slice.getNumber(),
                slice.getSize(),
                slice.getContent().stream()
                        .sorted(Comparator.comparing(PrivateMessage::getCreateAt))
                        .map(PrivateMessageDto::new)
                        .toList(),
                slice.hasNext());
    }



}
