package com.example.nowme.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Surface;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.core.AspectRatio;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;
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
import com.example.nowme.util.AppExecutors;
import com.example.nowme.util.ImageOrientationUtils;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity {

    PreviewView previewView;
    ImageCapture imageCapture;
    ImageButton captureButton;
    private boolean cameraStarted;

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

        captureButton = findViewById(R.id.captureButton);
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
        // Back button and back gesture slide the screen down like the close button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeWithSlideDown();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coming back from the settings screen after granting the permission
        if (!cameraStarted && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    private void startCamera() {
        if (cameraStarted) return;
        cameraStarted = true;

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

        if (imageCapture == null || !captureButton.isEnabled()) return;
        // One capture at a time; a double tap used to open two publish screens
        captureButton.setEnabled(false);

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
                        goToPublishActivity(uri, true);

                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        captureButton.setEnabled(true);
                        Toast.makeText(CameraActivity.this, R.string.capture_failed, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {

                if (uri != null) {
                    copyGalleryImageToCache(uri);
                }

            });

    // The picker's permission only covers this activity, so copy the raw file (no decoding) off the main thread
    private void copyGalleryImageToCache(Uri uri) {
        File photoFile = new File(
                getCacheDir(),
                "gallery_photo_" + System.currentTimeMillis() + ".jpg"
        );
        AppExecutors.io(() -> {
            boolean copied;
            try {
                ImageOrientationUtils.copyToFile(this, uri, photoFile);
                copied = true;
            } catch (IOException e) {
                copied = false;
            }
            boolean success = copied;
            AppExecutors.main(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (success) {
                    goToPublishActivity(Uri.fromFile(photoFile), false);
                } else {
                    Toast.makeText(this, R.string.file_error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void goToPublishActivity(Uri uri, boolean fromCamera) {

        Intent intent = new Intent(this, PublishActivity.class);
        intent.putExtra(PublishActivity.EXTRA_IMAGE_URI, uri.toString());
        intent.putExtra(PublishActivity.EXTRA_FROM_CAMERA, fromCamera);

        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.stay, R.anim.stay);

    }


    private void closeWithSlideDown() {
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom);
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {

                if (granted) {
                    startCamera();
                } else {
                    showCameraPermissionDialog();
                }

            });

    // Without the camera the preview stays black; explain and offer settings or the gallery
    private void showCameraPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.camera_permission_title)
                .setMessage(R.string.camera_permission_message)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> startActivity(
                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getPackageName(), null))))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
