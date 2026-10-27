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
import com.example.nowme.util.LoadMoreScrollListener;
import com.example.nowme.util.NowmeFeedInvalidationStore;
import com.example.nowme.network.dto.UserSearchResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final int FEED_PAGE_SIZE = 10;
    private static final int SEARCH_PAGE_SIZE = 10;

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
    private String searchQuery;
    private int searchNextPage;
    private boolean searchHasMore;
    private boolean searchInFlight;

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
        recyclerView.addOnScrollListener(new LoadMoreScrollListener() {
            @Override
            protected void onLoadMore() {
                loadMoreFeed();
            }
        });
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
        recyclerSearchUsers.addOnScrollListener(new LoadMoreScrollListener() {
            @Override
            protected void onLoadMore() {
                loadMoreSearchResults();
            }
        });

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
        searchInFlight = false;
        searchHasMore = false;

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

        searchQuery = query;
        searchNextPage = 1;
        searchHasMore = false;
        searchInFlight = true;
        searchCall = RetrofitClient.getApi().searchUsers(query, 0, SEARCH_PAGE_SIZE);

        searchCall.enqueue(new Callback<PageResponse<UserSearchResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<UserSearchResponse>> call,
                                   Response<PageResponse<UserSearchResponse>> response) {

                if (call != searchCall) return;
                searchInFlight = false;
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<UserSearchResponse> users = response.body().content != null
                            ? response.body().content
                            : Collections.emptyList();

                    userSearchAdapter.setUsers(users);
                    searchHasMore = response.body().hasNext();

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
                if (call.isCanceled() || call != searchCall) return;
                searchInFlight = false;
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

        Call<PageResponse<NowmeResponse>> call = RetrofitClient.getApi().getNowmes(0, FEED_PAGE_SIZE);

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

                    viewModel.items = new ArrayList<>(list);
                    viewModel.loaded = true;
                    viewModel.nextPage = 1;
                    viewModel.hasMore = response.body().hasNext();

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

    private void loadMoreFeed() {
        if (viewModel.loading || viewModel.loadingMore || !viewModel.hasMore || viewModel.items == null) return;

        viewModel.loadingMore = true;
        int page = viewModel.nextPage;
        RetrofitClient.getApi().getNowmes(page, FEED_PAGE_SIZE).enqueue(new Callback<PageResponse<NowmeResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<NowmeResponse>> call,
                                   Response<PageResponse<NowmeResponse>> response) {
                viewModel.loadingMore = false;
                // A pull-to-refresh started meanwhile; this page belongs to the old list
                if (viewModel.loading || page != viewModel.nextPage) return;
                if (!response.isSuccessful() || response.body() == null) return;

                List<NowmeResponse> more = new ArrayList<>();
                if (response.body().content != null) {
                    // New posts shift offset pages, so skip ones that are already shown
                    Set<Long> shown = new HashSet<>();
                    for (NowmeResponse item : viewModel.items) shown.add(item.id);
                    for (NowmeResponse item : response.body().content) {
                        if (item.id == null || shown.add(item.id)) more.add(item);
                    }
                }
                viewModel.items.addAll(more);
                viewModel.nextPage = page + 1;
                viewModel.hasMore = response.body().hasNext();
                if (adapter != null && isAdded()) {
                    adapter.appendItems(more);
                }
            }

            @Override
            public void onFailure(Call<PageResponse<NowmeResponse>> call, Throwable t) {
                viewModel.loadingMore = false;
            }
        });
    }

    private void loadMoreSearchResults() {
        if (!searchHasMore || searchQuery == null || searchInFlight) return;

        String query = searchQuery;
        int page = searchNextPage;
        searchInFlight = true;
        searchCall = RetrofitClient.getApi().searchUsers(query, page, SEARCH_PAGE_SIZE);
        searchCall.enqueue(new Callback<PageResponse<UserSearchResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<UserSearchResponse>> call,
                                   Response<PageResponse<UserSearchResponse>> response) {
                // A newer search replaced this request
                if (call != searchCall) return;
                searchInFlight = false;
                if (!isAdded() || !response.isSuccessful() || response.body() == null) return;

                userSearchAdapter.addUsers(response.body().content);
                searchNextPage = page + 1;
                searchHasMore = response.body().hasNext();
            }

            @Override
            public void onFailure(Call<PageResponse<UserSearchResponse>> call, Throwable t) {
                if (call == searchCall) {
                    searchInFlight = false;
                }
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
        boolean loadingMore = false;
        boolean hasMore = false;
        int nextPage = 0;
        int scrollPosition = 0;
        int scrollOffset = 0;
    }
}