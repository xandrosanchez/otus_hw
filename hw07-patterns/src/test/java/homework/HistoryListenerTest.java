package homework;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import org.example.listener.homework.HistoryListener;
import org.example.model.Message;
import org.example.model.ObjectForMessage;
import org.junit.jupiter.api.Test;

class HistoryListenerTest {

    @Test
    @SuppressWarnings({"java:S125", "java:S1135"})
    void listenerTest() {
        // given
        var historyListener = new HistoryListener();

        var id = 100L;
        var data = "33";
        var field13 = new ObjectForMessage();
        var field13Data = new ArrayList<String>();
        field13Data.add(data);
        field13.setData(field13Data);

        var message =
                new Message.Builder(id).field10("field10").field13(field13).build();

        // when
        historyListener.onUpdated(message);
        message.getField13().setData(new ArrayList<>()); // меняем исходное сообщение
        field13Data.clear(); // меняем исходный список

        // then
        var messageFromHistory = historyListener.findMessageById(id);
        assertThat(messageFromHistory).isPresent();
        assertThat(messageFromHistory.get().getField13().getData()).containsExactly(data);
    }
}
