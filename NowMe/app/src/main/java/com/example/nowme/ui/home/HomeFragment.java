package com.example.nowme.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private boolean refreshOnResume = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerFeed);
        adapter = new FeedAdapter(userId -> {
            Bundle args = new Bundle();
            args.putLong("userId", userId);
            Navigation.findNavController(view).navigate(R.id.profileFragment, args);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadFeed();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (refreshOnResume) {
            loadFeed();
        }
        refreshOnResume = true;
    }

    private void loadFeed() {
        Call<PageResponse<NowmeDto>> call = RetrofitClient.getApi().getNowmes();
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
