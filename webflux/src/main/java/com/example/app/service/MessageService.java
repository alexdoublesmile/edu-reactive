package com.example.app.service;

import com.example.app.model.entity.Message;
import com.example.app.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;

    public Flux<Message> findAll() {
        return messageRepository.findAll();
    }

    public Mono<Message> save(Message message) {
        return messageRepository.save(message);
    }
}
