package org.example;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class SimpleExample {
    public static void main(String[] args) {
        new SimpleExample().syncStream();
    }

//    1. LambdaSubscriber calls ArraySubscription.request(Long.MAX_VALUE)
//    2. ArraySubscription calls FilterSubscriber.onNext()
//    3. FilterSubscriber runs predicate & calls MapSubscriber.onNext()
//    4. MapSubscriber runs mapping & calls LambdaSubscriber.onNext()
//    5. LambdaSubscriber runs subscribe() function argument.
//    6. ArraySubscription calls FilterSubscriber.onComplete() through same chain (no actions here)
    public void simpleStream() {
        Integer[] array = generateIntArray(100);

        Flux.fromArray(array)
                .filter(i -> i % 2 != 0)
                .map(i -> "Number " + i + " is prime: " + checker.test(i))
                .subscribe(System.out::println);
    }

    public void syncStream() {
        Flux.<Integer>create(s -> {
                    s.next(50);
                    s.next(50);
                    s.next(50);
                    s.next(50);
                    s.next(100);
                    s.next(100);
                    s.next(100);
                    s.next(100);
                    s.next(150);
                    s.next(150);
                    s.next(150);
                    s.next(150);
                    s.complete();
                })
                .map(this::generateIntArray)
                .map(array -> Arrays.stream(array)
                        .filter(i -> (i % 2 != 0))
                        .collect(toList()))
                .flatMap(list -> Flux.fromIterable(list)
                        .map(i -> "Number " + i + " is prime: " + checker.test(i)))
                .subscribe(result -> System.out.println("IN thread [" + Thread.currentThread().getName() + "] -> " + result));
    }

    public void asyncBySubscribeOn() throws InterruptedException {
        var cdl = new CountDownLatch(1);

        Flux.<Integer>create(s -> {
                    s.next(50);
                    s.next(50);
                    s.next(50);
                    s.next(50);
                    s.next(100);
                    s.next(100);
                    s.next(100);
                    s.next(100);
                    s.next(150);
                    s.next(150);
                    s.next(150);
                    s.next(150);
                    s.complete();
                }).map(this::generateIntArray)
                .map(array -> Arrays.stream(array)
                        .filter(i -> (i % 2 != 0))
                        .collect(Collectors.toList()))
                .flatMap(list -> Flux.fromIterable(list)
                        .map(i -> "Number " + i + " is prime: " + checker.test(i)))
                .subscribeOn(Schedulers.boundedElastic())
                .doFinally(ignore -> cdl.countDown())
                .subscribe(result -> System.out.println("IN thread [" + Thread.currentThread().getName() + "] -> " + result));

        cdl.await();
    }

    public void asyncByPublishOn() throws InterruptedException {
        var cdl = new CountDownLatch(1);

        Flux.<Integer>create(s -> {
                    s.next(50);
                    s.next(50);
                    s.next(50);
                    s.next(50);
                    s.next(100);
                    s.next(100);
                    s.next(100);
                    s.next(100);
                    s.next(150);
                    s.next(150);
                    s.next(150);
                    s.next(150);
                    s.complete();
                }).map(this::generateIntArray)
                .map(array -> {
                    System.out.println("IN thread [" + Thread.currentThread().getName() + "] -> filtering array with size: " + array.length);
                    return Arrays.stream(array)
                            .filter(i -> (i % 2 != 0))
                            .collect(Collectors.toList());
                })
                .publishOn(Schedulers.boundedElastic())
                .flatMap(list -> Flux.fromIterable(list)
                        .map(i -> "Number " + i + " is prime: " + checker.test(i)))
                .doFinally(ignore -> cdl.countDown())
                .subscribe(result -> System.out.println("IN thread [" + Thread.currentThread().getName() + "] -> " + result));

        cdl.await();
    }

    public void asyncEachSource() throws InterruptedException {
        var cdl = new CountDownLatch(1);

        Flux.<Integer>create(s -> {
                    s.next(50);
                    s.next(50);
                    s.next(50);
                    s.next(50);
                    s.next(100);
                    s.next(100);
                    s.next(100);
                    s.next(100);
                    s.next(150);
                    s.next(150);
                    s.next(150);
                    s.next(150);
                    s.complete();
                }).map(this::generateIntArray)
                .map(array -> {
                    System.out.println("IN thread [" + Thread.currentThread().getName() + "] -> filtering array with size: " + array.length);
                    return Arrays.stream(array)
                            .filter(i -> (i % 2 != 0))
                            .collect(Collectors.toList());
                })
                .flatMap(list -> Flux.fromIterable(list)
                        .publishOn(Schedulers.boundedElastic())
                        .map(i -> "Number " + i + " is prime: " + checker.test(i)))
                .doFinally(ignore -> cdl.countDown())
                .subscribe(result -> System.out.println("IN thread [" + Thread.currentThread().getName() + "] -> " + result));

        cdl.await();
    }

    public void asyncEachTask() throws InterruptedException {
        var cdl = new CountDownLatch(1);

        Flux.<Integer>create(s -> {
                    s.next(50);
                    s.next(50);
                    s.next(50);
                    s.next(50);
                    s.next(100);
                    s.next(100);
                    s.next(100);
                    s.next(100);
                    s.next(150);
                    s.next(150);
                    s.next(150);
                    s.next(150);
                    s.complete();
                }).map(this::generateIntArray)
                .map(array -> {
                    System.out.println("IN thread [" + Thread.currentThread().getName() + "] -> filtering array with size: " + array.length);
                    return Arrays.stream(array)
                            .filter(i -> (i % 2 != 0))
                            .collect(Collectors.toList());
                })
                .flatMap(list -> Flux.fromIterable(list)
                        .flatMap(i -> Mono.defer(() -> Mono.just("Number " + i + " is prime: " + checker.test(i)))
                                .subscribeOn(Schedulers.boundedElastic()))
                )
                .doFinally(ignore -> cdl.countDown())
                .subscribe(result -> System.out.println("IN thread [" + Thread.currentThread().getName() + "] -> " + result));

        cdl.await();
    }

    private Integer[] generateIntArray(int size) {
        Integer[] array = new Integer[size];
        for (int i = 0; i < size; i++) {
            array[i] = ThreadLocalRandom.current().nextInt(90_000_000, 100_000_000);
        }
        return array;
    }

    private final Predicate<Integer> checker = num -> {
        for (int i = 2; i < num; i++) {
            if (num % i == 0) {
                return false;
            }
        }
        return true;
    };
}


