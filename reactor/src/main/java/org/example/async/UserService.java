package org.example.async;

import reactor.core.publisher.Flux;

import java.util.List;

public class UserService {
    public Flux<String> getFavoriteIds(Integer userId) {
        // e.g. trying to get remote value here
        return RemoteUtil.getFavoriteIdsReactive(userId);
    }

    public void processFavoritesById(Integer userId, Callback<List<String>> callback) {
        // e.g. trying to get remote value here
        new Thread(() -> runAsync(userId, callback)).start();
    }

    private void runAsync(Integer userId, Callback<List<String>> callback) {
        try {
            List<String> favoriteIdsList = RemoteUtil.getFavoriteIds(userId);
            callback.onSuccess(favoriteIdsList);
        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
