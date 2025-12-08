package org.example;

import java.util.List;
import org.example.handler.ComplexProcessor;
import org.example.listener.ListenerPrinterConsole;
import org.example.listener.homework.HistoryListener;
import org.example.model.Message;
import org.example.model.ObjectForMessage;
import org.example.processor.EvenSecondExceptionProcessor;
import org.example.processor.ProcessorConcatFields;
import org.example.processor.ProcessorUpperField10;
import org.example.processor.SwapFieldsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeWork {
    private static final Logger logger = LoggerFactory.getLogger(HomeWork.class);

    public static void main(String[] args) {
        var processors = List.of(
                new ProcessorConcatFields(),
                new SwapFieldsProcessor(),
                new ProcessorUpperField10(),
                new EvenSecondExceptionProcessor());

        var complexProcessor = new ComplexProcessor(processors, ex -> logger.error("Error: {}", ex.getMessage()));

        var consoleListener = new ListenerPrinterConsole();
        var historyListener = new HistoryListener();

        complexProcessor.addListener(consoleListener);
        complexProcessor.addListener(historyListener);

        var objectForMessage = new ObjectForMessage();
        objectForMessage.setData(List.of("data1", "data2"));

        var message = Message.builder(1L)
                .field1("field1")
                .field2("field2")
                .field3("field3")
                .field6("field6")
                .field10("field10")
                .field11("value11")
                .field12("value12")
                .field13(objectForMessage)
                .build();

        try {
            var result = complexProcessor.handle(message);
            logger.info("Result: {}", result);

            historyListener.findMessageById(1L).ifPresent(historyMsg -> logger.info("From history: {}", historyMsg));

        } catch (Exception e) {
            logger.info("Processing failed: {}", e.getMessage());
        } finally {
            complexProcessor.removeListener(consoleListener);
            complexProcessor.removeListener(historyListener);
        }
    }
}
