package org.example.processor;

import java.time.LocalDateTime;
import org.example.model.Message;

public class EvenSecondExceptionProcessor implements Processor {
    private final TimeProvider timeProvider;

    public EvenSecondExceptionProcessor() {
        this(LocalDateTime::now);
    }

    public EvenSecondExceptionProcessor(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    @Override
    public Message process(Message message) {
        if (timeProvider.getCurrentTime().getSecond() % 2 == 0) {
            throw new EvenSecondException("Even second exception");
        }
        return message;
    }

    @FunctionalInterface
    public interface TimeProvider {
        LocalDateTime getCurrentTime();
    }

    public static class EvenSecondException extends RuntimeException {
        public EvenSecondException(String message) {
            super(message);
        }
    }
}
