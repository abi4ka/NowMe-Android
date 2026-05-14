package com.example.nowme.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.nowme.PageResponse;
import com.example.nowme.R;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.NowmeResponse;
import com.example.nowme.network.dto.UserSearchResponse;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private HomeFeedViewModel viewModel;
    private SwipeRefreshLayout refreshLayout;

    private ImageButton btnSearch;
    private LinearLayout searchContainer;
    private EditText etSearchUser;
    private RecyclerView recyclerSearchUsers;
    private UserSearchAdapter userSearchAdapter;

    private Call<List<UserSearchResponse>> searchCall;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(HomeFeedViewModel.class);

        initFeed(view);
        initSearch(view);

        if (viewModel.items != null) {
            adapter.setItems(viewModel.items);
            restoreScrollPosition();
        }

        if (!viewModel.loaded && !viewModel.loading) {
            loadFeed(false);
        }

        return view;
    }

    private void initFeed(View view) {
        refreshLayout = view.findViewById(R.id.homeRefresh);
        refreshLayout.setOnRefreshListener(() -> loadFeed(true));

        recyclerView = view.findViewById(R.id.recyclerFeed);

        adapter = new FeedAdapter(userId -> {
            Bundle args = new Bundle();
            args.putLong("userId", userId);
            Navigation.findNavController(view).navigate(R.id.profileFragment, args);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void initSearch(View view) {
        btnSearch = view.findViewById(R.id.btnSearch);
        searchContainer = view.findViewById(R.id.searchContainer);
        etSearchUser = view.findViewById(R.id.etSearchUser);
        recyclerSearchUsers = view.findViewById(R.id.recyclerSearchUsers);

        userSearchAdapter = new UserSearchAdapter(userId -> {
            Bundle args = new Bundle();
            args.putLong("userId", userId);

            hideSearch();

            Navigation.findNavController(view).navigate(R.id.profileFragment, args);
        });

        recyclerSearchUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerSearchUsers.setAdapter(userSearchAdapter);

        btnSearch.setOnClickListener(v -> toggleSearch());

        etSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No necesitamos hacer nada aquí.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();

                if (query.length() < 2) {
                    clearSearchResults();
                    return;
                }

                searchUsers(query);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No necesitamos hacer nada aquí.
            }
        });
    }

    private void toggleSearch() {
        if (searchContainer.getVisibility() == View.VISIBLE) {
            hideSearch();
        } else {
            showSearch();
        }
    }

    private void showSearch() {
        searchContainer.setVisibility(View.VISIBLE);
        etSearchUser.requestFocus();
    }

    private void hideSearch() {
        searchContainer.setVisibility(View.GONE);
        etSearchUser.setText("");
        clearSearchResults();
    }

    private void clearSearchResults() {
        if (searchCall != null) {
            searchCall.cancel();
        }

        if (userSearchAdapter != null) {
            userSearchAdapter.clear();
        }

        if (recyclerSearchUsers != null) {
            recyclerSearchUsers.setVisibility(View.GONE);
        }
    }

    private void searchUsers(String query) {
        if (searchCall != null) {
            searchCall.cancel();
        }

        searchCall = RetrofitClient.getApi().searchUsers(query);

        searchCall.enqueue(new Callback<List<UserSearchResponse>>() {
            @Override
            public void onResponse(Call<List<UserSearchResponse>> call,
                                   Response<List<UserSearchResponse>> response) {

                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<UserSearchResponse> users = response.body();

                    userSearchAdapter.setUsers(users);

                    if (users.isEmpty()) {
                        recyclerSearchUsers.setVisibility(View.GONE);
                    } else {
                        recyclerSearchUsers.setVisibility(View.VISIBLE);
                    }

                } else {
                    recyclerSearchUsers.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<UserSearchResponse>> call, Throwable t) {
                if (call.isCanceled()) return;
                if (!isAdded()) return;

                recyclerSearchUsers.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Search error", Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.syncLikeStates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveScrollPosition();

        if (searchCall != null) {
            searchCall.cancel();
        }
    }

    private void loadFeed(boolean forceRefresh) {
        if (viewModel.loading) return;
        if (!forceRefresh && viewModel.loaded) return;

        viewModel.loading = true;

        Call<PageResponse<NowmeResponse>> call = RetrofitClient.getApi().getNowmes();

        call.enqueue(new Callback<PageResponse<NowmeResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<NowmeResponse>> call,
                                   Response<PageResponse<NowmeResponse>> response) {
                viewModel.loading = false;
                stopRefreshing();

                if (response.isSuccessful() && response.body() != null) {
                    List<NowmeResponse> list = response.body().content != null
                            ? response.body().content
                            : Collections.emptyList();

                    viewModel.items = list;
                    viewModel.loaded = true;

                    if (adapter != null) {
                        adapter.setItems(list);
                    }
                }
            }

            @Override
            public void onFailure(Call<PageResponse<NowmeResponse>> call, Throwable t) {
                viewModel.loading = false;
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

    private void saveScrollPosition() {
        if (recyclerView == null) return;

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return;

        int position = layoutManager.findFirstVisibleItemPosition();
        View firstItem = layoutManager.findViewByPosition(position);

        viewModel.scrollPosition = Math.max(position, 0);
        viewModel.scrollOffset = firstItem != null
                ? firstItem.getTop() - recyclerView.getPaddingTop()
                : 0;
    }

    private void restoreScrollPosition() {
        recyclerView.post(() -> {
            if (recyclerView == null) return;

            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

            if (layoutManager != null) {
                layoutManager.scrollToPositionWithOffset(
                        viewModel.scrollPosition,
                        viewModel.scrollOffset
                );
            }
        });
    }

    public static class HomeFeedViewModel extends ViewModel {
        List<NowmeResponse> items;
        boolean loaded = false;
        boolean loading = false;
        int scrollPosition = 0;
        int scrollOffset = 0;
    }
}