package com.example.nowme.ui.camera;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nowme.R;
import com.example.nowme.network.ApiErrors;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.util.AppExecutors;
import com.example.nowme.util.GallerySaver;
import com.example.nowme.util.ImageOrientationUtils;
import com.example.nowme.util.NowmeFeedInvalidationStore;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PublishActivity extends AppCompatActivity {

    ImageView imagePreview;
    EditText descriptionInput;
    TextView publishButton;

    public static final String EXTRA_IMAGE_URI = "imageUri";
    public static final String EXTRA_FROM_CAMERA = "fromCamera";

    Uri imageUri;
    boolean fromCamera;
    boolean publishRequestInFlight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        imagePreview = findViewById(R.id.imagePreview);
        descriptionInput = findViewById(R.id.descriptionInput);
        publishButton = findViewById(R.id.publishButton);
        ImageButton closeButton = findViewById(R.id.closeButton);
        TextView publishTitle = findViewById(R.id.publishTitle);

        closeButton.bringToFront();
        publishTitle.bringToFront();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.publishRoot), (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            closeButton.setY(statusBars.top + 4f);
            publishTitle.setY(statusBars.top + 16f);
            return insets;
        });

        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (uriString == null) {
            Toast.makeText(this, R.string.image_required, Toast.LENGTH_SHORT).show();
            closeWithSlideDown();
            return;
        }
        imageUri = Uri.parse(uriString);
        fromCamera = getIntent().getBooleanExtra(EXTRA_FROM_CAMERA, false);

        loadPreview();

        publishButton.setOnClickListener(v -> publishPost());
        closeButton.setOnClickListener(v -> closeWithSlideDown());
        // Back button and back gesture slide the screen down like the close button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeWithSlideDown();
            }
        });
    }

    // Decoding happens off the main thread and at screen size, never at full camera resolution
    private void loadPreview() {
        int maxEdge = Math.max(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        AppExecutors.io(() -> {
            Bitmap preview;
            try {
                preview = ImageOrientationUtils.decodeUpright(this, imageUri, maxEdge);
            } catch (IOException e) {
                preview = null;
            }
            Bitmap result = preview;
            AppExecutors.main(() -> {
                if (isFinishing() || isDestroyed()) {
                    if (result != null) result.recycle();
                    return;
                }
                if (result != null) {
                    imagePreview.setImageBitmap(result);
                } else {
                    Toast.makeText(this, R.string.file_error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void publishPost() {
        if (publishRequestInFlight) {
            return;
        }
        if (imageUri == null) {
            Toast.makeText(this, R.string.image_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String descriptionText = descriptionInput.getText().toString().trim();
        setPublishing(true);

        AppExecutors.io(() -> {
            File uploadFile;
            try {
                uploadFile = ImageOrientationUtils.writeUploadJpeg(this, imageUri, new File(getCacheDir(), "upload.jpg"));
            } catch (IOException e) {
                uploadFile = null;
            }
            File file = uploadFile;
            AppExecutors.main(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (file == null) {
                    setPublishing(false);
                    Toast.makeText(this, R.string.file_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                upload(file, descriptionText);
            });
        });
    }

    private void upload(File file, String descriptionText) {
        RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/jpeg"));
        MultipartBody.Part imagePart =
                MultipartBody.Part.createFormData("image", file.getName(), requestFile);
        RequestBody description =
                RequestBody.create(descriptionText, MediaType.parse("text/plain"));

        RetrofitClient.getApi().createNowme(imagePart, description).enqueue(new Callback<Long>() {
            @Override
            public void onResponse(Call<Long> call, Response<Long> response) {
                if (isFinishing() || isDestroyed()) return;
                setPublishing(false);

                if (!response.isSuccessful()) {
                    Toast.makeText(PublishActivity.this, errorMessage(response), Toast.LENGTH_SHORT).show();
                    return;
                }

                NowmeFeedInvalidationStore.invalidateAll();
                // Photos taken in the app only exist in the cache; gallery picks are already saved
                if (fromCamera && "file".equals(imageUri.getScheme()) && imageUri.getPath() != null) {
                    GallerySaver.saveInBackground(PublishActivity.this, new File(imageUri.getPath()));
                }
                Toast.makeText(PublishActivity.this, R.string.post_published, Toast.LENGTH_SHORT).show();
                closeWithSlideDown();
            }

            @Override
            public void onFailure(Call<Long> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                setPublishing(false);
                Toast.makeText(PublishActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int errorMessage(Response<?> response) {
        String code = ApiErrors.code(response);
        if ("FILE_TOO_LARGE".equals(code) || response.code() == 413) return R.string.file_too_large;
        if ("INVALID_IMAGE".equals(code)) return R.string.invalid_image;
        return R.string.publish_failed;
    }

    private void setPublishing(boolean publishing) {
        publishRequestInFlight = publishing;
        publishButton.setEnabled(!publishing);
        publishButton.setAlpha(publishing ? 0.5f : 1f);
    }


    private void closeWithSlideDown() {
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom);
    }
}
