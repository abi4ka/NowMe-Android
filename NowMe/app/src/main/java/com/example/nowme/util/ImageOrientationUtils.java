package com.example.nowme.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import androidx.annotation.WorkerThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Decodes photos upright (applying EXIF orientation) without ever loading the full-resolution
 * bitmap: images are subsampled while decoding and capped to a maximum edge. Must run off the
 * main thread.
 */
public final class ImageOrientationUtils {

    /** Long edge of uploaded photos; plenty for a phone screen and well under the server limit. */
    public static final int UPLOAD_MAX_EDGE = 2048;
    private static final int JPEG_QUALITY = 85;

    private ImageOrientationUtils() {
    }

    /** Writes an upright, downscaled JPEG suitable for upload. */
    @WorkerThread
    public static File writeUploadJpeg(Context context, Uri uri, File outputFile) throws IOException {
        Bitmap bitmap = decodeUpright(context, uri, UPLOAD_MAX_EDGE);
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)) {
                throw new IOException("Unable to encode image");
            }
        } finally {
            bitmap.recycle();
        }
        return outputFile;
    }

    /** Decodes an upright bitmap whose longest edge is at most {@code maxEdge} pixels. */
    @WorkerThread
    public static Bitmap decodeUpright(Context context, Uri uri, int maxEdge) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = open(context, uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Unable to decode image");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(Math.max(bounds.outWidth, bounds.outHeight), maxEdge);
        Bitmap bitmap;
        try (InputStream in = open(context, uri)) {
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }
        if (bitmap == null) {
            throw new IOException("Unable to decode image");
        }

        Matrix matrix = matrixForOrientation(readOrientation(context, uri));
        float scale = Math.min(1f, (float) maxEdge / Math.max(bitmap.getWidth(), bitmap.getHeight()));
        if (scale < 1f) {
            matrix.postScale(scale, scale);
        }
        if (matrix.isIdentity()) return bitmap;

        Bitmap transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (transformed != bitmap) {
            bitmap.recycle();
        }
        return transformed;
    }

    /** Copies the raw bytes of {@code uri} into {@code outputFile} (no decoding). */
    @WorkerThread
    public static File copyToFile(Context context, Uri uri, File outputFile) throws IOException {
        try (InputStream in = open(context, uri);
             OutputStream out = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        return outputFile;
    }

    // Largest power of two that keeps the decoded long edge >= maxEdge
    private static int sampleSize(int longEdge, int maxEdge) {
        int sampleSize = 1;
        while (longEdge / (sampleSize * 2) >= maxEdge) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private static InputStream open(Context context, Uri uri) throws IOException {
        try {
            InputStream in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                throw new IOException("Image stream is empty");
            }
            return in;
        } catch (SecurityException e) {
            throw new IOException("No permission to read image", e);
        }
    }

    private static int readOrientation(Context context, Uri uri) {
        try (InputStream in = open(context, uri)) {
            return new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
        } catch (IOException ignored) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    private static Matrix matrixForOrientation(int orientation) {
        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1f, -1f);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90f);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90f);
                break;
            default:
                break;
        }

        return matrix;
    }
}
