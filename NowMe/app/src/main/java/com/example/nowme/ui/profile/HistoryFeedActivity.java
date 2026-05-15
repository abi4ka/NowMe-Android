package com.example.nowme.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.nowme.R;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.NowmeResponse;
import com.example.nowme.ui.home.FeedAdapter;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFeedActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_NOWME_ID = "selectedNowmeId";

    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private SwipeRefreshLayout refreshLayout;
    private ProgressBar progress;
    private Long selectedNowmeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_history_feed);

        selectedNowmeId = getIntent().hasExtra(EXTRA_SELECTED_NOWME_ID)
                ? getIntent().getLongExtra(EXTRA_SELECTED_NOWME_ID, -1L)
                : null;
        if (selectedNowmeId != null && selectedNowmeId < 0) {
            selectedNowmeId = null;
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        progress = findViewById(R.id.progress);
        refreshLayout = findViewById(R.id.historyRefresh);
        recyclerView = findViewById(R.id.recyclerHistoryFeed);

        adapter = new FeedAdapter(null);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        refreshLayout.setOnRefreshListener(() -> loadHistory(true));
        loadHistory(false);
    }

    private void loadHistory(boolean forceRefresh) {
        if (!forceRefresh) {
            progress.setVisibility(View.VISIBLE);
        }

        RetrofitClient.getApi().getMyNowmeHistory().enqueue(new Callback<List<NowmeResponse>>() {
            @Override
            public void onResponse(Call<List<NowmeResponse>> call, Response<List<NowmeResponse>> response) {
                stopLoading();
                if (!response.isSuccessful()) {
                    Toast.makeText(HistoryFeedActivity.this, "History error", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<NowmeResponse> items = response.body() != null
                        ? response.body()
                        : Collections.emptyList();
                adapter.setItems(items);
                scrollToSelectedPost();
            }

            @Override
            public void onFailure(Call<List<NowmeResponse>> call, Throwable t) {
                stopLoading();
                Toast.makeText(HistoryFeedActivity.this, "History error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void stopLoading() {
        progress.setVisibility(View.GONE);
        refreshLayout.setRefreshing(false);
    }

    private void scrollToSelectedPost() {
        int position = adapter.getAdapterPositionForNowmeId(selectedNowmeId);
        if (position == RecyclerView.NO_POSITION) return;

        recyclerView.post(() -> {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (layoutManager != null) {
                layoutManager.scrollToPositionWithOffset(position, 0);
            }
        });
    }
}
