package org.example;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class CtxExample {
    public void withCtxBySubscribe() {
        String ctxKey = "key";

        Flux.fromArray(generateIntArray(10))
                .flatMap(i -> Mono.deferContextual(ctx -> {
                            int value = ctx.<Integer>getOrEmpty(ctxKey).orElseThrow(() -> new IllegalArgumentException("Ctx key not found!"));
                            String result = i % value == 0
                                    ? String.format("Thread [%s] -> %d divisor of the number %d", Thread.currentThread().getName(), value, i)
                                    : String.format("Thread [%s] -> %d NOT divisor of the number %d", Thread.currentThread().getName(), value, i);

                            return Mono.just(result);
                        })
                ).subscribe(System.out::println,
                        null,
                        null,
                        Context.of(ctxKey, ThreadLocalRandom.current().nextInt(2, 10)));
    }

    public void withCtxByOperator1() {
        String ctxKey = "key";

        Flux.fromArray(generateIntArray(10))
                .flatMap(i -> Mono.deferContextual(ctx -> {
                            int value = ctx.<Integer>getOrEmpty(ctxKey).orElseThrow(() -> new IllegalArgumentException("Ctx key not found!"));
                            String result = i % value == 0
                                    ? String.format("Thread [%s] -> %d divisor of the number %d", Thread.currentThread().getName(), value, i)
                                    : String.format("Thread [%s] -> %d NOT divisor of the number %d", Thread.currentThread().getName(), value, i);

                            return Mono.just(result);
                        })
                ).contextWrite(ctx -> ctx.put(ctxKey, ThreadLocalRandom.current().nextInt(2, 10)))
                .subscribe(System.out::println);
    }

    public void withCtxByOperator2() {
        String ctxKey = "key";

        Flux.fromArray(generateIntArray(10))
                .flatMap(i -> Mono.deferContextual(ctx -> {
                            int value = ctx.<Integer>getOrEmpty(ctxKey).orElseThrow(() -> new IllegalArgumentException("Ctx key not found!"));
                            String result = i % value == 0
                                    ? String.format("Thread [%s] -> %d divisor of the number %d", Thread.currentThread().getName(), value, i)
                                    : String.format("Thread [%s] -> %d NOT divisor of the number %d", Thread.currentThread().getName(), value, i);

                            return Mono.just(result);
                        })
                ).contextWrite(Context.of(ctxKey, ThreadLocalRandom.current().nextInt(2, 10)))
                .subscribe(System.out::println);
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
