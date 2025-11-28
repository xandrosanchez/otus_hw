package org.example.listener.homework;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.listener.Listener;
import org.example.model.Message;
import org.example.model.ObjectForMessage;

public class HistoryListener implements Listener, HistoryReader {
    private final Map<Long, Message> messageHistory = new HashMap<>();

    @Override
    public void onUpdated(Message msg) {
        messageHistory.put(msg.getId(), deepCopy(msg));
    }

    @Override
    public Optional<Message> findMessageById(long id) {
        return Optional.ofNullable(messageHistory.get(id)).map(this::deepCopy);
    }

    private Message deepCopy(Message original) {
        ObjectForMessage field13Copy = null;
        if (original.getField13() != null) {
            field13Copy = new ObjectForMessage();
            if (original.getField13().getData() != null) {
                field13Copy.setData(List.copyOf(original.getField13().getData()));
            }
        }

        return original.toBuilder().field13(field13Copy).build();
    }
}
