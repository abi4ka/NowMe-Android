package com.example.nowme.network;

import com.example.nowme.network.dto.AuthDto;
import com.example.nowme.network.dto.RefreshRequest;
import com.example.nowme.network.dto.UserDto;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface NowmeApi {

    @POST("/auth/login")
    Call<AuthDto> login(@Body UserDto user);

    @POST("/auth/register")
    Call<AuthDto> register(@Body UserDto user);

    @Multipart
    @POST("/nowme")
    Call<Long> createNowme(
            @Part MultipartBody.Part image,
            @Part("description") RequestBody description
    );

    @POST("/auth/refresh")
    Call<AuthDto> refresh(@Body RefreshRequest request);
}


