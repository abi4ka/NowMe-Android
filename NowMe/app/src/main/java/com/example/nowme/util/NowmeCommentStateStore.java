package com.example.nowme.util;

import com.example.nowme.network.dto.NowmeResponse;

import java.util.HashMap;
import java.util.Map;

public class NowmeCommentStateStore {

    private static final Map<Long, Long> commentCounts = new HashMap<>();

    public static void clear() {
        commentCounts.clear();
    }

    public static void remember(NowmeResponse nowme) {
        if (nowme == null || nowme.id == null || nowme.comments == null) return;

        commentCounts.put(nowme.id, nowme.comments);
    }

    public static void apply(NowmeResponse nowme) {
        if (nowme == null || nowme.id == null) return;

        Long storedCount = commentCounts.get(nowme.id);

        if (storedCount != null) {
            nowme.comments = storedCount;
        }
    }

    public static void update(Long nowmeId, Long comments) {
        if (nowmeId == null || comments == null) return;

        commentCounts.put(nowmeId, comments);
    }

    public static Long increase(Long nowmeId, Long currentComments) {
        if (nowmeId == null) return currentComments;

        long safeCurrent = currentComments != null ? currentComments : 0;
        long newValue = safeCurrent + 1;

        commentCounts.put(nowmeId, newValue);

        return newValue;
    }
}