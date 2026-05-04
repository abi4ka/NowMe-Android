package com.example.nowme.util;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.example.nowme.network.RetrofitClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NowmeImageCache {
    private static final int MAX_MEMORY_KB = (int) (Runtime.getRuntime().maxMemory() / 1024);
    private static final int CACHE_SIZE_KB = MAX_MEMORY_KB / 8;

    private static final LruCache<Long, Bitmap> CACHE = new LruCache<Long, Bitmap>(CACHE_SIZE_KB) {
        @Override
        protected int sizeOf(Long key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
    };

    private static final Map<Long, List<ImageCallback>> PENDING = new HashMap<>();

    private NowmeImageCache() {
    }

    public static Bitmap get(Long nowmeId) {
        if (nowmeId == null) return null;
        return CACHE.get(nowmeId);
    }

    public static void load(Long nowmeId, ImageCallback callback) {
        if (nowmeId == null || callback == null) return;

        Bitmap cachedBitmap = CACHE.get(nowmeId);
        if (cachedBitmap != null) {
            callback.onImageLoaded(cachedBitmap);
            return;
        }

        synchronized (PENDING) {
            List<ImageCallback> callbacks = PENDING.get(nowmeId);
            if (callbacks != null) {
                callbacks.add(callback);
                return;
            }

            callbacks = new ArrayList<>();
            callbacks.add(callback);
            PENDING.put(nowmeId, callbacks);
        }

        RetrofitClient.getApi().getNowmeImage(nowmeId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Bitmap bitmap = null;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        bitmap = ImageOrientationUtils.decodeUprightBitmap(response.body().byteStream());
                    } catch (IOException ignored) {
                    }
                }

                if (bitmap != null) {
                    CACHE.put(nowmeId, bitmap);
                }
                notifyCallbacks(nowmeId, bitmap);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                notifyCallbacks(nowmeId, null);
            }
        });
    }

    private static void notifyCallbacks(Long nowmeId, Bitmap bitmap) {
        List<ImageCallback> callbacks;
        synchronized (PENDING) {
            callbacks = PENDING.remove(nowmeId);
        }

        if (callbacks == null || bitmap == null) return;
        for (ImageCallback callback : callbacks) {
            callback.onImageLoaded(bitmap);
        }
    }

    public interface ImageCallback {
        void onImageLoaded(Bitmap bitmap);
    }
}
