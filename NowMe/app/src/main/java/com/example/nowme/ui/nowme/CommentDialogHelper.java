package com.example.nowme.ui.nowme;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.dto.CommentRequest;
import com.example.nowme.network.dto.CommentResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentDialogHelper {

    public interface OnCommentCreatedListener {
        void onCommentCreated(CommentResponse comment);
    }

    public static void show(
            Context context,
            Long nowmeId,
            OnCommentCreatedListener listener
    ) {
        if (context == null || nowmeId == null) return;

        EditText input = new EditText(context);

        input.setHint("Write a comment...");
        input.setSingleLine(false);
        input.setMinLines(2);
        input.setMaxLines(5);
        input.setGravity(Gravity.TOP | Gravity.START);

        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Add comment")
                .setView(input)
                .setPositiveButton("SEND", null)
                .setNegativeButton("CANCEL", null)
                .create();

        dialog.setOnShowListener(d -> {
            input.requestFocus();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                );
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String content = input.getText().toString().trim();

                if (content.isEmpty()) {
                    input.setError("Write something");
                    return;
                }

                if (content.length() > 1000) {
                    input.setError("Comment is too long");
                    return;
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

                CommentRequest request = new CommentRequest(content);

                RetrofitClient.getApi().createComment(nowmeId, request).enqueue(new Callback<CommentResponse>() {
                    @Override
                    public void onResponse(Call<CommentResponse> call, Response<CommentResponse> response) {
                        if (isHostGone(context)) return;
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);

                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(context, "Error adding comment", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (listener != null) {
                            listener.onCommentCreated(response.body());
                        }

                        dialog.dismiss();
                    }

                    @Override
                    public void onFailure(Call<CommentResponse> call, Throwable t) {
                        if (isHostGone(context)) return;
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }

    // Dismissing a dialog of a destroyed activity throws "View not attached to window manager"
    private static boolean isHostGone(Context context) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return activity.isFinishing() || activity.isDestroyed();
        }
        return false;
    }
}
