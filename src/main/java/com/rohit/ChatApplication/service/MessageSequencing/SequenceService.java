package com.rohit.ChatApplication.service.MessageSequencing;



import org.springframework.stereotype.Service;

import java.util.UUID;


@Service

public interface SequenceService {

    public long nextSequence(UUID channelId);

}
