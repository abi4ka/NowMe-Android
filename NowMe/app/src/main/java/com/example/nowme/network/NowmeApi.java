package com.example.nowme.network;

import com.example.nowme.PageResponse;
import com.example.nowme.network.dto.AuthRequest;
import com.example.nowme.network.dto.AuthResponse;
import com.example.nowme.network.dto.CommentRequest;
import com.example.nowme.network.dto.CommentResponse;
import com.example.nowme.network.dto.NowmeResponse;
import com.example.nowme.network.dto.RefreshRequest;
import com.example.nowme.network.dto.UpdateAvatarRequest;
import com.example.nowme.network.dto.UpdateNowmeVisibilityRequest;
import com.example.nowme.network.dto.UserProfileResponse;
import com.example.nowme.network.dto.UserSearchResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NowmeApi {

    @POST("/auth/login")
    Call<AuthResponse> login(@Body AuthRequest user);

    @POST("/auth/register")
    Call<AuthResponse> register(@Body AuthRequest user);

    @Multipart
    @POST("/nowme")
    Call<Long> createNowme(
            @Part MultipartBody.Part image,
            @Part("description") RequestBody description
    );

    @POST("/auth/refresh")
    Call<AuthResponse> refresh(@Body RefreshRequest request);

    @POST("/auth/logout")
    Call<Void> logout(@Body RefreshRequest request);

    @GET("/nowme")
    Call<PageResponse<NowmeResponse>> getNowmes(
    );

    @GET("/nowme/users/{userId}")
    Call<List<NowmeResponse>> getProfileNowmes(@Path("userId") Long userId);

    @GET("/nowme/me/history")
    Call<List<NowmeResponse>> getMyNowmeHistory();

    @GET("/nowme/{id}/image")
    Call<ResponseBody> getNowmeImage(@Path("id") Long id);

    @POST("/nowme/{id}/like")
    Call<Long> like(@Path("id") Long id);

    @DELETE("/nowme/{id}/like")
    Call<Long> unlike(@Path("id") Long id);

    @PUT("/nowme/{id}/visibility")
    Call<NowmeResponse> updateNowmeVisibility(
            @Path("id") Long id,
            @Body UpdateNowmeVisibilityRequest request
    );

    @PUT("/nowme/{id}/favorite")
    Call<NowmeResponse> toggleNowmeFavorite(@Path("id") Long id);

    @DELETE("/nowme/{id}")
    Call<Void> deleteNowme(@Path("id") Long id);

    @GET("/users/me")
    Call<UserProfileResponse> getMyProfile();

    @GET("/users/{id}")
    Call<UserProfileResponse> getUserProfile(@Path("id") Long id);

    @POST("/follow/{userId}")
    Call<ResponseBody> followUser(@Path("userId") Long userId);

    @DELETE("/follow/{userId}")
    Call<ResponseBody> unfollowUser(@Path("userId") Long userId);

    @PUT("users/avatar")
    Call<Void> updateAvatar(@Body UpdateAvatarRequest request);

    @GET("users/search")
    Call<PageResponse<UserSearchResponse>> searchUsers(
            @Query("query") String query,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("/nowme/{id}/comment")
    Call<PageResponse<CommentResponse>> getComments(
            @Path("id") Long nowmeId,
            @Query("page") int page,
            @Query("size") int size
    );

    @POST("/nowme/{id}/comment")
    Call<CommentResponse> createComment(
            @Path("id") Long nowmeId,
            @Body CommentRequest request
    );
}