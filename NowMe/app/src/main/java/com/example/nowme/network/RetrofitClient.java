package com.example.nowme.network;

import com.example.nowme.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    /** Server address for the current build type, see nowme.baseUrl in app/build.gradle.kts. */
    public static final String BASE_URL = BuildConfig.BASE_URL;

    private static volatile NowmeApi apiInstance;
    private static volatile OkHttpClient httpClient;

    public static NowmeApi getApi() {
        if (apiInstance == null) {
            synchronized (RetrofitClient.class) {
                if (apiInstance == null) {
                    apiInstance = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(getHttpClient())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(NowmeApi.class);
                }
            }
        }
        return apiInstance;
    }

    /**
     * Client that adds the access token and refreshes it on 401. Shared by Retrofit and Glide,
     * so images keep loading after the access token expires.
     */
    public static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (RetrofitClient.class) {
                if (httpClient == null) {
                    // Refresh calls use a separate client without the authenticator to avoid loops
                    NowmeApi refreshApi = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(withDebugLogging(new OkHttpClient.Builder()).build())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(NowmeApi.class);

                    OkHttpClient.Builder builder = new OkHttpClient.Builder()
                            .addInterceptor(new AuthInterceptor()) // access token
                            .authenticator(new AuthAuthenticator(refreshApi)); // auto-refresh
                    httpClient = withDebugLogging(builder).build();
                }
            }
        }
        return httpClient;
    }

    // Headers only and never in release: bodies contain passwords, tokens and image bytes
    private static OkHttpClient.Builder withDebugLogging(OkHttpClient.Builder builder) {
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            logging.redactHeader("Authorization");
            builder.addNetworkInterceptor(logging);
        }
        return builder;
    }
}
