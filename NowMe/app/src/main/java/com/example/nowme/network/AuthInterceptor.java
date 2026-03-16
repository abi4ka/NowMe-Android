package com.example.nowme.network;

import android.content.Context;
import com.example.nowme.MyApplication;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private String getSessionToken() {
        Context context = MyApplication.getAppContext();

        if (context == null) return null;

        return context
                .getSharedPreferences("session", Context.MODE_PRIVATE)
                .getString("sessionToken", null);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        String token = getSessionToken();

        if (token == null) {
            return chain.proceed(originalRequest);
        }

        Request newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(newRequest);
    }
}
