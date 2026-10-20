package com.example.nowme.ui.nowme;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nowme.PageResponse;
import com.example.nowme.R;
import com.example.nowme.network.dto.CommentResponse;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.NowmeResponse;
import com.example.nowme.network.dto.UpdateNowmeVisibilityRequest;
import com.example.nowme.ui.main.MainActivity;
import com.example.nowme.util.NowmeFeedInvalidationStore;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.util.NowmeCommentStateStore;
import com.example.nowme.util.NowmeImageCache;
import com.example.nowme.util.NowmeLikeStateStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NowmeActivity extends AppCompatActivity {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    ImageButton btnClose, btnMenu, btnLike, btnComment;
    TextView tvNumLike, tvNumComment, tvUsername, tvDescription, tvDate, tvEmoji;
    ImageView imgNowMe;

    boolean liked = false;
    boolean likeRequestInFlight = false;
    boolean deleteRequestInFlight = false;
    boolean favoriteRequestInFlight = false;
    boolean isOwner;
    NowmeResponse nowme;
    RecyclerView rvComments;
    CommentAdapter commentAdapter;

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

        commentAdapter = new CommentAdapter(comment -> {
            if (comment.userId != null) {
                openProfile(comment.userId);
            }
        });

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
        rvComments.setNestedScrollingEnabled(false);

        nowme = (NowmeResponse) getIntent().getSerializableExtra("nowme");

        if (nowme == null) {
            finish();
            return;
        }

        NowmeLikeStateStore.apply(nowme);
        NowmeCommentStateStore.apply(nowme);

        liked = nowme.liked != null && nowme.liked;
        isOwner = Boolean.TRUE.equals(nowme.owner);

        btnLike.setImageResource(
                liked ? R.drawable.ic_heart : R.drawable.ic_heart_empty
        );
        btnLike.setEnabled(nowme.id != null);

        tvUsername.setText(nowme.username);
        tvDescription.setText(nowme.description != null ? nowme.description : getString(R.string.no_description));
        tvNumLike.setText(String.valueOf(nowme.likes != null ? nowme.likes : 0));
        tvNumComment.setText(String.valueOf(nowme.comments != null ? nowme.comments : 0));
        tvEmoji.setText(nowme.userAvatar != null ? nowme.userAvatar : ":)");
        tvUsername.setOnClickListener(v -> openAuthorProfile());
        tvEmoji.setOnClickListener(v -> openAuthorProfile());

        LocalDate creationDate = parseCreationDate(nowme.creationTime);
        tvDate.setText(creationDate != null ? creationDate.format(DATE_FORMATTER) : "-- / -- / --");

        if (nowme.id != null) {
            NowmeImageCache.load(nowme.id, bitmap -> imgNowMe.setImageBitmap(bitmap));
            loadComments();
        }

        btnClose.setOnClickListener(v -> finish());

        btnMenu.setEnabled(isOwner);
        btnMenu.setAlpha(isOwner ? 1f : 0.45f);
        btnMenu.setOnClickListener(v -> {
            if (isOwner) {
                showNowmeMenu();
            }
        });

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

        btnComment.setOnClickListener(v -> showAddCommentDialog());
    }

    private void openAuthorProfile() {
        if (nowme.userId == null) return;

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_PROFILE_USER_ID, nowme.userId);
        startActivity(intent);
        finish();
    }

    private LocalDate parseCreationDate(String creationTime) {
        if (creationTime == null || creationTime.trim().isEmpty()) return null;

        try {
            return LocalDateTime.parse(creationTime.trim()).toLocalDate();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void showNowmeMenu() {
        if (nowme.id == null) return;

        View content = LayoutInflater.from(this)
                .inflate(R.layout.popup_nowme_visibility, null, false);
        TextView pinAction = content.findViewById(R.id.btnPinNowme);
        TextView visibilityAction = content.findViewById(R.id.btnVisibilityAction);
        TextView deleteAction = content.findViewById(R.id.btnDeleteNowme);
        boolean favorite = Boolean.TRUE.equals(nowme.favorite);
        String targetVisibility = "FRIENDS_ONLY".equals(nowme.visibility) ? "PUBLIC" : "FRIENDS_ONLY";
        pinAction.setText(favorite ? "Unpin" : "Pin");
        visibilityAction.setText("FRIENDS_ONLY".equals(nowme.visibility) ? "Make public" : "Make friends only");

        PopupWindow popupWindow = new PopupWindow(
                content,
                getResources().getDimensionPixelSize(R.dimen.nowme_visibility_menu_width),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(getResources().getDimension(R.dimen.nowme_space_sm));
        pinAction.setOnClickListener(v -> {
            popupWindow.dismiss();
            toggleFavorite();
        });
        visibilityAction.setOnClickListener(v -> {
            popupWindow.dismiss();
            updateVisibility(targetVisibility);
        });
        deleteAction.setOnClickListener(v -> {
            popupWindow.dismiss();
            confirmDeleteNowme();
        });
        popupWindow.showAsDropDown(btnMenu, -popupWindow.getWidth() + btnMenu.getWidth(), 0);
    }

    private void updateVisibility(String visibility) {
        if (nowme.id == null || visibility.equals(nowme.visibility)) return;

        btnMenu.setEnabled(false);
        RetrofitClient.getApi()
                .updateNowmeVisibility(nowme.id, new UpdateNowmeVisibilityRequest(visibility))
                .enqueue(new Callback<NowmeResponse>() {
                    @Override
                    public void onResponse(Call<NowmeResponse> call, Response<NowmeResponse> response) {
                        btnMenu.setEnabled(true);
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(NowmeActivity.this, "Visibility error", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        nowme.visibility = response.body().visibility;
                        Toast.makeText(NowmeActivity.this, visibilityLabel(nowme.visibility), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<NowmeResponse> call, Throwable t) {
                        btnMenu.setEnabled(true);
                        Toast.makeText(NowmeActivity.this, "Visibility error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void toggleFavorite() {
        if (nowme.id == null || favoriteRequestInFlight) return;

        favoriteRequestInFlight = true;
        btnMenu.setEnabled(false);
        RetrofitClient.getApi().toggleNowmeFavorite(nowme.id).enqueue(new Callback<NowmeResponse>() {
            @Override
            public void onResponse(Call<NowmeResponse> call, Response<NowmeResponse> response) {
                favoriteRequestInFlight = false;
                btnMenu.setEnabled(isOwner);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(NowmeActivity.this, "Pin error", Toast.LENGTH_SHORT).show();
                    return;
                }

                nowme.favorite = response.body().favorite;
                NowmeFeedInvalidationStore.invalidateProfile();
                Toast.makeText(
                        NowmeActivity.this,
                        Boolean.TRUE.equals(nowme.favorite) ? "Pinned" : "Unpinned",
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onFailure(Call<NowmeResponse> call, Throwable t) {
                favoriteRequestInFlight = false;
                btnMenu.setEnabled(isOwner);
                Toast.makeText(NowmeActivity.this, "Pin error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteNowme() {
        if (nowme.id == null || deleteRequestInFlight) return;

        View content = LayoutInflater.from(this)
                .inflate(R.layout.dialog_delete_nowme, null, false);
        TextView warning = content.findViewById(R.id.tvDeleteWarning);
        warning.setText(buildDeleteWarningText());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .create();
        content.findViewById(R.id.btnCancelDelete).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btnConfirmDelete).setOnClickListener(v -> {
            dialog.dismiss();
            deleteNowme();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private SpannableString buildDeleteWarningText() {
        String dangerText = "Delete forever";
        String message = dangerText + ", this cannot be undone.";
        SpannableString warning = new SpannableString(message);
        warning.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(this, R.color.nowme_danger)),
                0,
                dangerText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        warning.setSpan(
                new StyleSpan(android.graphics.Typeface.BOLD),
                0,
                dangerText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return warning;
    }

    private void deleteNowme() {
        if (nowme.id == null || deleteRequestInFlight) return;

        deleteRequestInFlight = true;
        btnMenu.setEnabled(false);
        RetrofitClient.getApi().deleteNowme(nowme.id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                deleteRequestInFlight = false;
                btnMenu.setEnabled(isOwner);

                if (!response.isSuccessful()) {
                    Toast.makeText(NowmeActivity.this, "Delete error", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(NowmeActivity.this, "Nowme deleted", Toast.LENGTH_SHORT).show();
                NowmeFeedInvalidationStore.invalidate();
                finish();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                deleteRequestInFlight = false;
                btnMenu.setEnabled(isOwner);
                Toast.makeText(NowmeActivity.this, "Delete error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String visibilityLabel(String visibility) {
        return "FRIENDS_ONLY".equals(visibility) ? "Friends" : "Public";
    }

    private void loadComments() {
        if (nowme == null || nowme.id == null) return;

        RetrofitClient.getApi().getComments(nowme.id, 0, 20).enqueue(new Callback<PageResponse<CommentResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<CommentResponse>> call, Response<PageResponse<CommentResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(NowmeActivity.this, "Error loading comments", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<CommentResponse> comments = response.body().content;
                commentAdapter.setItems(comments);
            }

            @Override
            public void onFailure(Call<PageResponse<CommentResponse>> call, Throwable t) {
                Toast.makeText(NowmeActivity.this, "Connection error loading comments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCommentDialog() {
        if (nowme == null || nowme.id == null) return;

        CommentDialogHelper.show(this, nowme.id, comment -> {
            if (commentAdapter != null) {
                commentAdapter.addItemAtTop(comment);
            }

            Long newCount = NowmeCommentStateStore.increase(nowme.id, nowme.comments);
            nowme.comments = newCount;

            tvNumComment.setText(String.valueOf(newCount));
        });
    }

    private void openProfile(Long userId) {
        if (userId == null) return;

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_PROFILE_USER_ID, userId);
        startActivity(intent);
        finish();
    }
}
