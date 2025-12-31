package ru.otus.services.processors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.api.SensorDataProcessor;
import ru.otus.api.model.SensorData;
import ru.otus.lib.SensorDataBufferedWriter;

@SuppressWarnings({"java:S1068", "java:S125"})
public class SensorDataProcessorBuffered implements SensorDataProcessor {
    private static final Logger log = LoggerFactory.getLogger(SensorDataProcessorBuffered.class);

    private final int bufferSize;
    private final SensorDataBufferedWriter writer;
    private final List<SensorData> dataBuffer;
    private final ReentrantLock lock;

    public SensorDataProcessorBuffered(int bufferSize, SensorDataBufferedWriter writer) {
        this.bufferSize = bufferSize;
        this.writer = writer;
        this.dataBuffer = new ArrayList<>(bufferSize);
        this.lock = new ReentrantLock();
    }

    @Override
    public void process(SensorData data) {
        lock.lock();
        try {
            int insertIndex = Collections.binarySearch(
                    dataBuffer, data, Comparator.comparing(SensorData::getMeasurementTime));

            if (insertIndex < 0) {
                insertIndex = -insertIndex - 1;
            }

            dataBuffer.add(insertIndex, data);

            if (dataBuffer.size() >= bufferSize) {
                flush();
            }
        } finally {
            lock.unlock();
        }
    }

    public void flush() {
        List<SensorData> dataToWrite = new ArrayList<>();

        lock.lock();
        try {
            if (!dataBuffer.isEmpty()) {
                // Копируем все данные из буфера
                dataToWrite.addAll(dataBuffer);
                // Очищаем буфер
                dataBuffer.clear();
            }
        } finally {
            lock.unlock();
        }

        if (!dataToWrite.isEmpty()) {
            try {
                writer.writeBufferedData(dataToWrite);
            } catch (Exception e) {
                log.error("Ошибка в процессе записи буфера", e);
            }
        }
    }

    @Override
    public void onProcessingEnd() {
        flush();
    }
}
