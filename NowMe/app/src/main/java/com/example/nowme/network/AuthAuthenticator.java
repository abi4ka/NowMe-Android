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

    private static final Object REFRESH_LOCK = new Object();

    private final NowmeApi authApi;

    public AuthAuthenticator(NowmeApi authApi) {
        this.authApi = authApi;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        Request failedRequest = response.request();
        String failedAuthorization = failedRequest.header("Authorization");

        // A 401 from login/register (wrong password) or an anonymous call is not an expired session
        if (failedAuthorization == null || failedRequest.url().encodedPath().startsWith("/auth/")) {
            return null;
        }

        Context context = MyApplication.getAppContext();
        if (context == null) return null;

        // One refresh at a time: the backend rotates refresh tokens, so parallel refreshes
        // with the same token would make all but the first fail and log the user out.
        synchronized (REFRESH_LOCK) {
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

            // Another request refreshed the tokens while this one waited: just retry with them
            if (!failedAuthorization.equals("Bearer " + accessToken)) {
                return withAccessToken(failedRequest, accessToken);
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

            return withAccessToken(failedRequest, newTokens.accessToken);
        }
    }

    private static Request withAccessToken(Request request, String accessToken) {
        return request.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
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
