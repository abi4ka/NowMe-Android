package com.example.nowme;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.NowmeDto;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private FeedAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerFeed);

        int initialPaddingLeft = recyclerView.getPaddingLeft();
        int initialPaddingTop = recyclerView.getPaddingTop();
        int initialPaddingRight = recyclerView.getPaddingRight();
        int initialPaddingBottom = recyclerView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    initialPaddingLeft,
                    initialPaddingTop + systemBars.top,
                    initialPaddingRight,
                    initialPaddingBottom + systemBars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(recyclerView);

        adapter = new FeedAdapter();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadFeed();

        return view;
    }

    private void loadFeed() {


        Call<PageResponse<NowmeDto>> call =
                RetrofitClient.getApi().getNowmes();

        call.enqueue(new Callback<PageResponse<NowmeDto>>() {

            @Override
            public void onResponse(Call<PageResponse<NowmeDto>> call,
                                   Response<PageResponse<NowmeDto>> response) {

                if (response.isSuccessful() && response.body() != null) {

                    List<NowmeDto> list = response.body().content;

                    adapter.setItems(list);
                }
            }

            @Override
            public void onFailure(Call<PageResponse<NowmeDto>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
