package com.example.nowme;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

public class ProfilePostAdapter extends RecyclerView.Adapter<ProfilePostAdapter.ProfilePostViewHolder> {

    private final List<NowmeDto> items = new ArrayList<>();

    public void setItems(List<NowmeDto> nowmes) {
        items.clear();
        if (nowmes != null) {
            items.addAll(nowmes);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProfilePostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_post, parent, false);
        return new ProfilePostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfilePostViewHolder holder, int position) {
        NowmeDto item = items.get(position);
        float density = holder.itemView.getResources().getDisplayMetrics().density;
        int spacing = Math.round(2 * density) * 2;
        int width = holder.itemView.getResources().getDisplayMetrics().widthPixels / 3 - spacing;
        int height = width * 4 / 3;
        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        if (layoutParams.height != height) {
            layoutParams.height = height;
            holder.itemView.setLayoutParams(layoutParams);
        }

        holder.image.setImageResource(R.drawable.ic_launcher_background);
        holder.pin.setVisibility(Boolean.TRUE.equals(item.favorite) ? View.VISIBLE : View.GONE);
        holder.pin.bringToFront();
        holder.itemView.setOnClickListener(v -> openNowme(v.getContext(), item));

        if (item.id == null) return;

        holder.image.setTag(item.id);
        RetrofitClient.getApi().getNowmeImage(item.id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                if (bitmap == null) return;

                Object currentTag = holder.image.getTag();
                if (currentTag instanceof Long && ((Long) currentTag).equals(item.id)) {
                    holder.image.setImageBitmap(bitmap);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // keep placeholder
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void openNowme(Context context, NowmeDto item) {
        Intent intent = new Intent(context, NowmeActivity.class);
        intent.putExtra("nowme", item);
        context.startActivity(intent);
    }

    static class ProfilePostViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final ImageView pin;

        ProfilePostViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgProfilePost);
            pin = itemView.findViewById(R.id.imgPinned);
        }
    }
}
