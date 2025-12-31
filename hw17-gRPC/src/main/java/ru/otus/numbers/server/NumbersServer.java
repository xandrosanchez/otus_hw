package ru.otus.numbers.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.numbers.generated.NumbersServiceGrpc;
import ru.otus.numbers.generated.SequenceRequest;
import ru.otus.numbers.generated.SequenceResponse;

public class NumbersServer {
    private static final Logger log = LoggerFactory.getLogger(NumbersServer.class);
    private final int port;
    private final Server server;

    public NumbersServer(int port) {
        this.port = port;
        this.server =
                ServerBuilder.forPort(port).addService(new NumbersServiceImpl()).build();
    }

    public void start() throws IOException {
        server.start();
        log.info("Server started, listening on {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                NumbersServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    static class NumbersServiceImpl extends NumbersServiceGrpc.NumbersServiceImplBase {
        private static final Logger log = LoggerFactory.getLogger(NumbersServiceImpl.class);

        @Override
        public void getSequence(SequenceRequest request, StreamObserver<SequenceResponse> responseObserver) {
            int firstValue = request.getFirstValue();
            int lastValue = request.getLastValue();

            log.info("Generating sequence from {} to {}", firstValue + 1, lastValue);

            try {
                for (int i = firstValue + 1; i <= lastValue; i++) {
                    TimeUnit.SECONDS.sleep(2); // Задержка 2 секунды

                    SequenceResponse response =
                            SequenceResponse.newBuilder().setValue(i).build();

                    log.info("Sending value: {}", i);
                    responseObserver.onNext(response);
                }
                responseObserver.onCompleted();
                log.info("Sequence generation completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                responseObserver.onError(e);
            } catch (Exception e) {
                responseObserver.onError(e);
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        NumbersServer server = new NumbersServer(8980);
        server.start();
        server.blockUntilShutdown(); // Эта строка блокирует выполнение до завершения сервера
    }
}
