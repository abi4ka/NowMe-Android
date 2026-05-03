package com.example.nowme.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ImageOrientationUtils {

    private static final int JPEG_QUALITY = 92;

    private ImageOrientationUtils() {
    }

    public static File writeUprightJpeg(Context context, Uri uri, File outputFile) throws IOException {
        byte[] imageBytes = readBytes(context.getContentResolver().openInputStream(uri));
        Bitmap bitmap = decodeUprightBitmap(imageBytes);
        if (bitmap == null) {
            throw new IOException("Unable to decode image");
        }

        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)) {
                throw new IOException("Unable to encode image");
            }
        }

        bitmap.recycle();
        return outputFile;
    }

    public static Bitmap decodeUprightBitmap(InputStream inputStream) throws IOException {
        return decodeUprightBitmap(readBytes(inputStream));
    }

    private static Bitmap decodeUprightBitmap(byte[] imageBytes) throws IOException {
        int orientation = readOrientation(imageBytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (bitmap == null) return null;

        Matrix matrix = matrixForOrientation(orientation);
        if (matrix.isIdentity()) return bitmap;

        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return rotated;
    }

    private static int readOrientation(byte[] imageBytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            ExifInterface exif = new ExifInterface(inputStream);
            return exif.getAttributeInt(
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

    private static byte[] readBytes(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("Image stream is empty");
        }

        try (InputStream source = inputStream;
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = source.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }
}
