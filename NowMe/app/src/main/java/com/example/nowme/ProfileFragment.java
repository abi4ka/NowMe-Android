package com.example.nowme;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.nowme.network.NowmeApi;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.UserProfileDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    TextView tvUserIcon, tvUsername, tvFollowers, tvFollowing, tvFriends;
    Button btnFollow, btnUnfollow;
    Long userId = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        if (getArguments() != null) {
            userId = getArguments().getLong("userId");
        }

        tvUserIcon = view.findViewById(R.id.tvUserIcon);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvFollowers = view.findViewById(R.id.tvFollowersCount);
        tvFollowing = view.findViewById(R.id.tvFollowingCount);
        tvFriends = view.findViewById(R.id.tvFriendsCount);

        btnFollow = view.findViewById(R.id.btnFollow);
        btnUnfollow = view.findViewById(R.id.btnUnfollow);

        loadProfile();

        return view;
    }

    private void loadProfile() {

        NowmeApi api = RetrofitClient.getApi();
        Call<UserProfileDto> call;

        if (userId == null) {
            call = api.getMyProfile();
        } else {
            call = api.getUserProfile(userId);
        }

        call.enqueue(new Callback<UserProfileDto>() {
            @Override
            public void onResponse(Call<UserProfileDto> call, Response<UserProfileDto> response) {
                if (response.isSuccessful() && response.body() != null) {

                    UserProfileDto user = response.body();

                    tvUserIcon.setText(user.avatar);
                    tvUsername.setText(user.username);
                    tvFollowers.setText(String.valueOf(user.followers != null ? user.followers : 0));
                    tvFollowing.setText(String.valueOf(user.following != null ? user.following : 0));
                    tvFriends.setText(String.valueOf(user.friends));

                    if (user.me) {
                        btnFollow.setVisibility(View.GONE);
                        btnUnfollow.setVisibility(View.GONE);
                    } else {

                        if (user.followingUser) {
                            btnFollow.setVisibility(View.GONE);
                            btnUnfollow.setVisibility(View.VISIBLE);
                        } else {
                            btnFollow.setVisibility(View.VISIBLE);
                            btnUnfollow.setVisibility(View.GONE);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<UserProfileDto> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
}