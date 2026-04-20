package com.example.nowme.network;

import com.example.nowme.PageResponse;
import com.example.nowme.network.dto.AuthDto;
import com.example.nowme.network.dto.NowmeDto;
import com.example.nowme.network.dto.RefreshRequest;
import com.example.nowme.network.dto.UserDto;
import com.example.nowme.network.dto.UserProfileDto;

import java.util.List;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

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

    @GET("/nowme")
    Call<PageResponse<NowmeDto>> getNowmes(
    );

    @GET("/nowme/{id}/image")
    Call<ResponseBody> getNowmeImage(@Path("id") Long id);

    @POST("/nowme/{id}/like")
    Call<Long> like(@Path("id") Long id);

    @DELETE("/nowme/{id}/like")
    Call<Long> unlike(@Path("id") Long id);

    @GET("/users/me")
    Call<UserProfileDto> getMyProfile();

    @GET("/users/{id}")
    Call<UserProfileDto> getUserProfile(@Path("id") Long id);

    @POST("/follow/{userId}")
    Call<ResponseBody> followUser(@Path("userId") Long userId);

    @DELETE("/follow/{userId}")
    Call<ResponseBody> unfollowUser(@Path("userId") Long userId);
}


