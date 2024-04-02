package com.example.streaming.controller;

import com.example.streaming.service.StreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
public class StreamController {
    private final StreamService streamService;

    @GetMapping(value = "/{title}", produces = "video/mp4")
    public Mono<Resource> getVideo(@PathVariable("title") String title) {
        return streamService.getVideo(title);
    }

    public Mono<Resource> getVideo(
            @PathVariable("title") String title,
            @RequestHeader("Range") String range) {
        System.out.println("range in bytes() : " + range);
        return streamService.getVideo(title);
    }
}
