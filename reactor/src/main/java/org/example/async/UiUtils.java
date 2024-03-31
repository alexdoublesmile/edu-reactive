package org.example.async;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class UiUtils {
    public static void submitOnUiThread(Runnable action) {
        action.run();
    }

    public static void errorPopup(Throwable error) {
        System.err.println(error.getMessage());
    }

    public static Scheduler uiThreadScheduler() {
        return Schedulers.boundedElastic();
    }
}
