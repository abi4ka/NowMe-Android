package com.example.nowme.util;

import com.example.nowme.network.dto.NowmeDto;

import java.util.HashMap;
import java.util.Map;

public class NowmeLikeStateStore {
    private static final Map<Long, LikeState> STATES = new HashMap<>();

    private NowmeLikeStateStore() {
    }

    public static void remember(NowmeDto nowme) {
        if (nowme == null || nowme.id == null || nowme.liked == null) return;

        synchronized (STATES) {
            STATES.put(nowme.id, new LikeState(nowme.liked, nowme.likes));
        }
    }

    public static void update(Long nowmeId, boolean liked, Long likes) {
        if (nowmeId == null) return;

        synchronized (STATES) {
            STATES.put(nowmeId, new LikeState(liked, likes));
        }
    }

    public static void apply(NowmeDto nowme) {
        if (nowme == null || nowme.id == null) return;

        LikeState state;
        synchronized (STATES) {
            state = STATES.get(nowme.id);
        }

        if (state == null) return;
        nowme.liked = state.liked;
        if (state.likes != null) {
            nowme.likes = state.likes;
        }
    }

    private static class LikeState {
        final boolean liked;
        final Long likes;

        LikeState(boolean liked, Long likes) {
            this.liked = liked;
            this.likes = likes;
        }
    }
}
