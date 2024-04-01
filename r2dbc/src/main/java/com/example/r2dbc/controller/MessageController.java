package com.example.r2dbc.controller;

import com.example.r2dbc.model.Message;
import com.example.r2dbc.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @GetMapping
    public Flux<Message> list(
            @RequestParam(defaultValue = "0") Long start,
            @RequestParam(defaultValue = "3") Long count
    ) {
        return messageService.findAll();
    }

    @PostMapping
    public Mono<Message> save(@RequestBody Message message) {
        return messageService.save(message);
    }
}
