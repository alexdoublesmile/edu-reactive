package com.example.webflux.functional.handler;

import com.example.webflux.model.Message;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class GreetingHandler {

    public Mono<ServerResponse> hello(ServerRequest request) {
        final Long start = request.queryParam("start")
                .map(Long::valueOf)
                .orElse(0L);

        final Long count = request.queryParam("count")
                .map(Long::valueOf)
                .orElse(3L);

        final Flux<Message> body = Flux.just(
                        "Hello, Reactive",
                        "One more",
                        "Third message",
                        "Fourth message"
                )
                .skip(start)
                .take(count)
                .map(Message::new);

        return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body, Message.class);
    }

    public Mono<ServerResponse> render(ServerRequest request) {
        final String username = request.queryParam("user")
                .orElse("Anonymous");

        return ServerResponse
                .ok()
                .render("index", Map.of("user", username));
    }
}
