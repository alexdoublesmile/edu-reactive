package org.example.util;

import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MICROSECONDS;

public final class Benchmark {
    private static final TimeUnit DEFAULT_TIME_UNIT = MICROSECONDS;

    public static long measure(Runnable runnable) {
        long startTime = System.nanoTime();
        long elapsedTime;
        try {
            runnable.run();
        } finally {
            long endTime = System.nanoTime();
            elapsedTime = (endTime - startTime) / getTimeUnitDivider(DEFAULT_TIME_UNIT);
        }
        return elapsedTime;
    }

    public static void measure(Operation operation) {
        long startTime = System.nanoTime();
        try {
            operation.run();
        } finally {
            long endTime = System.nanoTime();
            long elapsedTime = (endTime - startTime) / getTimeUnitDivider(operation.unit);

            // console, log or store the time for further use
            System.err.printf("%s: %d %s\n", operation.name, elapsedTime, operation.unit.name().toLowerCase());
        }
    }

    private static long getTimeUnitDivider(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case NANOSECONDS -> 1L;
            case MICROSECONDS -> 1000L;
            case MILLISECONDS -> 1000_000L;
            case SECONDS -> 1000_000_000L;
            case MINUTES -> 1000_000_000L * 60;
            case HOURS -> 1000_000_000L * 60 * 60;
            case DAYS -> 1000_000_000L * 60 * 60 * 24;
        };
    }

    public static Operation operation(String name, Runnable operation, TimeUnit unit) {
        return Operation.builder()
                .name(name)
                .action(operation)
                .unit(unit)
                .build();
    }

    public static Operation operation(String name, Runnable operation) {
        return Operation.builder()
                .name(name)
                .action(operation)
                .unit(DEFAULT_TIME_UNIT)
                .build();
    }

    @Builder
    private static class Operation {
        @Getter
        private String name;
        @Getter
        private TimeUnit unit;
        private Runnable action;

        public void run() {
            action.run();
        }
    }
}
