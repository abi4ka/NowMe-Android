package com.example.nowme.ui.camera;

import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nowme.R;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.util.ImageOrientationUtils;

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

    Uri imageUri;
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

        String uriString = getIntent().getStringExtra("imageUri");
        imageUri = Uri.parse(uriString);

        try {
            imagePreview.setImageBitmap(ImageOrientationUtils.decodeUprightBitmap(
                    getContentResolver().openInputStream(imageUri)
            ));
        } catch (IOException e) {
            imagePreview.setImageURI(imageUri);
        }

        publishButton.setOnClickListener(v -> publishPost());
        closeButton.setOnClickListener(v -> closeWithSlideDown());
    }

    private void publishPost() {
        if (publishRequestInFlight) {
            return;
        }

        String descriptionText = descriptionInput.getText().toString().trim();

        if (imageUri == null) {
            Toast.makeText(this, "Image required", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = uriToFile(imageUri);

            RequestBody requestFile =
                    RequestBody.create(file, MediaType.parse("image/*"));

            MultipartBody.Part imagePart =
                    MultipartBody.Part.createFormData("image", file.getName(), requestFile);

            RequestBody description =
                    RequestBody.create(descriptionText, MediaType.parse("text/plain"));

            publishRequestInFlight = true;
            publishButton.setEnabled(false);

            Call<Long> call = RetrofitClient.getApi()
                    .createNowme(imagePart, description);

            call.enqueue(new Callback<Long>() {

                @Override
                public void onResponse(Call<Long> call, Response<Long> response) {
                    publishRequestInFlight = false;
                    publishButton.setEnabled(true);

                    if (response.isSuccessful()) {

                        Toast.makeText(PublishActivity.this, "Post published", Toast.LENGTH_SHORT).show();
                        closeWithSlideDown();

                    } else {
                        Toast.makeText(PublishActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Long> call, Throwable t) {
                    publishRequestInFlight = false;
                    publishButton.setEnabled(true);
                    t.printStackTrace();
                    Toast.makeText(PublishActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (IOException e) {
            publishRequestInFlight = false;
            publishButton.setEnabled(true);
            e.printStackTrace();
            Toast.makeText(this, "File error", Toast.LENGTH_SHORT).show();
        }
    }

    private File uriToFile(Uri uri) throws IOException {
        File file = new File(getCacheDir(), "upload.jpg");
        return ImageOrientationUtils.writeUprightJpeg(this, uri, file);
    }

    @Override
    public void onBackPressed() {
        closeWithSlideDown();
    }

    private void closeWithSlideDown() {
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom);
    }
}
