package ru.petrelevich.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.petrelevich.domain.Message;

@Controller
public class MessageController {
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private static final String TOPIC_TEMPLATE = "/topic/response.";
    private static final String SPECIAL_ROOM = "1408";

    private final WebClient datastoreClient;
    private final SimpMessagingTemplate template;

    public MessageController(WebClient datastoreClient, SimpMessagingTemplate template) {
        this.datastoreClient = datastoreClient;
        this.template = template;
    }

    @MessageMapping("/message.{roomId}")
    public void getMessage(@DestinationVariable("roomId") String roomId, Message message) {
        logger.info("get message:{}, roomId:{}", message, roomId);

        // Запрещаем отправку сообщений в комнату 1408
        if (SPECIAL_ROOM.equals(roomId)) {
            logger.warn("Attempt to send message to room 1408 blocked");
            return;
        }

        // Сначала отправляем сообщение в комнату отправителя
        String senderRoomTopic = String.format("%s%s", TOPIC_TEMPLATE, roomId);
        template.convertAndSend(senderRoomTopic, new Message(HtmlUtils.htmlEscape(message.messageStr())));

        // Отправляем сообщение в комнату 1408
        String room1408Topic = String.format("%s%s", TOPIC_TEMPLATE, SPECIAL_ROOM);
        template.convertAndSend(room1408Topic, new Message(HtmlUtils.htmlEscape(message.messageStr())));

        logger.debug("Message sent to room {} and room 1408", roomId);

        // Асинхронно сохраняем сообщение в базу данных (не ждем результата)
        saveMessage(roomId, message)
                .subscribe(
                        msgId -> logger.info("message saved with id:{}", msgId),
                        error -> logger.error("Failed to save message", error));
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        var genericMessage = (GenericMessage<byte[]>) event.getMessage();
        var simpDestination = (String) genericMessage.getHeaders().get("simpDestination");
        if (simpDestination == null) {
            logger.error("Can not get simpDestination header, headers:{}", genericMessage.getHeaders());
            throw new ChatException("Can not get simpDestination header");
        }
        if (!simpDestination.startsWith(TOPIC_TEMPLATE)) {
            return;
        }

        var roomId = simpDestination.substring(simpDestination.lastIndexOf(TOPIC_TEMPLATE) + TOPIC_TEMPLATE.length());
        logger.info("Parsed roomId: {}", roomId);

        var principal = event.getUser();
        if (principal == null) {
            return;
        }

        logger.info("subscription for:{}, roomId:{}, user:{}", simpDestination, roomId, principal.getName());

        if (SPECIAL_ROOM.equals(roomId)) {
            getAllMessages()
                    .doOnError(ex -> logger.error("getting all messages for roomId:{} failed", roomId, ex))
                    .subscribe(message -> {
                        logger.debug("Sending message to room 1408: {}", message.messageStr());
                        template.convertAndSendToUser(principal.getName(), simpDestination, message);
                    });
        } else {
            getMessagesByRoomId(roomId)
                    .doOnError(ex -> logger.error("getting messages for roomId:{} failed", roomId, ex))
                    .subscribe(message -> {
                        logger.debug("Sending message to room {}: {}", roomId, message.messageStr());
                        template.convertAndSendToUser(principal.getName(), simpDestination, message);
                    });
        }
    }

    private Mono<Long> saveMessage(String roomId, Message message) {
        logger.info("Saving message to room: {}", roomId);
        return datastoreClient
                .post()
                .uri(String.format("/msg/%s", roomId))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(message)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.FORBIDDEN)) {
                        logger.warn("Cannot save message for room {}", roomId);
                        return Mono.just(-1L);
                    }
                    return response.bodyToMono(Long.class);
                });
    }

    private Flux<Message> getMessagesByRoomId(String roomId) {
        logger.info("Getting messages for room: {}", roomId);
        return datastoreClient
                .get()
                .uri(String.format("/msg/%s", roomId))
                .accept(MediaType.APPLICATION_NDJSON)
                .exchangeToFlux(response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                        return response.bodyToFlux(Message.class);
                    } else {
                        logger.error("Failed to get messages for room {}, status: {}", roomId, response.statusCode());
                        return response.createException().flatMapMany(Mono::error);
                    }
                });
    }

    // Новый метод для получения ВСЕХ сообщений (для комнаты 1408)
    private Flux<Message> getAllMessages() {
        logger.info("Getting all messages for room 1408");
        return datastoreClient
                .get()
                .uri("/msg/all")
                .accept(MediaType.APPLICATION_NDJSON)
                .exchangeToFlux(response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                        return response.bodyToFlux(Message.class);
                    } else {
                        logger.error("Failed to get all messages, status: {}", response.statusCode());
                        return response.createException().flatMapMany(Mono::error);
                    }
                });
    }
}
