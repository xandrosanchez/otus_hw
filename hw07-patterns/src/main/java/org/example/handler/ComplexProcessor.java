package org.example.handler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.example.listener.Listener;
import org.example.model.Message;
import org.example.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplexProcessor implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(ComplexProcessor.class);

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final List<Processor> processors;
    private final Consumer<Exception> errorHandler;

    public ComplexProcessor(List<Processor> processors, Consumer<Exception> errorHandler) {
        this.processors = processors;
        this.errorHandler = errorHandler;
    }

    @Override
    public Message handle(Message msg) {
        Message newMsg = msg;
        for (Processor pros : processors) {
            try {
                newMsg = pros.process(newMsg);
            } catch (Exception ex) {
                errorHandler.accept(ex);
            }
        }
        notify(newMsg);
        return newMsg;
    }

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notify(Message msg) {
        listeners.forEach(listener -> {
            try {
                listener.onUpdated(msg);
            } catch (Exception ex) {
                logger.error("error", ex);
            }
        });
    }
}
