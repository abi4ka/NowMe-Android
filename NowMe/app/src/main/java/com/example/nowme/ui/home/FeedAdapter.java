package com.example.nowme.ui.home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nowme.R;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.NowmeResponse;
import com.example.nowme.ui.nowme.NowmeActivity;
import com.example.nowme.util.NowmeImageCache;
import com.example.nowme.util.NowmeLikeStateStore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_DATE_HEADER = 0;
    private static final int VIEW_TYPE_POST = 1;
    private static final DateTimeFormatter FALLBACK_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<RowItem> rows = new ArrayList<>();
    private final OnAuthorClickListener onAuthorClickListener;

    public FeedAdapter(OnAuthorClickListener onAuthorClickListener) {
        this.onAuthorClickListener = onAuthorClickListener;
    }

    public void setItems(List<NowmeResponse> items) {
        rows.clear();

        String lastDateLabel = null;
        for (NowmeResponse item : items) {
            NowmeLikeStateStore.apply(item);
            NowmeLikeStateStore.remember(item);
            String dateLabel = formatDateLabel(item.creationTime);
            if (!dateLabel.equals(lastDateLabel)) {
                rows.add(new DateHeaderRow(dateLabel));
                lastDateLabel = dateLabel;
            }
            rows.add(new PostRow(item));
        }

        notifyDataSetChanged();
    }

    public void syncLikeStates() {
        for (RowItem row : rows) {
            if (row instanceof PostRow) {
                NowmeLikeStateStore.apply(((PostRow) row).item);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) instanceof DateHeaderRow ? VIEW_TYPE_DATE_HEADER : VIEW_TYPE_POST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_DATE_HEADER) {
            View view = inflater.inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        }

        View view = inflater.inflate(R.layout.mini_nowme, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RowItem row = rows.get(position);

        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).title.setText(((DateHeaderRow) row).title);
            return;
        }

        PostViewHolder postHolder = (PostViewHolder) holder;
        NowmeResponse item = ((PostRow) row).item;
        NowmeLikeStateStore.apply(item);

        if (item.userAvatar != null && !item.userAvatar.trim().isEmpty()) {
            postHolder.avatar.setText(item.userAvatar);
        } else {
            postHolder.avatar.setText(":)");
        }

        postHolder.username.setText(item.username);
        postHolder.image.setImageResource(R.drawable.ic_launcher_background);
        postHolder.likeButton.setEnabled(item.id != null);
        postHolder.likeButton.setTag(item.id);
        postHolder.likeButton.setImageResource(isLiked(item) ? R.drawable.ic_heart : R.drawable.ic_heart_empty);

        View.OnClickListener authorClickListener = v -> {
            if (onAuthorClickListener != null && item.userId != null) {
                onAuthorClickListener.onAuthorClick(item.userId);
            }
        };
        postHolder.avatar.setOnClickListener(authorClickListener);
        postHolder.username.setOnClickListener(authorClickListener);

        postHolder.image.setOnClickListener(v -> {

            Context context = v.getContext();

            Intent intent = new Intent(context, NowmeActivity.class);

            intent.putExtra("nowme", item);

            context.startActivity(intent);
        });

        postHolder.likeButton.setOnClickListener(v -> toggleLike(item, postHolder.likeButton));

        if (item.id == null) return;

        postHolder.image.setTag(item.id);
        NowmeImageCache.load(item.id, bitmap -> {
            Object currentTag = postHolder.image.getTag();
            if (currentTag instanceof Long && ((Long) currentTag).equals(item.id)) {
                postHolder.image.setImageBitmap(bitmap);
            }
        });
    }

    private void toggleLike(NowmeResponse item, ImageButton likeButton) {
        if (item.id == null) return;

        boolean wasLiked = isLiked(item);
        likeButton.setEnabled(false);
        updateLikeState(item, likeButton, !wasLiked, item.likes);

        Call<Long> call = wasLiked
                ? RetrofitClient.getApi().unlike(item.id)
                : RetrofitClient.getApi().like(item.id);

        call.enqueue(new Callback<Long>() {
            @Override
            public void onResponse(Call<Long> call, Response<Long> response) {
                if (isButtonBoundToItem(likeButton, item)) {
                    likeButton.setEnabled(true);
                }

                if (!response.isSuccessful() || response.body() == null) {
                    updateLikeState(item, likeButton, wasLiked, item.likes);
                    Toast.makeText(likeButton.getContext(), "Like error", Toast.LENGTH_SHORT).show();
                    return;
                }

                updateLikeState(item, likeButton, !wasLiked, response.body());
            }

            @Override
            public void onFailure(Call<Long> call, Throwable t) {
                if (isButtonBoundToItem(likeButton, item)) {
                    likeButton.setEnabled(true);
                }
                updateLikeState(item, likeButton, wasLiked, item.likes);
                Toast.makeText(likeButton.getContext(), "Like error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isLiked(NowmeResponse item) {
        return item.liked != null && item.liked;
    }

    private void updateLikeState(NowmeResponse item, ImageButton likeButton, boolean liked, Long likes) {
        item.liked = liked;
        if (likes != null) {
            item.likes = likes;
        }
        NowmeLikeStateStore.update(item.id, liked, item.likes);
        if (isButtonBoundToItem(likeButton, item)) {
            likeButton.setImageResource(liked ? R.drawable.ic_heart : R.drawable.ic_heart_empty);
        }
    }

    private boolean isButtonBoundToItem(ImageButton likeButton, NowmeResponse item) {
        Object tag = likeButton.getTag();
        return item.id != null && item.id.equals(tag);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private String formatDateLabel(String creationTime) {
        LocalDate date = parseLocalDate(creationTime);
        if (date == null) return "Unknown date";

        return date.getDayOfMonth() + " " +
                date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private LocalDate parseLocalDate(String creationTime) {
        if (creationTime == null || creationTime.trim().isEmpty()) return null;

        String value = creationTime.trim();

        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value, FALLBACK_DATE_TIME_FORMATTER).toLocalDate();
        } catch (Exception ignored) {
        }

        return null;
    }

    private interface RowItem {
    }

    interface OnAuthorClickListener {
        void onAuthorClick(Long userId);
    }

    private static class DateHeaderRow implements RowItem {
        final String title;

        DateHeaderRow(String title) {
            this.title = title;
        }
    }

    private static class PostRow implements RowItem {
        final NowmeResponse item;

        PostRow(NowmeResponse item) {
            this.item = item;
        }
    }

    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView title;

        DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvDateHeader);
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        final TextView avatar;
        final TextView username;
        final ImageView image;
        final ImageButton likeButton;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.tvEmoji);
            username = itemView.findViewById(R.id.tvUsername);
            image = itemView.findViewById(R.id.imgNowMe);
            likeButton = itemView.findViewById(R.id.btnLike);
        }
    }
}
