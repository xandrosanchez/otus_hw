package ru.otus.numbers.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.numbers.generated.NumbersServiceGrpc;
import ru.otus.numbers.generated.SequenceRequest;
import ru.otus.numbers.generated.SequenceResponse;

public class NumbersClient {
    private static final Logger log = LoggerFactory.getLogger(NumbersClient.class);

    private final ManagedChannel channel;
    private final NumbersServiceGrpc.NumbersServiceStub asyncStub;
    private final AtomicInteger lastServerValue = new AtomicInteger(0);
    private final CountDownLatch finishLatch = new CountDownLatch(1);

    public NumbersClient(String host, int port) {
        this.channel =
                ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.asyncStub = NumbersServiceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void getSequence(int firstValue, int lastValue) {
        SequenceRequest request = SequenceRequest.newBuilder()
                .setFirstValue(firstValue)
                .setLastValue(lastValue)
                .build();

        log.info("Requesting sequence from {} to {}", firstValue, lastValue);

        asyncStub.getSequence(request, new StreamObserver<SequenceResponse>() {
            @Override
            public void onNext(SequenceResponse value) {
                int newValue = value.getValue();
                lastServerValue.set(newValue); // Просто устанавливаем значение
                log.info("new value:{}", newValue);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in stream", t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("request completed");
                finishLatch.countDown();
            }
        });
    }

    public void startCalculation() throws InterruptedException {
        int currentValue = 0;

        for (int i = 0; i <= 50; i++) {
            TimeUnit.SECONDS.sleep(1); // Задержка 1 секунда

            // Используем getAndSet(0) - атомарно получаем значение и сбрасываем его
            int serverValue = lastServerValue.getAndSet(0);

            if (serverValue > 0) {
                // Используем значение от сервера только один раз
                currentValue = currentValue + serverValue + 1;
            } else {
                currentValue = currentValue + 1;
            }

            log.info("currentValue:{}", currentValue);
        }

        // Ждем завершения stream от сервера
        finishLatch.await(5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws InterruptedException {
        log.info("numbers Client is starting...");

        NumbersClient client = new NumbersClient("localhost", 8980);
        try {
            // Запрашиваем последовательность от 0 до 30
            client.getSequence(0, 30);
            // Запускаем основной цикл расчета
            client.startCalculation();
        } finally {
            client.shutdown();
        }
    }
}
