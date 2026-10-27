package com.example.nowme.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Background thread for disk and bitmap work, plus a way back to the main thread. */
public final class AppExecutors {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "nowme-io");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public static void io(Runnable task) {
        IO.execute(task);
    }

    public static void main(Runnable task) {
        MAIN.post(task);
    }
}
