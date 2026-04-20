package com.example.nowme;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nowme.network.NowmeApi;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.NowmeDto;
import com.example.nowme.network.dto.UserProfileDto;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    TextView tvUserIcon, tvUsername, tvFollowers, tvFollowing, tvFriends, tvStreak;
    Button btnFollow;
    RecyclerView recyclerProfilePosts;
    ProfilePostAdapter postAdapter;
    Long userId = null;
    Long profileUserId = null;
    boolean followingProfile = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        if (getArguments() != null && getArguments().containsKey("userId")) {
            userId = getArguments().getLong("userId");
        }

        tvUserIcon = view.findViewById(R.id.tvUserIcon);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvFollowers = view.findViewById(R.id.tvFollowersCount);
        tvFollowing = view.findViewById(R.id.tvFollowingCount);
        tvFriends = view.findViewById(R.id.tvFriendsCount);
        tvStreak = view.findViewById(R.id.tvNowmeNumber);

        btnFollow = view.findViewById(R.id.btnFollow);
        btnFollow.setOnClickListener(v -> toggleFollow());

        recyclerProfilePosts = view.findViewById(R.id.recyclerProfilePosts);
        postAdapter = new ProfilePostAdapter();
        recyclerProfilePosts.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerProfilePosts.setAdapter(postAdapter);

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

                    profileUserId = user.id;
                    followingProfile = user.followingUser || user.following;

                    tvUserIcon.setText(user.avatar != null ? user.avatar : "");
                    tvUsername.setText(user.username != null ? user.username : "");
                    tvFollowers.setText(String.valueOf(getFollowersCount(user)));
                    tvFollowing.setText(String.valueOf(user.followingCount != null ? user.followingCount : 0));
                    tvFriends.setText(String.valueOf(user.friends != null ? user.friends : 0));

                    if (user.streakDays != null) {
                        tvStreak.setText(user.streakDays + " 🔥");
                        tvStreak.setVisibility(View.VISIBLE);
                    } else {
                        tvStreak.setText("");
                        tvStreak.setVisibility(View.GONE);
                    }

                    if (user.me) {
                        btnFollow.setVisibility(View.GONE);
                    } else {
                        updateFollowButton();
                        btnFollow.setVisibility(View.VISIBLE);
                    }

                    loadProfilePosts();
                }
            }

            @Override
            public void onFailure(Call<UserProfileDto> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void loadProfilePosts() {
        if (profileUserId == null) return;

        RetrofitClient.getApi().getProfileNowmes(profileUserId).enqueue(new Callback<List<NowmeDto>>() {
            @Override
            public void onResponse(Call<List<NowmeDto>> call, Response<List<NowmeDto>> response) {
                if (response.isSuccessful()) {
                    postAdapter.setItems(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<NowmeDto>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private long getFollowersCount(UserProfileDto user) {
        if (user.followersCount != null) return user.followersCount;
        if (user.followerCount != null) return user.followerCount;
        return 0L;
    }

    private void updateFollowButton() {
        btnFollow.setEnabled(true);
        btnFollow.setText(followingProfile ? "Following" : "Follow");
        btnFollow.setTextColor(followingProfile ? 0xFFFFFFFF : 0xFF001B26);
        btnFollow.setBackgroundResource(followingProfile
                ? R.drawable.button_following
                : R.drawable.button_follow);
    }

    private void toggleFollow() {
        if (profileUserId == null) return;

        btnFollow.setEnabled(false);
        Call<ResponseBody> call = followingProfile
                ? RetrofitClient.getApi().unfollowUser(profileUserId)
                : RetrofitClient.getApi().followUser(profileUserId);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    btnFollow.setEnabled(true);
                    Toast.makeText(getContext(), "Follow error", Toast.LENGTH_SHORT).show();
                    return;
                }

                followingProfile = !followingProfile;
                updateFollowButton();
                loadProfile();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btnFollow.setEnabled(true);
                Toast.makeText(getContext(), "Follow error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
