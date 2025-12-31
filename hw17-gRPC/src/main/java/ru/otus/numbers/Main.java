package ru.otus.numbers;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "server".equals(args[0])) {
            // Запуск сервера
            // ./gradlew L34-multiprocess:homework:runServer -Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64
            // либо
            // ./gradlew :L34-multiprocess:homework:run --args="server"
            // -Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64
            ru.otus.numbers.server.NumbersServer.main(new String[] {});
        } else {
            // Запуск клиента
            // ./gradlew L34-multiprocess:homework:runClient -Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64
            // либо
            // ./gradlew :L34-multiprocess:homework:run --args="client"
            // -Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64
            ru.otus.numbers.client.NumbersClient.main(new String[] {});
        }
    }
}
