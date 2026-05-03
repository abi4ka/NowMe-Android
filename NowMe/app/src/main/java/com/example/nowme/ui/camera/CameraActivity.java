package com.example.nowme.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Surface;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.camera.core.AspectRatio;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nowme.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;

public class CameraActivity extends AppCompatActivity {

    PreviewView previewView;
    ImageCapture imageCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.cameraPreview);
        FrameLayout cameraContainer = findViewById(R.id.cameraContainer);

        cameraContainer.post(() -> {
            int width = cameraContainer.getWidth();
            float ratio = 3f / 4f;
            int height = (int) (width / ratio);
            cameraContainer.getLayoutParams().height = height;
            cameraContainer.requestLayout();
        });

        ImageButton captureButton = findViewById(R.id.captureButton);
        ImageButton galleryButton = findViewById(R.id.galleryButton);
        ImageButton closeButton = findViewById(R.id.closeButton);
        TextView cameraTitle = findViewById(R.id.cameraTitle);
        closeButton.bringToFront();
        cameraTitle.bringToFront();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cameraRoot), (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            closeButton.setY(statusBars.top + 4f);
            cameraTitle.setY(statusBars.top + 16f);
            return insets;
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        captureButton.setOnClickListener(v -> takePhoto());
        galleryButton.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        closeButton.setOnClickListener(v -> closeWithSlideDown());
    }

    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {

            try {

                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                int rotation = previewView.getDisplay() != null
                        ? previewView.getDisplay().getRotation()
                        : Surface.ROTATION_0;

                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(rotation)
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(rotation)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture
                );

            } catch (Exception e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {

        if (imageCapture == null) return;

        if (previewView.getDisplay() != null) {
            imageCapture.setTargetRotation(previewView.getDisplay().getRotation());
        }

        File photoFile = new File(
                getCacheDir(),
                "photo_" + System.currentTimeMillis() + ".jpg"
        );

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {

                        Uri uri = Uri.fromFile(photoFile);
                        goToPublishActivity(uri);

                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        exception.printStackTrace();
                    }
                }
        );
    }

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {

                if (uri != null) {
                    goToPublishActivity(uri);
                }

            });

    private void goToPublishActivity(Uri uri) {

        Intent intent = new Intent(this, PublishActivity.class);
        intent.putExtra("imageUri", uri.toString());

        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.stay, R.anim.stay);

    }

    @Override
    public void onBackPressed() {
        closeWithSlideDown();
    }

    private void closeWithSlideDown() {
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom);
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {

                if (granted) startCamera();

            });
}
