package org.example;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Objects;

@Slf4j
public class FluxTest {

    @Test
    public void checkBaseSubscriber() {
        final Flux<Integer> publisher = Flux.range(1, 10)
                .log();

        publisher.subscribe(new BaseSubscriber<>() {
            private int count;
            private final int requestSize = 2;

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(requestSize);
            }

            @Override
            protected void hookOnNext(Integer value) {
                count++;
                if (count >= requestSize) {
                    count = 0;
                    request(requestSize);
                }
            }
        });

        log.info(" ------------------ ");

        StepVerifier.create(publisher)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
    }

    @Test
    public void checkPrettyBackPressure() {
        final Flux<Integer> publisher = Flux.range(1, 10)
                .log()
                .limitRate(2);

        publisher.subscribe();

        log.info(" ------------------ ");

        StepVerifier.create(publisher)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
    }

    @Test
    public void checkHotPublisher() {
        final ConnectableFlux<Integer> publisher = Flux.range(1, 10)
                .log()
                .delayElements(Duration.ofMillis(100))
                .publish();

//        publisher.connect();
//        log.info("Sleep 300ms");
//        Thread.sleep(300);
//        publisher.subscribe();
//        log.info("Sleep 200ms");
//        publisher.subscribe();

        StepVerifier.create(publisher)
                .then(publisher::connect)
                .thenConsumeWhile(i -> i <= 5)
                .expectNext(6, 7, 8, 9, 10)
                .verifyComplete();
    }

    @Test
    public void checkSubscribeOnPublishOn() {
        final Flux<Integer> publisher = Flux.range(1, 4)
                .map(i -> {
                    log.info("First map operation - num {}. Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .publishOn(Schedulers.boundedElastic())
                .subscribeOn(Schedulers.single())
                .map(i -> {
                    log.info("Second map operation - num {}. Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(publisher)
                .expectSubscription()
                .expectNext(1, 2, 3, 4)
                .verifyComplete();
    }

    @Test
    public void checkZip() {
        final Flux<String> animeNameStream = Flux.just("Grand Blue", "Baki");
        final Flux<String> animeStudioStream = Flux.just("Zero-G", "TS");
        final Flux<Integer> animeEpisodesStream = Flux.just(12, 24);

        final Flux<Anime> animeFlux = Flux.zip(animeNameStream, animeStudioStream, animeEpisodesStream)
                .flatMap(tuple -> Flux.just(
                        new Anime(tuple.getT1(), tuple.getT2(), tuple.getT3())));

        StepVerifier.create(animeFlux)
                .expectSubscription()
                .expectNext(
                        new Anime("Grand Blue", "Zero-G", 12),
                        new Anime("Baki", "TS", 24)
                )
                .verifyComplete();
    }

    private class Anime {
        private final String name;
        private final String studio;
        private final Integer episodeCount;

        public Anime(String name, String studio, Integer episodeCount) {
            this.name = name;
            this.studio = studio;
            this.episodeCount = episodeCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Anime anime = (Anime) o;
            return Objects.equals(name, anime.name) && Objects.equals(studio, anime.studio) && Objects.equals(episodeCount, anime.episodeCount);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, studio, episodeCount);
        }
    }
}
