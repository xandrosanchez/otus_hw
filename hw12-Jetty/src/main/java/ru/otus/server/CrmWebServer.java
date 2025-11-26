package ru.otus.server;

@SuppressWarnings({"java:S112", "java:S1181"})
public interface CrmWebServer {
    void start() throws Exception;

    void join() throws Exception;

    void stop() throws Exception;
}
