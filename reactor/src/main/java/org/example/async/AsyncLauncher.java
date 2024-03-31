package org.example.async;

import lombok.SneakyThrows;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AsyncLauncher {
    private final UserService userService = new UserService();
    private final SuggestionService suggestionService = new SuggestionService();
    private final FavoriteService favoriteService = new FavoriteService();

    public static void main(String[] args) {
        new AsyncLauncher().asyncByReactorNotFuture();
    }

    // Callback:
    // + extra param that gets called when the result is available (Swing’s EventListener hierarchy)
    // - hard to compose, leading to "callback hell"
    public void asyncByCallback(Integer userId) {
        userService.processFavoritesById(userId, getProcessCallback());
        System.out.println("Do other operations while previous line is processing...");
    }

    // Future:
    // + obj wraps access to async process result & polled until result is available (ExecutorService tasks)
    // - no lazy computations, complex error handling & several results returning
    // - hard to look for each join() & get() to prevent blocking
    public void asyncByFuture() {
        CompletableFuture<List<String>> ids = RemoteUtil.getIds();

        CompletableFuture<List<String>> result = ids.thenComposeAsync(l -> {
            Stream<CompletableFuture<String>> zip =
                    l.stream().map(i -> {
                        CompletableFuture<String> nameTask = RemoteUtil.getNameById(i);
                        CompletableFuture<Integer> statTask = RemoteUtil.getStatById(i);

                        return nameTask.thenCombineAsync(statTask, (name, stat) -> "Name " + name + " has stats " + stat);
                    });
            List<CompletableFuture<String>> combinationList = zip.collect(Collectors.toList());
            CompletableFuture<String>[] combinationArray = combinationList.toArray(new CompletableFuture[combinationList.size()]);

            CompletableFuture<Void> allDone = CompletableFuture.allOf(combinationArray);
            return allDone.thenApply(v -> combinationList.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()));
        });

        System.out.println("Do other operations while previous line is processing...");
        final List<String> resultList = result.join();
        System.out.println(resultList);
    }

    private Callback<List<String>> getProcessCallback() {
        return new Callback<>() {
            public void onSuccess(List<String> list) {
                if (list.isEmpty()) {
                    suggestionService.processDefaultFavorites(new Callback<>() {
                        public void onSuccess(List<Favorite> list) {
                            UiUtils.submitOnUiThread(() -> {
                                list.stream()
                                        .limit(5)
                                        .forEach(UiList::show);
                            });
                        }

                        public void onError(Throwable error) {
                            UiUtils.errorPopup(error);
                        }
                    });
                } else {
                    list.stream()
                            .limit(5)
                            .forEach(favId -> favoriteService.processFavoriteById(favId,
                                    new Callback<Favorite>() {
                                        public void onSuccess(Favorite details) {
                                            UiUtils.submitOnUiThread(() -> UiList.show(details));
                                        }

                                        public void onError(Throwable error) {
                                            UiUtils.errorPopup(error);
                                        }
                                    }
                            ));
                }
            }

            public void onError(Throwable error) {
                UiUtils.errorPopup(error);
            }
        };
    }

    @SneakyThrows
    public void asyncByReactorNotCallback(Integer userId) {
        userService.getFavoriteIds(userId)
                .flatMap(favoriteService::getFavorite)
                .switchIfEmpty(suggestionService.getDefaultFavorites())
                .take(5)
                .publishOn(UiUtils.uiThreadScheduler())
                .subscribe(UiList::show, UiUtils::errorPopup);

        System.out.println("Do other operations while previous line is processing...");
        Thread.sleep(5000);
    }

    @SneakyThrows
    public void asyncByReactorNotFuture() {
        Flux<String> ids = RemoteUtil.getIdsReactive();

        Flux<String> combinations =
                ids.flatMap(id -> {
                    Mono<String> nameTask = RemoteUtil.getNameByIdReactive(id);
                    Mono<Integer> statTask = RemoteUtil.getStatByIdReactive(id);

                    return nameTask.zipWith(statTask,
                            (name, stat) -> "Name " + name + " has stats " + stat);
                });

        Mono<List<String>> result = combinations.collectList();

        System.out.println("Do other operations while previous line is processing...");
        // In production, we would continue working with the Flux or Mono
        List<String> results = result.block();
        System.out.println(results);
    }
}
