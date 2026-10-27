package com.example.nowme.util;

public class NowmeFeedInvalidationStore {
    private static boolean feedInvalidated = false;
    private static boolean profileInvalidated = false;

    private NowmeFeedInvalidationStore() {
    }

    public static synchronized void invalidate() {
        feedInvalidated = true;
    }

    /** A post was created, deleted or changed: reload both the feed and the profile grid. */
    public static synchronized void invalidateAll() {
        invalidate();
        invalidateProfile();
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
