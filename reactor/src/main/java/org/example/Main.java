package org.example;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Main {
    public static void main(String[] args) {
        Mono.create();
        Mono.just();
        Mono.empty();
        Mono.justOrEmpty();
        Mono.from();
        Mono.fromCallable();
        Mono.fromDirect();
        Mono.fromFuture();
        Mono.fromCompletionStage();
        Mono.fromRunnable();
        Mono.fromSupplier();
        Mono.defer();
        Mono.deferContextual();
        Mono.error();
        Mono.never();
        Mono.delay();
        Mono.using();
        Mono.when();
        Mono.usingWhen();
        Mono.whenDelayError();
        Mono.zip();
        Mono.zipDelayError();
        Mono.sequenceEqual();
        Mono.firstWithSignal();
        Mono.firstWithValue();
        Mono.ignoreElements();
        Mono.first();

        Flux.combineLatest()
        Flux.zip()
        Flux.concat()
        Flux.()
    }
}