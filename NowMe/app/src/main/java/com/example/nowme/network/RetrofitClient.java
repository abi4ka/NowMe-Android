package com.example.nowme.network;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class RetrofitClient {

    private static final String BASE_URL = "http://10.0.2.2:25565/";
    private static volatile NowmeApi apiInstance;

    public static NowmeApi getApi() {
        if (apiInstance == null) {
            synchronized (RetrofitClient.class) {
                if (apiInstance == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                    NowmeApi tempApi = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(NowmeApi.class);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(logging)
                            .addInterceptor(new AuthInterceptor()) // access token
                            .authenticator(new AuthAuthenticator(tempApi)) // auto-refresh
                            .build();

                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    apiInstance = retrofit.create(NowmeApi.class);
                }
            }
        }
        return apiInstance;
    }
}