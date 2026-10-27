package com.example.nowme.util;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Calls {@link #onLoadMore()} when the user scrolls close to the end of a linear list. */
public abstract class LoadMoreScrollListener extends RecyclerView.OnScrollListener {

    private static final int THRESHOLD = 3;

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (dy <= 0) return;

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) return;

        int lastVisible = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        if (lastVisible >= layoutManager.getItemCount() - 1 - THRESHOLD) {
            onLoadMore();
        }
    }

    /** Called repeatedly while near the end; implementations must ignore calls while loading. */
    protected abstract void onLoadMore();
}
