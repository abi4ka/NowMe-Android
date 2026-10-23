package com.example.nowme.util;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.nowme.R;

/**
 * Placeholder for post images that matches the app's navy surfaces.
 * The loading variant shows a soft moving shimmer; the error variant shows a centered
 * "image unavailable" icon. It has no intrinsic size, so it always fills the ImageView
 * without being scaled by centerCrop.
 */
public final class NowmeImagePlaceholder extends Drawable {

    private static final long SHIMMER_DURATION_MS = 1300L;
    private static final int SHIMMER_HIGHLIGHT = 0x14FFFFFF;
    private static final int ICON_MAX_SIZE_DP = 36;

    private final Paint backgroundPaint = new Paint();
    private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix shimmerMatrix = new Matrix();
    @Nullable
    private final Drawable icon;
    @Nullable
    private final ValueAnimator animator;
    private final float density;
    private float progress;
    private int alpha = 255;

    private NowmeImagePlaceholder(Context context, boolean shimmer, boolean showIcon) {
        density = context.getResources().getDisplayMetrics().density;
        backgroundPaint.setColor(ContextCompat.getColor(context, R.color.nowme_navy_800));

        Drawable iconDrawable = showIcon ? ContextCompat.getDrawable(context, R.drawable.ic_image_unavailable) : null;
        icon = iconDrawable != null ? iconDrawable.mutate() : null;

        if (shimmer) {
            shimmerPaint.setShader(new LinearGradient(
                    0f, 0f, 1f, 0f,
                    new int[]{0x00FFFFFF, SHIMMER_HIGHLIGHT, 0x00FFFFFF},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP));
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(SHIMMER_DURATION_MS);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> {
                if (!isHostedByView()) {
                    // Glide keeps the placeholder inside its cross-fade drawable after the photo
                    // is shown; stop there so the view isn't redrawn every frame forever.
                    a.cancel();
                    return;
                }
                progress = (float) a.getAnimatedValue();
                invalidateSelf();
            });
        } else {
            animator = null;
        }
    }

    /** Shown while the image is loading. */
    public static NowmeImagePlaceholder loading(Context context) {
        return new NowmeImagePlaceholder(context, true, false);
    }

    /** Shown when there is no image or it failed to load. */
    public static NowmeImagePlaceholder error(Context context) {
        return new NowmeImagePlaceholder(context, false, true);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        canvas.drawRect(bounds, backgroundPaint);

        if (animator != null && bounds.width() > 0) {
            // A band 60% of the width sweeps from fully off-screen left to fully off-screen right
            float bandWidth = bounds.width() * 0.6f;
            float start = bounds.left - bandWidth + (bounds.width() + bandWidth) * progress;
            shimmerMatrix.setScale(bandWidth, 1f);
            shimmerMatrix.postTranslate(start, 0f);
            shimmerPaint.getShader().setLocalMatrix(shimmerMatrix);
            canvas.drawRect(bounds, shimmerPaint);
        }

        if (icon != null) {
            int size = Math.min(Math.round(ICON_MAX_SIZE_DP * density), Math.min(bounds.width(), bounds.height()) / 3);
            int left = bounds.centerX() - size / 2;
            int top = bounds.centerY() - size / 2;
            icon.setBounds(left, top, left + size, top + size);
            icon.draw(canvas);
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (animator != null) {
            if (visible && isHostedByView() && !animator.isStarted()) {
                animator.start();
            } else if (!visible && animator.isStarted()) {
                animator.cancel();
            }
        }
        return changed;
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        backgroundPaint.setAlpha(alpha);
        shimmerPaint.setAlpha(alpha);
        if (icon != null) {
            icon.setAlpha(alpha);
        }
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        backgroundPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return alpha == 255 ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }

    private boolean isHostedByView() {
        return getCallback() instanceof View;
    }
}
