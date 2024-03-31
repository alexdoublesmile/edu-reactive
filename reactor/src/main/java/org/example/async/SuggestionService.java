package org.example.async;

import reactor.core.publisher.Flux;

import java.util.List;

public class SuggestionService {
    public void processDefaultFavorites(Callback<List<Favorite>> callback) {
        try {
            // e.g. trying to get remote value here
            List<Favorite> favoritesList = RemoteUtil.getDefaultFavoriteList();
            callback.onSuccess(favoritesList);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public Flux<Favorite> getDefaultFavorites() {
        return RemoteUtil.getDefaultFavoriteListReactive();
    }
}
