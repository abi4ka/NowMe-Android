package com.example.nowme.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.WorkerThread;

import com.example.nowme.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Saves photos into the shared gallery under Pictures/NowMe via MediaStore.
 * Needs no storage permission on Android 10+ because the app only adds its own images.
 */
public final class GallerySaver {

    private static final String ALBUM = Environment.DIRECTORY_PICTURES + "/NowMe";

    private GallerySaver() {
    }

    /** Copies {@code source} into the gallery in the background and toasts the result. */
    public static void saveInBackground(Context context, File source) {
        Context appContext = context.getApplicationContext();
        AppExecutors.io(() -> {
            boolean saved;
            try {
                save(appContext, source);
                saved = true;
            } catch (IOException e) {
                saved = false;
            }
            int message = saved ? R.string.saved_to_gallery : R.string.save_to_gallery_failed;
            AppExecutors.main(() -> Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show());
        });
    }

    @WorkerThread
    public static Uri save(Context context, File source) throws IOException {
        ImageType type = detectType(source);
        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "NowMe_" + System.currentTimeMillis() + type.extension);
        values.put(MediaStore.Images.Media.MIME_TYPE, type.mimeType);
        values.put(MediaStore.Images.Media.RELATIVE_PATH, ALBUM);
        // Hidden from other apps until the bytes are fully written
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
        if (uri == null) {
            throw new IOException("MediaStore insert failed");
        }

        try (InputStream in = new FileInputStream(source);
             OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) {
                throw new IOException("Cannot open gallery file");
            }
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            resolver.delete(uri, null, null);
            throw e;
        }

        ContentValues done = new ContentValues();
        done.put(MediaStore.Images.Media.IS_PENDING, 0);
        resolver.update(uri, done, null, null);
        return uri;
    }

    // Files from Glide's cache have no extension, so look at the content
    private static ImageType detectType(File file) throws IOException {
        byte[] header = new byte[12];
        int read = 0;
        try (InputStream in = new FileInputStream(file)) {
            int n;
            while (read < header.length && (n = in.read(header, read, header.length - read)) != -1) {
                read += n;
            }
        }
        if (read >= 8 && Arrays.equals(Arrays.copyOf(header, 8),
                new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
            return ImageType.PNG;
        }
        if (read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return ImageType.WEBP;
        }
        return ImageType.JPEG;
    }

    private enum ImageType {
        JPEG("image/jpeg", ".jpg"),
        PNG("image/png", ".png"),
        WEBP("image/webp", ".webp");

        final String mimeType;
        final String extension;

        ImageType(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }
    }
}
