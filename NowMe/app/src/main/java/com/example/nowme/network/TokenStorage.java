package com.example.nowme.network;

import android.content.Context;

import com.example.nowme.network.dto.AuthResponse;

public class TokenStorage {

    private static final String PREF = "session";
    private static final String KEY_ACCESS = "accessToken";
    private static final String KEY_REFRESH = "refreshToken";

    public static void save(Context context, AuthResponse dto) {
        SessionManager.resetRedirectState();

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACCESS, dto.accessToken)
                .putString(KEY_REFRESH, dto.refreshToken)
                .apply();
    }

    public static String getAccess(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_ACCESS, null);
    }

    public static String getRefresh(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_REFRESH, null);
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}
