package com.example.nowme.network;

import com.example.nowme.network.dto.AuthDto;
import com.example.nowme.network.dto.UserDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface NowmeApi {

    @POST("/auth/login")
    Call<AuthDto> login(@Body UserDto user);

    @POST("/auth/register")
    Call<AuthDto> register(@Body UserDto user);


}


