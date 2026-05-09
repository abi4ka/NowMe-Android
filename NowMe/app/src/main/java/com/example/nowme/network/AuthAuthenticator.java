package com.example.nowme.network;

import android.content.Context;

import com.example.nowme.MyApplication;
import com.example.nowme.network.dto.AuthResponse;
import com.example.nowme.network.dto.RefreshRequest;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class AuthAuthenticator implements Authenticator {

    private final NowmeApi authApi;

    public AuthAuthenticator(NowmeApi authApi) {
        this.authApi = authApi;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {

        Context context = MyApplication.getAppContext();
        if (context == null) return null;

        String refreshToken = TokenStorage.getRefresh(context);
        String accessToken = TokenStorage.getAccess(context);

        if (refreshToken == null || accessToken == null) {
            SessionManager.expireSession(context);
            return null;
        }

        if (responseCount(response) >= 2) {
            SessionManager.expireSession(context);
            return null;
        }

        RefreshRequest request = new RefreshRequest();
        request.accessToken = accessToken;
        request.refreshToken = refreshToken;

        retrofit2.Response<AuthResponse> refreshResponse =
                authApi.refresh(request).execute();

        if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) {
            SessionManager.expireSession(context);
            return null;
        }

        AuthResponse newTokens = refreshResponse.body();
        TokenStorage.save(context, newTokens);

        // repeat original request with new access
        return response.request().newBuilder()
                .header("Authorization", "Bearer " + newTokens.accessToken)
                .build();
    }

    private int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}
