package com.example.nowme;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class NowmeActivity extends AppCompatActivity {

    ImageButton btnClose, btnMenu, btnLike, btnComment;
    TextView tvNumLike, tvNumComment;
    int likes;
    boolean liked = false;
    boolean isOwner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nowme);

        btnClose = findViewById(R.id.btnClose);
        btnMenu = findViewById(R.id.btnMenu);
        btnLike = findViewById(R.id.btnLike);
        btnComment = findViewById(R.id.btnComment);
        tvNumLike = findViewById(R.id.tvNumLike);
        tvNumComment = findViewById(R.id.tvNumComment);

        likes = getIntent().getIntExtra("likes", 0);
        liked = getIntent().getBooleanExtra("liked", false);
        isOwner = getIntent().getBooleanExtra("isOwner", false);

        btnClose.setOnClickListener(v -> finish());

        btnMenu.setOnClickListener(v -> {
            Toast.makeText(this, "Menú (delete / pin)", Toast.LENGTH_SHORT).show();
        });

        if (isOwner) {
            tvNumLike.setVisibility(TextView.VISIBLE);
            tvNumLike.setText(String.valueOf(likes));
        } else {
            tvNumLike.setVisibility(TextView.GONE);
        }

        btnLike.setOnClickListener(v -> {

            liked = !liked;

            btnLike.setImageResource(liked ? R.drawable.ic_heart : R.drawable.ic_heart_empty);

            int likes = Integer.parseInt(tvNumLike.getText().toString());

            if (liked) {
                likes++;
            } else {
                likes--;
            }

            tvNumLike.setText(String.valueOf(likes));
        });

        btnComment.setOnClickListener(v -> {
            Toast.makeText(this, "Abrir comentarios", Toast.LENGTH_SHORT).show();

            // abrir teclado
            //hacer scroll al RecyclerView
        });
    }
}