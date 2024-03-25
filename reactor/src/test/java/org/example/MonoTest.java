package org.example;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Slf4j
public class MonoTest {

    @Test
    public void checkSubscriber() {
        String name = "William Shakespeare";
        final Mono<String> publisher = Mono.just(name)
                .log()
                .map(String::toUpperCase);

        publisher.subscribe(
                log::info,
                Throwable::printStackTrace,
                () -> log.info("Finished!"),
                subscription -> subscription.request(5)
        );

        log.info(" ------------------ ");

        StepVerifier.create(publisher)
                .expectNext(name.toUpperCase())
                .verifyComplete();
    }

    @Test
    public void checkDoOn() {
        String name = "William Shakespeare";
        final Mono<String> publisher = Mono.just(name)
                .log()
                .map(String::toUpperCase)
                .doOnSubscribe(subscription -> log.info("Subscribed"))
                .doOnRequest(num -> log.info("Requested {}", num))
                .doOnEach(str -> log.info("Some signal received"))
                .doOnSuccess(str -> log.info("Not error received"))
                .doOnNext(str -> log.info("Received: {}", str))
                .doOnError(Throwable::printStackTrace)
//                .doOnDiscard()
                .doOnCancel(() -> log.info("Cancelled"))
                .doOnTerminate(() -> log.info("Terminated"));

        publisher.subscribe(
                log::info,
                Throwable::printStackTrace,
                () -> log.info("Finished!"),
                subscription -> subscription.request(5)
        );

        log.info(" ------------------ ");

        StepVerifier.create(publisher)
                .expectNext(name.toUpperCase())
                .verifyComplete();
    }

    @Test
    public void checkOnError() {
        String name = "William Shakespeare";
        String empty = "empty value";
        final Mono<Object> error = Mono.error(new IllegalArgumentException("Bad mono"))
                .onErrorReturn(empty)

                // all will be ignored
                .onErrorResume(ex -> {
                    log.info("After error resuming...");
                    return Mono.just(name);
                })
//                .onErrorContinue()
//                .onErrorMap()
                .onErrorStop()
                .onErrorComplete()
                .doOnError(ex -> log.error(ex.getMessage()))

                .log();

        StepVerifier.create(error)
                .expectNext(empty)
                .verifyComplete();
    }
}
