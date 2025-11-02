package org.example.listener.homework;

import java.util.Optional;
import org.example.model.Message;

public interface HistoryReader {

    Optional<Message> findMessageById(long id);
}
