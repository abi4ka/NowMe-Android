package com.example.nowme.network;

import com.example.nowme.network.dto.UserDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface NowmeApi {

    @POST("/auth/login")
    Call<String> login(@Body UserDto user);

    @POST("/auth/register")
    Call<String> register(@Body UserDto user);

}


