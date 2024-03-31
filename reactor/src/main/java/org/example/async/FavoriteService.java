package org.example.async;

import reactor.core.publisher.Flux;

public class FavoriteService {
    public void processFavoriteById(String favId, Callback<Favorite> callback) {
        try {
            // e.g. trying to get remote value here
            Favorite favorite = RemoteUtil.getFavorite(favId);
            callback.onSuccess(favorite);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public Flux<Favorite> getFavorite(String favId) {
        return RemoteUtil.getFavoriteReactive(favId);
    }
}
