package com.example.nowme;

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

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private List<NowmeDto> items = new ArrayList<>();

    public void setItems(List<NowmeDto> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.mini_nowme, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        NowmeDto item = items.get(position);

        if (item.userAvatar != null && !item.userAvatar.trim().isEmpty()) {
            holder.avatar.setText(item.userAvatar);
        } else {
            holder.avatar.setText("🙂");
        }
        holder.username.setText(item.username);
        holder.image.setImageResource(R.drawable.ic_launcher_background);

        if (item.id == null) {
            return;
        }

        holder.image.setTag(item.id);

        RetrofitClient.getApi().getNowmeImage(item.id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                if (bitmap == null) {
                    return;
                }

                Object currentTag = holder.image.getTag();
                if (currentTag instanceof Long && ((Long) currentTag).equals(item.id)) {
                    holder.image.setImageBitmap(bitmap);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Keep placeholder when loading fails.
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        TextView avatar;
        TextView username;
        ImageView image;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.tvEmoji);
            username = itemView.findViewById(R.id.tvUsername);
            image = itemView.findViewById(R.id.imgNowMe);
        }
    }
}
