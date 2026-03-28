package com.example.nowme.network;

import android.content.Context;
import com.example.nowme.MyApplication;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Context context = MyApplication.getAppContext();
        if (context == null) return chain.proceed(chain.request());

        String accessToken = TokenStorage.getAccess(context);
        if (accessToken == null) return chain.proceed(chain.request());

        Request request = chain.request().newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();

        return chain.proceed(request);
    }
}