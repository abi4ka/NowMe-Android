package com.example.nowme.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nowme.R;
import com.example.nowme.network.RetrofitClient;
import com.example.nowme.network.TokenStorage;
import com.example.nowme.network.dto.UpdateAvatarRequest;
import com.example.nowme.ui.auth.AuthActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsBottomSheetFragment extends BottomSheetDialogFragment {

    private String avatar;
    private OnAvatarUpdatedListener listener;
    private TextView tvUserAvatar;

    public interface OnAvatarUpdatedListener {
        void onAvatarUpdated(String newAvatar);
    }

    public static SettingsBottomSheetFragment newInstance(String avatar, OnAvatarUpdatedListener listener) {
        SettingsBottomSheetFragment fragment = new SettingsBottomSheetFragment();
        fragment.avatar = avatar;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_settings, container, false);

        tvUserAvatar = view.findViewById(R.id.tvUserAvatar);
        if (avatar != null && tvUserAvatar != null) {
            tvUserAvatar.setText(avatar);
        }

        View btnBack = view.findViewById(R.id.btnBackSettings);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> dismiss());
        }

        View btnEditAvatar = view.findViewById(R.id.btnEditAvatar);
        if (btnEditAvatar != null) {
            btnEditAvatar.setOnClickListener(v -> showEditAvatarDialog());
        }

        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logout());
        }

        return view;
    }

    private void showEditAvatarDialog() {
        EditText input = new EditText(requireContext());
        input.setHint(R.string.new_emoji);
        input.setText("");
        input.setTextSize(28);
        input.setSingleLine(true);
        input.setGravity(android.view.Gravity.CENTER);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_avatar)
                .setMessage("Current: " + (avatar != null ? avatar : ""))
                .setView(input)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            input.requestFocus();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(
                        android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                );
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newAvatar = input.getText().toString().trim();
                if (newAvatar.isEmpty()) {
                    input.setError(getString(R.string.write_an_emoji));
                    return;
                }
                updateAvatar(newAvatar, dialog);
            });
        });

        dialog.show();
    }

    private void updateAvatar(String newAvatar, AlertDialog dialog) {
        UpdateAvatarRequest request = new UpdateAvatarRequest(newAvatar);
        RetrofitClient.getApi().updateAvatar(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    avatar = newAvatar;
                    if (tvUserAvatar != null) {
                        tvUserAvatar.setText(newAvatar);
                    }
                    if (listener != null) {
                        listener.onAvatarUpdated(newAvatar);
                    }
                    Toast.makeText(requireContext(), R.string.avatar_updated, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(requireContext(), R.string.error_updating_avatar, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        if (getContext() == null) return;
        TokenStorage.clear(requireContext());
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        dismiss();
    }
}
