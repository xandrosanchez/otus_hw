package org.example.handler;

import org.example.listener.Listener;
import org.example.model.Message;

public interface Handler {
    Message handle(Message msg);

    void addListener(Listener listener);

    void removeListener(Listener listener);
}
