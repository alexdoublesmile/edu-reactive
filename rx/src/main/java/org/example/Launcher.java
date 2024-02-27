package org.example;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.example.entity.Client;

import java.util.ArrayList;

import static io.reactivex.rxjava3.core.Flowable.create;

public class Launcher {
    public static void main(String[] args) {
        final ArrayList<Client> clientList = new ArrayList<>();
        clientList.add(new Client("John", 18, true));
        clientList.add(new Client("Ann", 18, true));
        clientList.add(new Client("Sveta", 28, false));
        clientList.add(new Client("Kate", 28, false));
        clientList.add(new Client("Alex", 28, true));

        runAsStream(clientList);
        runRxSingleThread(clientList);
        runRxMultiThread(clientList);

        System.out.println("------------------");

    }

    private static void runRxMultiThread(ArrayList<Client> clientList) {
        final Flowable<Client> clientFlow = create(emitter -> {
            clientList.forEach(emitter::onNext);
            emitter.onComplete();
        }, BackpressureStrategy.BUFFER); // overflow handling strategy

        clientFlow
                // run all operations in computation-pool (for huge number of operations)
                .observeOn(Schedulers.computation())
                // run flow creation by IO-pool (for one expensive operation)
                .subscribeOn(Schedulers.io())
                .filter(Client::isActive)
                .map(Client::getName)
                // run specific operation by specific pool
                .observeOn(Schedulers.io())
                .forEach(System.out::println);

    }

    private static void runRxSingleThread(ArrayList<Client> clientList) {
        final Flowable<Client> clientFlow = create(emitter -> {
            clientList.forEach(emitter::onNext);
            emitter.onComplete();
        }, BackpressureStrategy.BUFFER);

        clientFlow
                .filter(Client::isActive)
                .map(Client::getName)
                .forEach(System.out::println);
    }

    private static void runAsStream(ArrayList<Client> clientList) {
        clientList.stream()
                .filter(Client::isActive)
                .map(Client::getName)
                .forEach(System.out::println);
    }


}
