package com.example.nowme;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PublishActivity extends AppCompatActivity {

    ImageView imagePreview;
    EditText descriptionInput;
    Button publishButton;

    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        imagePreview = findViewById(R.id.imagePreview);
        descriptionInput = findViewById(R.id.descriptionInput);
        publishButton = findViewById(R.id.publishButton);

        String uriString = getIntent().getStringExtra("imageUri");
        imageUri = Uri.parse(uriString);

        imagePreview.setImageURI(imageUri);

        publishButton.setOnClickListener(v -> publishPost());
    }

    private void publishPost() {

        String description = descriptionInput.getText().toString();

        System.out.println("IMAGE: " + imageUri);
        System.out.println("TEXT: " + description);

        Toast.makeText(this, "Post published", Toast.LENGTH_SHORT).show();

        finish();
    }
}