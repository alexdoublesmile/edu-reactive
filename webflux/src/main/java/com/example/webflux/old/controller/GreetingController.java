package com.example.webflux.old.controller;

import com.example.webflux.model.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/greeting")
public class GreetingController {

    @GetMapping
    public Flux<Message> list(
            @RequestParam(defaultValue = "0") Long start,
            @RequestParam(defaultValue = "3") Long count
    ) {
        return Flux.just(
                        "Hello, Reactive",
                        "One more",
                        "Third message",
                        "Fourth message"
                )
                .skip(start)
                .take(count)
                .map(Message::new);

    }
}
