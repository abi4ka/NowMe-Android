package com.example.nowme.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nowme.R;
import com.example.nowme.network.NowmeApi;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.TokenStorage;
import com.example.nowme.network.dto.UpdateAvatarRequest;
import com.example.nowme.network.dto.NowmeResponse;
import com.example.nowme.network.dto.UserProfileResponse;
import com.example.nowme.ui.auth.AuthActivity;
import com.example.nowme.util.NowmeFeedInvalidationStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private static final long MY_PROFILE_CACHE_KEY = -1L;

    TextView tvUserIcon, tvUsername, tvFollowers, tvFollowing, tvFriends, tvStreak, tvUserAvatar;
    Button btnFollow;
    ImageButton btnCalendar, btnSettings;
    RecyclerView recyclerProfilePosts;
    ProfilePostAdapter postAdapter;
    SwipeRefreshLayout refreshLayout;
    View settingsOverlay, settingsPanel;
    OnBackPressedCallback settingsBackCallback;
    Long userId = null;
    Long profileUserId = null;
    boolean followingProfile = false;
    boolean settingsClosing = false;
    ProfileViewModel viewModel;
    ProfileState profileState;
    LinearLayout btnEditAvatar;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        if (getArguments() != null && getArguments().containsKey("userId")) {
            userId = getArguments().getLong("userId");
        }
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        profileState = viewModel.getState(getCacheKey());

        refreshLayout = view.findViewById(R.id.profileRefresh);
        refreshLayout.setOnRefreshListener(() -> loadProfile(true));

        tvUserIcon = view.findViewById(R.id.tvUserIcon);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvFollowers = view.findViewById(R.id.tvFollowersCount);
        tvFollowing = view.findViewById(R.id.tvFollowingCount);
        tvFriends = view.findViewById(R.id.tvFriendsCount);
        tvStreak = view.findViewById(R.id.tvNowmeNumber);

        btnFollow = view.findViewById(R.id.btnFollow);
        btnFollow.setOnClickListener(v -> toggleFollow());

        btnCalendar = requireActivity().findViewById(R.id.appBarCalendarButton);
        btnCalendar.setOnClickListener(v -> {
            if (profileState.user != null && profileState.user.me) {
                startActivity(new Intent(requireContext(), CalendarHistoryActivity.class));
                requireActivity().overridePendingTransition(
                        R.anim.slide_in_left_to_right,
                        R.anim.stay
                );
            }
        });
        btnSettings = requireActivity().findViewById(R.id.appBarSettingsButton);
        btnSettings.setOnClickListener(v -> showSettingsPanel());

        settingsOverlay = view.findViewById(R.id.settingsOverlay);
        settingsPanel = view.findViewById(R.id.settingsPanel);
        btnEditAvatar = view.findViewById(R.id.btnEditAvatar);
        tvUserAvatar = view.findViewById(R.id.tvUserAvatar);

        view.findViewById(R.id.btnBackSettings).setOnClickListener(v -> closeSettingsPanel(null));
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> closeSettingsPanel(this::logout));
        btnEditAvatar.setOnClickListener(v -> showEditAvatarDialog());
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

        if (profileState.user != null) {
            renderProfile(profileState.user);
        }
        if (profileState.posts != null) {
            postAdapter.setItems(profileState.posts);
            updateProfilePostsHeight(profileState.posts.size());
        }
        if (!profileState.profileLoaded && !profileState.profileLoading) {
            loadProfile(false);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (NowmeFeedInvalidationStore.consumeProfileInvalidated()) {
            profileState.postsLoaded = false;
            loadProfilePosts(true);
        }
    }

    private long getCacheKey() {
        return userId == null ? MY_PROFILE_CACHE_KEY : userId;
    }

    private void loadProfile(boolean forceRefresh) {
        if (profileState.profileLoading) return;
        if (!forceRefresh && profileState.profileLoaded) return;

        NowmeApi api = RetrofitClient.getApi();
        Call<UserProfileResponse> call;

        if (userId == null) {
            call = api.getMyProfile();
        } else {
            call = api.getUserProfile(userId);
        }

        profileState.profileLoading = true;
        call.enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                profileState.profileLoading = false;
                if (response.isSuccessful() && response.body() != null) {

                    UserProfileResponse user = response.body();
                    profileState.user = user;
                    profileState.profileLoaded = true;
                    renderProfile(user);

                    if (forceRefresh || !profileState.postsLoaded) {
                        loadProfilePosts(forceRefresh);
                    } else {
                        stopRefreshing();
                    }
                } else {
                    stopRefreshing();
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                profileState.profileLoading = false;
                stopRefreshing();
                t.printStackTrace();
            }
        });
    }

    private void renderProfile(UserProfileResponse user) {
        profileUserId = user.id;
        followingProfile = user.followingUser || user.following;


        String avatar = user.avatar != null ? user.avatar : "";

        tvUserIcon.setText(avatar);

        if (tvUserAvatar != null) {
            tvUserAvatar.setText(avatar);
        }

        tvUsername.setText(user.username != null ? user.username : "");
        tvFollowers.setText(String.valueOf(getFollowersCount(user)));
        tvFollowing.setText(String.valueOf(user.followingCount != null ? user.followingCount : 0));
        tvFriends.setText(String.valueOf(user.friends != null ? user.friends : 0));

        if (user.streakDays != null) {
            tvStreak.setText(user.streakDays + " \uD83D\uDD25");
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
    }

    private void loadProfilePosts(boolean forceRefresh) {
        if (profileUserId == null) {
            stopRefreshing();
            return;
        }
        if (!forceRefresh && profileState.postsLoaded) {
            stopRefreshing();
            return;
        }
        if (profileState.postsLoading) return;

        profileState.postsLoading = true;
        RetrofitClient.getApi().getProfileNowmes(profileUserId).enqueue(new Callback<List<NowmeResponse>>() {
            @Override
            public void onResponse(Call<List<NowmeResponse>> call, Response<List<NowmeResponse>> response) {
                profileState.postsLoading = false;
                stopRefreshing();
                if (response.isSuccessful()) {
                    List<NowmeResponse> posts = response.body();
                    profileState.posts = posts;
                    profileState.postsLoaded = true;
                    if (postAdapter != null) {
                        postAdapter.setItems(posts);
                    }
                    updateProfilePostsHeight(posts != null ? posts.size() : 0);
                }
            }

            @Override
            public void onFailure(Call<List<NowmeResponse>> call, Throwable t) {
                profileState.postsLoading = false;
                stopRefreshing();
                t.printStackTrace();
            }
        });
    }

    private void stopRefreshing() {
        if (refreshLayout != null) {
            refreshLayout.setRefreshing(false);
        }
    }

    private void updateProfilePostsHeight(int itemCount) {
        if (recyclerProfilePosts == null) return;

        recyclerProfilePosts.post(() -> {
            final int spanCount = 3;
            int rowCount = (itemCount + spanCount - 1) / spanCount;
            int availableWidth = recyclerProfilePosts.getWidth();
            if (availableWidth <= 0) {
                availableWidth = getResources().getDisplayMetrics().widthPixels;
            }

            float density = getResources().getDisplayMetrics().density;
            int itemSpacing = Math.round(2 * density) * 2;
            int itemWidth = availableWidth / spanCount - itemSpacing;
            int itemHeight = Math.max(0, itemWidth * 4 / 3);
            int rowMargin = Math.round(2 * density) * 2;
            int verticalPadding = recyclerProfilePosts.getPaddingTop()
                    + recyclerProfilePosts.getPaddingBottom();

            ViewGroup.LayoutParams layoutParams = recyclerProfilePosts.getLayoutParams();
            int targetHeight = rowCount * (itemHeight + rowMargin) + verticalPadding;
            if (layoutParams.height != targetHeight) {
                layoutParams.height = targetHeight;
                recyclerProfilePosts.setLayoutParams(layoutParams);
            }
        });
    }

    private long getFollowersCount(UserProfileResponse user) {
        if (user.followersCount != null) return user.followersCount;
        return 0L;
    }

    private void updateFollowButton() {
        btnFollow.setEnabled(true);
        btnFollow.setText(followingProfile ? "Following" : "Follow");
        btnFollow.setTextColor(ContextCompat.getColor(
                requireContext(),
                R.color.nowme_text_primary
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
                profileState.profileLoaded = false;
                profileState.postsLoaded = false;
                loadProfile(true);
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
    private void showEditAvatarDialog() {
        EditText input = new EditText(requireContext());

        input.setHint("New emoji");
        input.setText("");
        input.setTextSize(28);
        input.setSingleLine(true);
        input.setGravity(android.view.Gravity.CENTER);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Edit avatar")
                .setMessage("Current: " + tvUserIcon.getText().toString())
                .setView(input)
                .setPositiveButton("SAVE", null)
                .setNegativeButton("CANCEL", null)
                .create();

        dialog.setOnShowListener(d -> {
            input.requestFocus();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(
                        android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                );
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newAvatar = input.getText().toString().trim();

                if (newAvatar.isEmpty()) {
                    input.setError("Write an emoji");
                    return;
                }

                updateAvatar(newAvatar, dialog);
            });
        });

        dialog.show();
    }

    private void updateAvatar(String newAvatar, AlertDialog dialog) {
        UpdateAvatarRequest request = new UpdateAvatarRequest(newAvatar);

        RetrofitClient.getApi().updateAvatar(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {

                    tvUserAvatar.setText(newAvatar);

                    tvUserIcon.setText(newAvatar);

                    if (profileState.user != null) {
                        profileState.user.avatar = newAvatar;
                    }

                    Toast.makeText(requireContext(), "Avatar updated", Toast.LENGTH_SHORT).show();

                    dialog.dismiss();

                } else {
                    try {
                        String error = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e("AvatarUpdate", "Error code: " + response.code() + " body: " + error);
                    } catch (Exception e) {
                        Log.e("AvatarUpdate", "Error reading error body", e);
                    }

                    Toast.makeText(requireContext(), "Error updating avatar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(requireContext(), "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
            btnCalendar.setOnClickListener(null);
        }
        if (btnSettings != null) {
            btnSettings.setVisibility(View.GONE);
            btnSettings.setOnClickListener(null);
        }
    }

    public static class ProfileViewModel extends ViewModel {
        private final Map<Long, ProfileState> states = new HashMap<>();

        ProfileState getState(long key) {
            ProfileState state = states.get(key);
            if (state == null) {
                state = new ProfileState();
                states.put(key, state);
            }
            return state;
        }
    }

    static class ProfileState {
        UserProfileResponse user;
        List<NowmeResponse> posts;
        boolean profileLoaded = false;
        boolean profileLoading = false;
        boolean postsLoaded = false;
        boolean postsLoading = false;
    }
}
