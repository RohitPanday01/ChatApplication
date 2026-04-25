package com.rohit.ChatApplication.service.ReadReciept;

import com.rohit.ChatApplication.data.ReadReceipt;
import com.rohit.ChatApplication.data.ReceiptType;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
public class ReadReceiptEmitService {




    public ReadReceipt emitDeliveredReceipt(PrivateMessageDto message) {

        String eventId = String.format(
                "%s:%s:%s",
                message.getId(),
                message.getTo().getId(),
                ReceiptType.DELIVERED
        );

         ReadReceipt event = new ReadReceipt(
                eventId ,
                message.getId().toString()
                , message.getChannel().toString(),
                message.getFrom().getUsername(),
                ReceiptType.DELIVERED ,
                message.getTo().getUsername(),
                Instant.now());
        return  event;
    }

    public ReadReceipt emitSentReceipt(PrivateMessageDto messageDto){
        String eventId = String.format(
                "%s:%s:%s",
                messageDto.getId(),
                messageDto.getTo().getId(),
                ReceiptType.SENT
        );

        ReadReceipt event = new ReadReceipt(
                eventId ,
                messageDto.getId().toString()
                , messageDto.getChannel().toString(),
                messageDto.getFrom().getUsername(),
                ReceiptType.SENT ,
                messageDto.getTo().getUsername(),
                Instant.now());
        return  event;
    }

    public ReadReceipt emitSeenReceipt(PrivateMessageDto messageDto){
        String eventId = String.format(
                "%s:%s:%s",
                messageDto.getId(),
                messageDto.getTo().getId(),
                ReceiptType.SEEN
        );

        ReadReceipt event = new ReadReceipt(
                eventId ,
                messageDto.getId().toString()
                , messageDto.getChannel().toString(),
                messageDto.getFrom().getUsername(),
                ReceiptType.SEEN ,
                messageDto.getTo().getUsername(),
                Instant.now());
        return  event;
    }


}
