package com.example.nowme.network;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.example.nowme.AuthActivity;

import java.util.concurrent.atomic.AtomicBoolean;

public class SessionManager {

    public static final String EXTRA_SESSION_EXPIRED = "sessionExpired";

    private static final AtomicBoolean redirectingToLogin = new AtomicBoolean(false);

    public static void resetRedirectState() {
        redirectingToLogin.set(false);
    }

    public static void expireSession(Context context) {
        if (context == null || !redirectingToLogin.compareAndSet(false, true)) {
            return;
        }

        Context appContext = context.getApplicationContext();
        TokenStorage.clear(appContext);

        new Handler(Looper.getMainLooper()).post(() -> {
            Intent intent = new Intent(appContext, AuthActivity.class);
            intent.putExtra(EXTRA_SESSION_EXPIRED, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            appContext.startActivity(intent);
        });
    }
}
