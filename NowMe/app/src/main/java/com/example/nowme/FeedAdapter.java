package com.example.nowme;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.NowmeDto;

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

import okhttp3.ResponseBody;
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

    public void setItems(List<NowmeDto> items) {
        rows.clear();

        String lastDateLabel = null;
        for (NowmeDto item : items) {
            String dateLabel = formatDateLabel(item.creationTime);
            if (!dateLabel.equals(lastDateLabel)) {
                rows.add(new DateHeaderRow(dateLabel));
                lastDateLabel = dateLabel;
            }
            rows.add(new PostRow(item));
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
        NowmeDto item = ((PostRow) row).item;

        if (item.userAvatar != null && !item.userAvatar.trim().isEmpty()) {
            postHolder.avatar.setText(item.userAvatar);
        } else {
            postHolder.avatar.setText(":)");
        }

        postHolder.username.setText(item.username);
        postHolder.image.setImageResource(R.drawable.ic_launcher_background);

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

        if (item.id == null) return;

        postHolder.image.setTag(item.id);

        RetrofitClient.getApi().getNowmeImage(item.id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                if (bitmap == null) return;

                Object currentTag = postHolder.image.getTag();
                if (currentTag instanceof Long && ((Long) currentTag).equals(item.id)) {
                    postHolder.image.setImageBitmap(bitmap);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // nada
            }
        });
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
        final NowmeDto item;

        PostRow(NowmeDto item) {
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

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.tvEmoji);
            username = itemView.findViewById(R.id.tvUsername);
            image = itemView.findViewById(R.id.imgNowMe);
        }
    }
}
