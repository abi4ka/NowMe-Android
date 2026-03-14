package com.example.nowme;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class CameraFragment extends Fragment {

    private ImageView imagePreview;
    private Uri imageUri;

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                if (result) {
                    imagePreview.setImageURI(imageUri);
                    uploadImage(imageUri);
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imagePreview.setImageURI(uri);
                    uploadImage(uri);
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        imagePreview = view.findViewById(R.id.imagePreview);

        Button btnCamera = view.findViewById(R.id.btnCamera);
        Button btnGallery = view.findViewById(R.id.btnGallery);

        btnCamera.setOnClickListener(v -> {

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {

                openCamera();

            } else {

                requestCameraPermission.launch(Manifest.permission.CAMERA);

            }

        });
        btnGallery.setOnClickListener(v -> openGallery());

        return view;
    }

    private void openCamera() {

        File file = new File(requireContext().getCacheDir(),
                "photo_" + System.currentTimeMillis() + ".jpg");

        imageUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                file
        );

        takePictureLauncher.launch(imageUri);
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void uploadImage(Uri uri) {
        System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    }

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {

                if (granted) {
                    openCamera();
                } else {
                    Toast.makeText(getContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
                }

            });
}