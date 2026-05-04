package com.example.nowme.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.example.nowme.network.dto.NowmeDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private HomeFeedViewModel viewModel;
    private SwipeRefreshLayout refreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(HomeFeedViewModel.class);

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

        if (viewModel.items != null) {
            adapter.setItems(viewModel.items);
            restoreScrollPosition();
        }

        if (!viewModel.loaded && !viewModel.loading) {
            loadFeed(false);
        }

        return view;
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
    }

    private void loadFeed(boolean forceRefresh) {
        if (viewModel.loading) return;
        if (!forceRefresh && viewModel.loaded) return;

        viewModel.loading = true;
        Call<PageResponse<NowmeDto>> call = RetrofitClient.getApi().getNowmes();
        call.enqueue(new Callback<PageResponse<NowmeDto>>() {
            @Override
            public void onResponse(Call<PageResponse<NowmeDto>> call,
                                   Response<PageResponse<NowmeDto>> response) {
                viewModel.loading = false;
                stopRefreshing();

                if (response.isSuccessful() && response.body() != null) {
                    List<NowmeDto> list = response.body().content;
                    viewModel.items = list;
                    viewModel.loaded = true;
                    if (adapter != null) {
                        adapter.setItems(list);
                    }
                }
            }

            @Override
            public void onFailure(Call<PageResponse<NowmeDto>> call, Throwable t) {
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
        viewModel.scrollOffset = firstItem != null ? firstItem.getTop() - recyclerView.getPaddingTop() : 0;
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
        List<NowmeDto> items;
        boolean loaded = false;
        boolean loading = false;
        int scrollPosition = 0;
        int scrollOffset = 0;
    }
}
