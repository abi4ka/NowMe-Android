package com.example.nowme.network;

import com.example.nowme.network.dto.RefreshRequest;
import com.example.nowme.util.NowmeCommentStateStore;
import com.example.nowme.util.NowmeLikeStateStore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.example.nowme.ui.auth.AuthActivity;

import java.util.concurrent.atomic.AtomicBoolean;

public class SessionManager {

    public static final String EXTRA_SESSION_EXPIRED = "sessionExpired";

    private static final AtomicBoolean redirectingToLogin = new AtomicBoolean(false);

    public static void resetRedirectState() {
        redirectingToLogin.set(false);
    }

    /** Logs out on the server (best effort) and drops all local session state. */
    public static void logout(Context context) {
        Context appContext = context.getApplicationContext();
        String refreshToken = TokenStorage.getRefresh(appContext);
        if (refreshToken != null) {
            RefreshRequest request = new RefreshRequest();
            request.accessToken = TokenStorage.getAccess(appContext);
            request.refreshToken = refreshToken;
            RetrofitClient.getApi().logout(request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                }
            });
        }
        clearLocalSession(appContext);
    }

    // Cached like/comment states belong to the previous user and must not leak into the next session
    private static void clearLocalSession(Context context) {
        TokenStorage.clear(context);
        NowmeLikeStateStore.clear();
        NowmeCommentStateStore.clear();
    }

    public static void expireSession(Context context) {
        if (context == null || !redirectingToLogin.compareAndSet(false, true)) {
            return;
        }

        Context appContext = context.getApplicationContext();
        clearLocalSession(appContext);

        new Handler(Looper.getMainLooper()).post(() -> {
            Intent intent = new Intent(appContext, AuthActivity.class);
            intent.putExtra(EXTRA_SESSION_EXPIRED, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            appContext.startActivity(intent);
        });
    }
}
