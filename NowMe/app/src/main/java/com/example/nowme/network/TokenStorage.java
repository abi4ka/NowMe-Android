package com.example.nowme.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.nowme.network.dto.AuthResponse;

public class TokenStorage {

    private static final String PREF = "session_encrypted";
    private static final String LEGACY_PREF = "session";
    private static final String KEY_ACCESS = "accessToken";
    private static final String KEY_REFRESH = "refreshToken";

    private static SharedPreferences getPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    PREF,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.w("TokenStorage", "Falling back to standard SharedPreferences", e);
            return context.getSharedPreferences(LEGACY_PREF, Context.MODE_PRIVATE);
        }
    }

    public static void save(Context context, AuthResponse dto) {
        SessionManager.resetRedirectState();

        getPrefs(context)
                .edit()
                .putString(KEY_ACCESS, dto.accessToken)
                .putString(KEY_REFRESH, dto.refreshToken)
                .apply();
    }

    public static String getAccess(Context context) {
        String access = getPrefs(context).getString(KEY_ACCESS, null);
        if (access == null) {
            access = context.getSharedPreferences(LEGACY_PREF, Context.MODE_PRIVATE).getString(KEY_ACCESS, null);
        }
        return access;
    }

    public static String getRefresh(Context context) {
        String refresh = getPrefs(context).getString(KEY_REFRESH, null);
        if (refresh == null) {
            refresh = context.getSharedPreferences(LEGACY_PREF, Context.MODE_PRIVATE).getString(KEY_REFRESH, null);
        }
        return refresh;
    }

    public static void clear(Context context) {
        getPrefs(context).edit().clear().apply();
        context.getSharedPreferences(LEGACY_PREF, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
