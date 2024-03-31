package org.example.async;

public interface Callback<T> {
    void onSuccess(T value);
    void onError(Throwable error);
}
