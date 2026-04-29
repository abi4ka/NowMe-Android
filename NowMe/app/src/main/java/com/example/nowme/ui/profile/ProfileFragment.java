package com.example.nowme.ui.profile;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nowme.R;
import com.example.nowme.network.NowmeApi;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.TokenStorage;
import com.example.nowme.network.dto.NowmeDto;
import com.example.nowme.network.dto.UserProfileDto;
import com.example.nowme.ui.auth.AuthActivity;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    TextView tvUserIcon, tvUsername, tvFollowers, tvFollowing, tvFriends, tvStreak;
    Button btnFollow;
    ImageButton btnCalendar, btnSettings;
    RecyclerView recyclerProfilePosts;
    ProfilePostAdapter postAdapter;
    View settingsOverlay, settingsPanel;
    OnBackPressedCallback settingsBackCallback;
    Long userId = null;
    Long profileUserId = null;
    boolean followingProfile = false;
    boolean settingsClosing = false;

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

        btnCalendar = requireActivity().findViewById(R.id.appBarCalendarButton);
        btnSettings = requireActivity().findViewById(R.id.appBarSettingsButton);
        btnSettings.setOnClickListener(v -> showSettingsPanel());

        settingsOverlay = view.findViewById(R.id.settingsOverlay);
        settingsPanel = view.findViewById(R.id.settingsPanel);
        view.findViewById(R.id.btnBackSettings).setOnClickListener(v -> closeSettingsPanel(null));
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> closeSettingsPanel(this::logout));
        settingsBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                closeSettingsPanel(null);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, settingsBackCallback);

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
                        btnCalendar.setVisibility(View.VISIBLE);
                        btnSettings.setVisibility(View.VISIBLE);

                    } else {
                        updateFollowButton();
                        btnFollow.setVisibility(View.VISIBLE);
                        btnCalendar.setVisibility(View.GONE);
                        btnSettings.setVisibility(View.GONE);
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
        return 0L;
    }

    private void updateFollowButton() {
        btnFollow.setEnabled(true);
        btnFollow.setText(followingProfile ? "Following" : "Follow");
        btnFollow.setTextColor(ContextCompat.getColor(
                requireContext(),
                followingProfile ? R.color.nowme_text_primary : R.color.nowme_accent_dark_text
        ));
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

    private void showSettingsPanel() {
        if (settingsOverlay == null || settingsPanel == null) return;

        settingsClosing = false;
        settingsBackCallback.setEnabled(true);
        settingsPanel.animate().cancel();
        settingsPanel.setTranslationX(getResources().getDisplayMetrics().widthPixels);
        settingsOverlay.setVisibility(View.VISIBLE);
        settingsPanel.post(() -> openSettingsPanel(settingsPanel));
    }

    private void openSettingsPanel(View panel) {
        panel.animate().cancel();
        panel.animate()
                .translationX(0f)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void closeSettingsPanel(Runnable afterClose) {
        if (settingsOverlay == null || settingsPanel == null || settingsClosing) return;
        settingsClosing = true;
        settingsBackCallback.setEnabled(false);

        settingsPanel.animate().cancel();
        settingsPanel.animate()
                .translationX(settingsPanel.getWidth())
                .setDuration(240)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    settingsOverlay.setVisibility(View.GONE);
                    settingsPanel.setTranslationX(0f);
                    settingsClosing = false;
                    if (afterClose != null) {
                        afterClose.run();
                    }
                })
                .start();
    }

    private void logout() {
        if (getContext() == null) return;

        TokenStorage.clear(requireContext());

        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (btnCalendar != null) {
            btnCalendar.setVisibility(View.GONE);
        }
        if (btnSettings != null) {
            btnSettings.setVisibility(View.GONE);
            btnSettings.setOnClickListener(null);
        }
    }
}
