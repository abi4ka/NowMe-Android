package com.example.nowme.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.example.nowme.R;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.TokenStorage;

/**
 * Utility for loading and caching NowMe post images with automatic JWT authorization,
 * lifecycle management, cross-fade transitions, and disk/memory caching powered by Glide.
 */
public final class NowmeImageLoader {

    private NowmeImageLoader() {
    }

    public static String getImageUrl(Long nowmeId) {
        if (nowmeId == null) return null;
        return RetrofitClient.BASE_URL + "nowme/" + nowmeId + "/image";
    }

    public static GlideUrl buildGlideUrl(Long nowmeId) {
        String url = getImageUrl(nowmeId);
        if (url == null) return null;
        return new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("Authorization", () -> {
                    String token = TokenStorage.getAccess();
                    return (token != null && !token.isEmpty()) ? "Bearer " + token : "";
                })
                .build());
    }

    public static void loadNowmeImage(ImageView imageView, Long nowmeId) {
        loadNowmeImage(imageView, nowmeId, R.drawable.ic_launcher_background, null);
    }

    public static void loadNowmeImage(ImageView imageView, Long nowmeId, @DrawableRes int placeholderResId) {
        loadNowmeImage(imageView, nowmeId, placeholderResId, null);
    }

    public static void loadNowmeImage(
            ImageView imageView,
            Long nowmeId,
            @DrawableRes int placeholderResId,
            @Nullable RequestListener<Drawable> listener
    ) {
        if (imageView == null) return;

        Context context = imageView.getContext();
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return;
            }
        }

        if (nowmeId == null) {
            Glide.with(imageView).clear(imageView);
            imageView.setImageResource(placeholderResId);
            return;
        }

        GlideUrl glideUrl = buildGlideUrl(nowmeId);
        RequestBuilder<Drawable> request = Glide.with(imageView)
                .load(glideUrl)
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(DrawableTransitionOptions.withCrossFade());

        if (listener != null) {
            request = request.listener(listener);
        }

        request.into(imageView);
    }

    public static void clear(ImageView imageView) {
        if (imageView == null) return;
        try {
            Glide.with(imageView).clear(imageView);
        } catch (IllegalArgumentException ignored) {
            // Context might already be destroyed
        }
    }
}
