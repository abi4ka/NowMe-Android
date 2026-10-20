package com.example.nowme.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.example.nowme.ui.main.MainActivity;
import com.example.nowme.util.NowmeFeedInvalidationStore;
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
    private View homeEmptyState;
    private TextView tvSearchEmpty;

    private Call<PageResponse<UserSearchResponse>> searchCall;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(HomeFeedViewModel.class);

        initFeed(view);
        initSearch(view);

        if (viewModel.items != null) {
            adapter.setItems(viewModel.items);
            updateFeedEmptyState(viewModel.items.isEmpty());
            restoreScrollPosition();
        }

        if (!viewModel.loaded && !viewModel.loading) {
            loadFeed(false);
        }

        return view;
    }

    private void updateFeedEmptyState(boolean isEmpty) {
        if (homeEmptyState != null) {
            homeEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private void initFeed(View view) {
        refreshLayout = view.findViewById(R.id.homeRefresh);
        refreshLayout.setOnRefreshListener(() -> loadFeed(true));

        recyclerView = view.findViewById(R.id.recyclerFeed);
        homeEmptyState = view.findViewById(R.id.homeEmptyState);

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
        tvSearchEmpty = view.findViewById(R.id.tvSearchEmpty);

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
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();

                if (query.isEmpty()) {
                    clearSearchResults();
                    return;
                }
                searchUsers(query);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void toggleSearch() {
        String query = etSearchUser.getText().toString().trim();

        if (query.isEmpty()) {
            clearSearchResults();
            etSearchUser.requestFocus();
            showKeyboard();
        } else {
            searchUsers(query);
        }
    }

    private void showSearch() {
        etSearchUser.requestFocus();
        showKeyboard();
    }

    private void hideSearch() {
        hideKeyboard();
        etSearchUser.setText("");
        clearSearchResults();
    }

    private void showKeyboard() {
        if (!isAdded()) return;

        etSearchUser.postDelayed(() -> {
            if (!isAdded()) return;

            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.showSoftInput(etSearchUser, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }, 150);
    }

    private void hideKeyboard() {
        if (!isAdded()) return;

        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null && etSearchUser != null) {
            imm.hideSoftInputFromWindow(etSearchUser.getWindowToken(), 0);
        }
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

        if (tvSearchEmpty != null) {
            tvSearchEmpty.setVisibility(View.GONE);
        }

        if (searchContainer != null) {
            searchContainer.setVisibility(View.GONE);
        }
    }

    private void searchUsers(String query) {
        if (searchCall != null) {
            searchCall.cancel();
        }

        searchCall = RetrofitClient.getApi().searchUsers(query, 0, 10);

        searchCall.enqueue(new Callback<PageResponse<UserSearchResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<UserSearchResponse>> call,
                                   Response<PageResponse<UserSearchResponse>> response) {

                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<UserSearchResponse> users = response.body().content != null
                            ? response.body().content
                            : Collections.emptyList();

                    userSearchAdapter.setUsers(users);

                    searchContainer.setVisibility(View.VISIBLE);
                    if (users.isEmpty()) {
                        recyclerSearchUsers.setVisibility(View.GONE);
                        if (tvSearchEmpty != null) tvSearchEmpty.setVisibility(View.VISIBLE);
                    } else {
                        if (tvSearchEmpty != null) tvSearchEmpty.setVisibility(View.GONE);
                        recyclerSearchUsers.setVisibility(View.VISIBLE);
                    }

                } else {
                    recyclerSearchUsers.setVisibility(View.GONE);
                    if (tvSearchEmpty != null) tvSearchEmpty.setVisibility(View.GONE);
                    searchContainer.setVisibility(View.GONE);

                    Toast.makeText(
                            requireContext(),
                            R.string.network_error,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<UserSearchResponse>> call, Throwable t) {
                if (call.isCanceled()) return;
                if (!isAdded()) return;

                recyclerSearchUsers.setVisibility(View.GONE);
                if (tvSearchEmpty != null) tvSearchEmpty.setVisibility(View.GONE);
                searchContainer.setVisibility(View.GONE);

                Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (NowmeFeedInvalidationStore.consumeInvalidated()) {
            loadFeed(true);
            return;
        }
        if (adapter != null) {
            adapter.syncStates();
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
                    updateFeedEmptyState(list.isEmpty());
                } else {
                    if (adapter == null || adapter.getItemCount() == 0) {
                        updateFeedEmptyState(true);
                    }
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.unable_to_load_feed, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<PageResponse<NowmeResponse>> call, Throwable t) {
                viewModel.loading = false;
                stopRefreshing();
                if (adapter == null || adapter.getItemCount() == 0) {
                    updateFeedEmptyState(true);
                }
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