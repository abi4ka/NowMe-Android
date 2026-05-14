package com.example.nowme.util;

public class NowmeFeedInvalidationStore {
    private static boolean feedInvalidated = false;

    private NowmeFeedInvalidationStore() {
    }

    public static synchronized void invalidate() {
        feedInvalidated = true;
    }

    public static synchronized boolean consumeInvalidated() {
        boolean invalidated = feedInvalidated;
        feedInvalidated = false;
        return invalidated;
    }
}
