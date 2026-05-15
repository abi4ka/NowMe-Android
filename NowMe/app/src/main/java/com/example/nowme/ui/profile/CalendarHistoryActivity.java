package com.example.nowme.ui.profile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.nowme.R;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.NowmeResponse;
import com.example.nowme.util.NowmeImageCache;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarHistoryActivity extends AppCompatActivity {

    private static final DateTimeFormatter FALLBACK_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LinearLayout monthContainer;
    private ScrollView calendarScroll;
    private SwipeRefreshLayout refreshLayout;
    private ProgressBar progress;
    private TextView emptyView;
    private final ArrayList<NowmeResponse> posts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_calendar_history);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        calendarScroll = findViewById(R.id.calendarScroll);
        monthContainer = findViewById(R.id.monthContainer);
        refreshLayout = findViewById(R.id.calendarRefresh);
        progress = findViewById(R.id.progress);
        emptyView = findViewById(R.id.tvEmpty);

        refreshLayout.setOnRefreshListener(() -> loadHistory(true));
        loadHistory(false);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_right_to_left);
    }

    private void loadHistory(boolean forceRefresh) {
        if (!forceRefresh) {
            progress.setVisibility(View.VISIBLE);
        }

        RetrofitClient.getApi().getMyNowmeHistory().enqueue(new Callback<List<NowmeResponse>>() {
            @Override
            public void onResponse(Call<List<NowmeResponse>> call, Response<List<NowmeResponse>> response) {
                stopLoading();
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(CalendarHistoryActivity.this, "Calendar error", Toast.LENGTH_SHORT).show();
                    return;
                }

                posts.clear();
                posts.addAll(response.body());
                renderCalendar();
            }

            @Override
            public void onFailure(Call<List<NowmeResponse>> call, Throwable t) {
                stopLoading();
                Toast.makeText(CalendarHistoryActivity.this, "Calendar error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void stopLoading() {
        progress.setVisibility(View.GONE);
        refreshLayout.setRefreshing(false);
    }

    private void renderCalendar() {
        monthContainer.removeAllViews();
        emptyView.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);

        Map<LocalDate, NowmeResponse> latestPostByDate = new LinkedHashMap<>();
        TreeSet<YearMonth> months = new TreeSet<>();

        for (NowmeResponse post : posts) {
            LocalDate date = parseLocalDate(post.creationTime);
            if (date == null) continue;

            latestPostByDate.putIfAbsent(date, post);
            months.add(YearMonth.from(date));
        }

        for (YearMonth month : months) {
            addMonth(month, latestPostByDate);
        }

        scrollToBottom();
    }

    private void scrollToBottom() {
        if (calendarScroll == null) return;

        calendarScroll.post(() -> calendarScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void addMonth(YearMonth month, Map<LocalDate, NowmeResponse> latestPostByDate) {
        TextView title = new TextView(this);
        title.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear());
        title.setTextColor(ContextCompat.getColor(this, R.color.nowme_text_primary));
        title.setTextSize(20);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setPadding(dp(4), dp(18), dp(4), dp(10));
        monthContainer.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        grid.setUseDefaultMargins(false);
        monthContainer.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        addWeekHeaders(grid);

        int leadingEmptyDays = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < leadingEmptyDays; i++) {
            grid.addView(new View(this), createCellLayoutParams());
        }

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            grid.addView(createDayCell(date, latestPostByDate.get(date)), createCellLayoutParams());
        }
    }

    private void addWeekHeaders(GridLayout grid) {
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String day : days) {
            TextView label = new TextView(this);
            label.setText(day);
            label.setTextColor(ContextCompat.getColor(this, R.color.nowme_text_muted));
            label.setTextSize(12);
            label.setGravity(Gravity.CENTER);
            label.setPadding(0, 0, 0, dp(6));
            grid.addView(label, createHeaderLayoutParams());
        }
    }

    private View createDayCell(LocalDate date, NowmeResponse post) {
        FrameLayout cell = new FrameLayout(this);
        cell.setPadding(dp(2), dp(2), dp(2), dp(2));

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(ContextCompat.getColor(this, R.color.nowme_surface_soft));
        cell.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        View scrim = new View(this);
        scrim.setBackgroundColor(Color.argb(post != null ? 82 : 130, 1, 8, 22));
        cell.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView number = new TextView(this);
        number.setText(String.valueOf(date.getDayOfMonth()));
        number.setTextColor(ContextCompat.getColor(this, R.color.nowme_text_primary));
        number.setTextSize(17);
        number.setTypeface(number.getTypeface(), android.graphics.Typeface.BOLD);
        number.setGravity(Gravity.CENTER);
        cell.addView(number, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        if (post != null && post.id != null) {
            image.setTag(post.id);
            NowmeImageCache.load(post.id, bitmap -> {
                Object tag = image.getTag();
                if (tag instanceof Long && ((Long) tag).equals(post.id)) {
                    image.setImageBitmap(bitmap);
                }
            });
            cell.setForeground(ContextCompat.getDrawable(this, android.R.drawable.list_selector_background));
            cell.setOnClickListener(v -> openHistoryFeed(post.id));
        } else {
            cell.setAlpha(0.55f);
            cell.setOnClickListener(v ->
                    Toast.makeText(this, "No posts for this day", Toast.LENGTH_SHORT).show()
            );
        }

        return cell;
    }

    private GridLayout.LayoutParams createHeaderLayoutParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
        );
        params.width = 0;
        params.height = dp(24);
        return params;
    }

    private GridLayout.LayoutParams createCellLayoutParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
        );
        params.width = 0;
        params.height = dp(70);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }

    private void openHistoryFeed(Long selectedNowmeId) {
        Intent intent = new Intent(this, HistoryFeedActivity.class);
        intent.putExtra(HistoryFeedActivity.EXTRA_SELECTED_NOWME_ID, selectedNowmeId);
        startActivity(intent);
    }

    private LocalDate parseLocalDate(String creationTime) {
        if (creationTime == null || creationTime.trim().isEmpty()) return null;

        String value = creationTime.trim();

        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return Instant.parse(value).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value, FALLBACK_DATE_TIME_FORMATTER).toLocalDate();
        } catch (Exception ignored) {
        }

        return null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
