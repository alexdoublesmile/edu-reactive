package org.example;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class HotExample {

    public void hotStream() throws InterruptedException {
        var cdl = new CountDownLatch(1);

        Flux<Integer> stream = Flux.range(0, 10)
                .delayElements(Duration.ofMillis(500))
                .doFinally(ignore -> cdl.countDown())
                .subscribeOn(Schedulers.single())
                .share();

        stream.subscribe();

        Thread.sleep(2000);

        stream.subscribe(o -> System.out.println("[" + Thread.currentThread().getName() + "] Subscriber 1 -> " + o));

        Thread.sleep(2000);

        stream.subscribe(o -> System.out.println("[" + Thread.currentThread().getName() + "] Subscriber 2 -> " + o));

        cdl.await();
    }

    public void hotInfiniteStream() throws InterruptedException {
        var cdl = new CountDownLatch(1);

        Flux<Object> stream = Flux.create(fluxSink -> {
                    while (true) {
                        fluxSink.next(System.currentTimeMillis());
                    }
                })
                .sample(Duration.ofMillis(500))
                .doFinally(ignore -> cdl.countDown())
                .subscribeOn(Schedulers.single())
                .share();

        stream.subscribe(o -> System.out.println("[" + Thread.currentThread().getName() + "] Subscriber 1 -> " + o));

        Thread.sleep(4000);

        stream.subscribe(o -> System.out.println("[" + Thread.currentThread().getName() + "] Subscriber 2 -> " + o));

        cdl.await();
    }
}
