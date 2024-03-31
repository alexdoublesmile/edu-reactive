package org.example.async;

import lombok.SneakyThrows;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RemoteUtil {
    @SneakyThrows
    public static List<String> getFavoriteIds(Integer userId) {
        Thread.sleep(2000);
        return switch (userId) {
            case 12 -> List.of("1", "2", "3", "12", "99", "13", "7");
            case 5 -> List.of("01", "02", "03", "012", "099", "013", "07");
            case 0 -> Collections.emptyList();
            default -> throw new RuntimeException("some gone wrong with get favorite ids");
        };
    }

    @SneakyThrows
    public static Favorite getFavorite(String favId) {
        Thread.sleep(3000);
        return Favorite.builder()
                .name("name for favId " + favId)
                .description("desc for favId " + favId)
                .build();
    }

    @SneakyThrows
    public static List<Favorite> getDefaultFavoriteList() {
        Thread.sleep(5000);
        return List.of(
                Favorite.builder()
                        .name("name for default favId 1")
                        .description("desc for default favId 1")
                        .build(),
                Favorite.builder()
                        .name("name for default favId 2")
                        .description("desc for default favId 2")
                        .build()
        );
    }

    @SneakyThrows
    public static Flux<String> getFavoriteIdsReactive(Integer userId) {
//        Thread.sleep(2000);
        return switch (userId) {
            case 12 -> Flux.fromIterable(List.of("1", "2", "3", "12", "99", "13", "7"));
            case 5 -> Flux.fromIterable(List.of("01", "02", "03", "012", "099", "013", "07"));
            case 0 -> Flux.empty();
            default -> throw new RuntimeException("some gone wrong with get favorite ids");
        };
    }

    @SneakyThrows
    public static Flux<Favorite> getFavoriteReactive(String favId) {
//        Thread.sleep(3000);
        return Flux.just(Favorite.builder()
                .name("name for favId " + favId)
                .description("desc for favId " + favId)
                .build());
    }

    @SneakyThrows
    public static Flux<Favorite> getDefaultFavoriteListReactive() {
//        Thread.sleep(5000);
        return Flux.just(
                Favorite.builder()
                        .name("name for default favId 1")
                        .description("desc for default favId 1")
                        .build(),
                Favorite.builder()
                        .name("name for default favId 2")
                        .description("desc for default favId 2")
                        .build()
        );
    }

    @SneakyThrows
    public static CompletableFuture<List<String>> getIds() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            return List.of("1", "2", "3", "4", "5");
        });
    }

    public static CompletableFuture<String> getNameById(String id) {
        switch (id) {
            case "1": return CompletableFuture.supplyAsync(() -> "Joe");
            case "2": return CompletableFuture.supplyAsync(() -> "Bart");
            case "3": return CompletableFuture.supplyAsync(() -> "Henry");
            case "4": return CompletableFuture.supplyAsync(() -> "Nicole");
            case "5": return CompletableFuture.supplyAsync(() -> "ABSLAJNFOAJNFOANFANSF");
            default: return CompletableFuture.failedFuture(new RuntimeException("no name"));
        }
    }

    public static CompletableFuture<Integer> getStatById(String id) {
        switch (id) {
            case "1": return CompletableFuture.supplyAsync(() -> 103);
            case "2": return CompletableFuture.supplyAsync(() -> 104);
            case "3": return CompletableFuture.supplyAsync(() -> 105);
            case "4": return CompletableFuture.completedFuture(106);
            case "5": return CompletableFuture.supplyAsync(() -> 121);
            default: return CompletableFuture.failedFuture(new RuntimeException("no statistic"));
        }
    }

    public static Flux<String> getIdsReactive() {
        return Flux.fromIterable(List.of("1", "2", "3", "4", "5"));
    }

    public static Mono<String> getNameByIdReactive(String id) {
        switch (id) {
            case "1": return Mono.just("Joe");
            case "2": return Mono.just("Bart");
            case "3": return Mono.just("Henry");
            case "4": return Mono.just("Nicole");
            case "5": return Mono.just("ABSLAJNFOAJNFOANFANSF");
            default: return Mono.error(new RuntimeException("no name"));
        }
    }

    public static Mono<Integer> getStatByIdReactive(String id) {
        switch (id) {
            case "1": return Mono.just(103);
            case "2": return Mono.just(104);
            case "3": return Mono.just(105);
            case "4": return Mono.just(106);
            case "5": return Mono.just(121);
            default: return Mono.error(new RuntimeException("no statistic"));
        }
    }
}
