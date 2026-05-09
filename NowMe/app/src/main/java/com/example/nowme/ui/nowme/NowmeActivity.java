package com.example.nowme.ui.nowme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nowme.R;
import com.example.nowme.network.dto.NowmeResponse;
import com.example.nowme.ui.main.MainActivity;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.util.NowmeImageCache;
import com.example.nowme.util.NowmeLikeStateStore;

import java.io.Serializable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NowmeActivity extends AppCompatActivity {

    ImageButton btnClose, btnMenu, btnLike, btnComment;
    TextView tvNumLike, tvNumComment, tvUsername, tvDescription, tvDate, tvEmoji;
    ImageView imgNowMe;

    boolean liked = false;
    boolean likeRequestInFlight = false;
    boolean isOwner;
    NowmeResponse nowme;

    RecyclerView rvComments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nowme);

        btnClose = findViewById(R.id.btnClose);
        btnMenu = findViewById(R.id.btnMenu);
        btnLike = findViewById(R.id.btnLike);
        btnComment = findViewById(R.id.btnComment);

        tvNumLike = findViewById(R.id.tvNumLike);
        tvNumComment = findViewById(R.id.tvNumComment);
        tvUsername = findViewById(R.id.tvUsername);
        tvDescription = findViewById(R.id.tvDescription);
        tvDate = findViewById(R.id.tvDate);
        tvEmoji = findViewById(R.id.tvEmoji);

        imgNowMe = findViewById(R.id.imgNowMe);
        rvComments = findViewById(R.id.rvComments);

        nowme = (NowmeResponse) getIntent().getSerializableExtra("nowme");

        if (nowme == null) {
            finish();
            return;
        }

        NowmeLikeStateStore.apply(nowme);
        liked = nowme.liked != null && nowme.liked;

        btnLike.setImageResource(
                liked ? R.drawable.ic_heart : R.drawable.ic_heart_empty
        );
        btnLike.setEnabled(nowme.id != null);

        tvUsername.setText(nowme.username);
        tvDescription.setText(nowme.description != null ? nowme.description : "Sin descripción");
        tvNumLike.setText(String.valueOf(nowme.likes != null ? nowme.likes : 0));
        tvNumComment.setText(String.valueOf(nowme.comments != null ? nowme.comments : 0));
        tvEmoji.setText(nowme.userAvatar != null ? nowme.userAvatar : ":)");
        tvUsername.setOnClickListener(v -> openAuthorProfile());
        tvEmoji.setOnClickListener(v -> openAuthorProfile());

        //upgrade date
        try {
            String value = nowme.creationTime;

            java.time.LocalDate date;

            try {
                date = java.time.OffsetDateTime.parse(value).toLocalDate();
            } catch (Exception e1) {
                try {
                    date = java.time.LocalDateTime.parse(value).toLocalDate();
                } catch (Exception e2) {
                    date = java.time.LocalDate.parse(value);
                }
            }

            String formatted = date.format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            );

            tvDate.setText(formatted);

        } catch (Exception e) {
            tvDate.setText("-- / -- / --");
        }

        if (nowme.id != null) {
            NowmeImageCache.load(nowme.id, bitmap -> imgNowMe.setImageBitmap(bitmap));
        }

        btnClose.setOnClickListener(v -> finish());

        btnMenu.setOnClickListener(v ->
                Toast.makeText(this, "Menú (delete / pin)", Toast.LENGTH_SHORT).show()
        );

        btnLike.setOnClickListener(v -> {

            if (nowme.id == null || likeRequestInFlight) return;

            boolean wasLiked = liked;
            Long previousLikes = nowme.likes;
            likeRequestInFlight = true;
            btnLike.setEnabled(false);

            Call<Long> call;

            if (!wasLiked) {
                call = RetrofitClient.getApi().like(nowme.id);
            } else {
                call = RetrofitClient.getApi().unlike(nowme.id);
            }

            call.enqueue(new Callback<Long>() {
                @Override
                public void onResponse(Call<Long> call, Response<Long> response) {
                    likeRequestInFlight = false;
                    btnLike.setEnabled(nowme.id != null);

                    if (!response.isSuccessful() || response.body() == null) {
                        liked = wasLiked;
                        nowme.likes = previousLikes;
                        nowme.liked = wasLiked;
                        Toast.makeText(NowmeActivity.this, "Like error", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long newLikes = response.body();

                    liked = !wasLiked;

                    btnLike.setImageResource(
                            liked ? R.drawable.ic_heart : R.drawable.ic_heart_empty
                    );

                    tvNumLike.setText(String.valueOf(newLikes));

                    nowme.likes = newLikes;
                    nowme.liked = liked;
                    NowmeLikeStateStore.update(nowme.id, liked, newLikes);
                }

                @Override
                public void onFailure(Call<Long> call, Throwable t) {
                    likeRequestInFlight = false;
                    btnLike.setEnabled(nowme.id != null);
                    liked = wasLiked;
                    nowme.likes = previousLikes;
                    nowme.liked = wasLiked;
                    Toast.makeText(NowmeActivity.this, "Like error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnComment.setOnClickListener(v ->
                Toast.makeText(this, "Add comment", Toast.LENGTH_SHORT).show()
        );
    }

    private void openAuthorProfile() {
        if (nowme.userId == null) return;

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_PROFILE_USER_ID, nowme.userId);
        startActivity(intent);
        finish();
    }
}
