package com.example.nowme;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.NowmeDto;

import java.io.Serializable;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NowmeActivity extends AppCompatActivity {

    ImageButton btnClose, btnMenu, btnLike, btnComment;
    TextView tvNumLike, tvNumComment, tvUsername, tvDescription, tvDate, tvEmoji;
    ImageView imgNowMe;

    boolean liked = false;
    boolean isOwner;
    NowmeDto nowme;

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

        nowme = (NowmeDto) getIntent().getSerializableExtra("nowme");

        if (nowme == null) {
            finish();
            return;
        }

        liked = nowme.liked != null && nowme.liked;

        btnLike.setImageResource(
                liked ? R.drawable.ic_heart : R.drawable.ic_heart_empty
        );

        tvUsername.setText(nowme.username);
        tvDescription.setText(nowme.description != null ? nowme.description : "Sin descripción");
        tvNumLike.setText(String.valueOf(nowme.likes != null ? nowme.likes : 0));
        tvNumComment.setText(String.valueOf(nowme.comments != null ? nowme.comments : 0));
        tvEmoji.setText(nowme.userAvatar != null ? nowme.userAvatar : ":)");

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

            RetrofitClient.getApi().getNowmeImage(nowme.id)
                    .enqueue(new Callback<ResponseBody>() {

                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (!response.isSuccessful() || response.body() == null) return;

                            Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                            if (bitmap != null) {
                                imgNowMe.setImageBitmap(bitmap);
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            t.printStackTrace();
                        }
                    });
        }

        btnClose.setOnClickListener(v -> finish());

        btnMenu.setOnClickListener(v ->
                Toast.makeText(this, "Menú (delete / pin)", Toast.LENGTH_SHORT).show()
        );

        btnLike.setOnClickListener(v -> {

            if (nowme.id == null) return;

            Call<Long> call;

            if (!liked) {
                call = RetrofitClient.getApi().like(nowme.id);
            } else {
                call = RetrofitClient.getApi().unlike(nowme.id);
            }

            call.enqueue(new Callback<Long>() {
                @Override
                public void onResponse(Call<Long> call, Response<Long> response) {

                    if (!response.isSuccessful() || response.body() == null) return;

                    long newLikes = response.body();

                    liked = !liked;

                    btnLike.setImageResource(
                            liked ? R.drawable.ic_heart : R.drawable.ic_heart_empty
                    );

                    tvNumLike.setText(String.valueOf(newLikes));

                    nowme.likes = newLikes;
                }

                @Override
                public void onFailure(Call<Long> call, Throwable t) {
                    Toast.makeText(NowmeActivity.this, "Like error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnComment.setOnClickListener(v ->
                Toast.makeText(this, "Add comment", Toast.LENGTH_SHORT).show()
        );
    }
}