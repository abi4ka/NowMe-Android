package com.example.nowme.util;

public class NowmeFeedInvalidationStore {
    private static boolean feedInvalidated = false;
    private static boolean profileInvalidated = false;

    private NowmeFeedInvalidationStore() {
    }

    public static synchronized void invalidate() {
        feedInvalidated = true;
    }

    public static synchronized void invalidateProfile() {
        profileInvalidated = true;
    }

    public static synchronized boolean consumeInvalidated() {
        boolean invalidated = feedInvalidated;
        feedInvalidated = false;
        return invalidated;
    }

    public static synchronized boolean consumeProfileInvalidated() {
        boolean invalidated = profileInvalidated;
        profileInvalidated = false;
        return invalidated;
    }
}
