package com.rohit.ChatApplication.exception;

public class KafkaDeliveryException extends RuntimeException {


    public KafkaDeliveryException(String message, Throwable cause) {
      super(message, cause);
    }

    public KafkaDeliveryException(String message) {
      super(message);
    }

    public KafkaDeliveryException(Throwable cause) {
      super(cause);
    }

    public KafkaDeliveryException() {
    }

}
