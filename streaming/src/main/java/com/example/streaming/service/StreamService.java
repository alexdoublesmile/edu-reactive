package com.example.streaming.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class StreamService {
    private static final String FORMAT = "classpath:video/%s.mp4";
    private final ResourceLoader resourceLoader;

    public Mono<Resource> getVideo(String title){
        final String path = String.format(FORMAT, title);
        return Mono.fromSupplier(
             () -> resourceLoader.getResource(path));
    }
}
