package org.example.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.example.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvenSecondExceptionProcessorTest {

    private EvenSecondExceptionProcessor processor;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        testMessage = Message.builder(1L).build();
    }

    @Test
    @DisplayName("Должен выбросить исключение, когда секунда четная")
    void shouldThrowExceptionWhenSecondIsEven() {
        LocalDateTime evenSecondTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0); // 0 секунда - четная
        processor = new EvenSecondExceptionProcessor(() -> evenSecondTime);

        EvenSecondExceptionProcessor.EvenSecondException exception = assertThrows(
                EvenSecondExceptionProcessor.EvenSecondException.class, () -> processor.process(testMessage));

        assertEquals("Even second exception", exception.getMessage());
    }

    @Test
    @DisplayName("Должен успешно обработать сообщение, когда секунда нечетная")
    void shouldProcessMessageWhenSecondIsOdd() {
        LocalDateTime oddSecondTime = LocalDateTime.of(2024, 1, 1, 12, 0, 1); // 1 секунда - нечетная
        processor = new EvenSecondExceptionProcessor(() -> oddSecondTime);

        Message result = processor.process(testMessage);

        assertSame(testMessage, result);
    }

    @Test
    @DisplayName("Должен работать с разными четными секундами")
    void shouldThrowExceptionForVariousEvenSeconds() {
        int[] evenSeconds = {0, 2, 10, 30, 58};

        for (int second : evenSeconds) {
            LocalDateTime evenSecondTime = LocalDateTime.of(2024, 1, 1, 12, 0, second);
            processor = new EvenSecondExceptionProcessor(() -> evenSecondTime);

            assertThrows(
                    EvenSecondExceptionProcessor.EvenSecondException.class,
                    () -> processor.process(testMessage),
                    "Должен бросать исключение для секунды: " + second);
        }
    }

    @Test
    @DisplayName("Должен работать с разными нечетными секундами")
    void shouldProcessMessageForVariousOddSeconds() {
        int[] oddSeconds = {1, 3, 15, 31, 59};

        for (int second : oddSeconds) {
            LocalDateTime oddSecondTime = LocalDateTime.of(2024, 1, 1, 12, 0, second);
            processor = new EvenSecondExceptionProcessor(() -> oddSecondTime);

            assertDoesNotThrow(
                    () -> processor.process(testMessage), "Не должен бросать исключение для секунды: " + second);
        }
    }

    @Test
    @DisplayName("Исключение должно содержать правильное сообщение")
    void exceptionShouldHaveCorrectMessage() {
        LocalDateTime evenSecondTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        processor = new EvenSecondExceptionProcessor(() -> evenSecondTime);

        EvenSecondExceptionProcessor.EvenSecondException exception = assertThrows(
                EvenSecondExceptionProcessor.EvenSecondException.class, () -> processor.process(testMessage));

        assertEquals("Even second exception", exception.getMessage());
        assertNull(exception.getCause());
    }
}
