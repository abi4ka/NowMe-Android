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

    TextView tvUserIcon, tvUsername, tvFollowers, tvFollowing, tvFriends, tvStreak;
    Button btnFollow;
    ImageButton btnCalendar, btnSettings;
    RecyclerView recyclerProfilePosts;
    View profileEmptyState;
    ProfilePostAdapter postAdapter;
    SwipeRefreshLayout refreshLayout;
    Long userId = null;
    Long profileUserId = null;
    boolean followingProfile = false;
    ProfileViewModel viewModel;
    ProfileState profileState;

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
        if (btnCalendar != null) {
            btnCalendar.setOnClickListener(v -> {
                if (profileState.user != null && profileState.user.me) {
                    startActivity(new Intent(requireContext(), CalendarHistoryActivity.class));
                    requireActivity().overridePendingTransition(
                            R.anim.slide_in_left_to_right,
                            R.anim.stay
                    );
                }
            });
        }
        btnSettings = requireActivity().findViewById(R.id.appBarSettingsButton);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showSettingsPanel());
        }

        recyclerProfilePosts = view.findViewById(R.id.recyclerProfilePosts);
        profileEmptyState = view.findViewById(R.id.profileEmptyState);
        postAdapter = new ProfilePostAdapter();
        recyclerProfilePosts.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerProfilePosts.setAdapter(postAdapter);

        if (profileState.user != null) {
            renderProfile(profileState.user);
        }
        if (profileState.posts != null) {
            postAdapter.setItems(profileState.posts);
            int count = profileState.posts.size();
            updateProfilePostsHeight(count);
            if (profileEmptyState != null) {
                profileEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            }
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
                    // The user may have left the screen while loading; state is kept in the ViewModel
                    if (!isViewAlive()) return;
                    renderProfile(user);

                    if (forceRefresh || !profileState.postsLoaded) {
                        loadProfilePosts(forceRefresh);
                    } else {
                        stopRefreshing();
                    }
                } else if (isViewAlive()) {
                    stopRefreshing();
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                profileState.profileLoading = false;
                if (isViewAlive()) {
                    stopRefreshing();
                }
            }
        });
    }

    private void renderProfile(UserProfileResponse user) {
        profileUserId = user.id;
        followingProfile = user.followingUser || user.following;


        String avatar = user.avatar != null ? user.avatar : "";

        tvUserIcon.setText(avatar);

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
                if (response.isSuccessful()) {
                    profileState.posts = response.body();
                    profileState.postsLoaded = true;
                }
                if (!isViewAlive()) return;
                stopRefreshing();
                if (response.isSuccessful()) {
                    List<NowmeResponse> posts = response.body();
                    if (postAdapter != null) {
                        postAdapter.setItems(posts);
                    }
                    int postCount = posts != null ? posts.size() : 0;
                    updateProfilePostsHeight(postCount);
                    if (profileEmptyState != null) {
                        profileEmptyState.setVisibility(postCount == 0 ? View.VISIBLE : View.GONE);
                    }
                } else {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<NowmeResponse>> call, Throwable t) {
                profileState.postsLoading = false;
                stopRefreshing();
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
                }
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
                if (!isViewAlive()) return;
                if (!response.isSuccessful()) {
                    btnFollow.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.follow_error, Toast.LENGTH_SHORT).show();
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
                if (!isViewAlive()) return;
                btnFollow.setEnabled(true);
                Toast.makeText(requireContext(), R.string.follow_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isViewAlive() {
        return isAdded() && getView() != null;
    }

    private void showSettingsPanel() {
        String avatar = profileState.user != null ? profileState.user.avatar : null;
        SettingsBottomSheetFragment bottomSheet = SettingsBottomSheetFragment.newInstance(avatar, newAvatar -> {
            if (profileState.user != null) {
                profileState.user.avatar = newAvatar;
            }
            if (tvUserIcon != null) {
                tvUserIcon.setText(newAvatar);
            }
        });
        bottomSheet.show(getChildFragmentManager(), "settings");
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
