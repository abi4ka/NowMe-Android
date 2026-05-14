package com.example.nowme.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nowme.R;
import com.example.nowme.network.dto.UserSearchResponse;

import java.util.ArrayList;
import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(Long userId);
    }

    private final List<UserSearchResponse> users = new ArrayList<>();
    private final OnUserClickListener listener;

    public UserSearchAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<UserSearchResponse> newUsers) {
        users.clear();

        if (newUsers != null) {
            users.addAll(newUsers);
        }

        notifyDataSetChanged();
    }

    public void clear() {
        users.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);

        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserSearchResponse user = users.get(position);

        holder.tvUsername.setText(user.username);

        if (user.avatar != null && !user.avatar.trim().isEmpty()) {
            holder.tvUserAvatar.setText(user.avatar);
        } else {
            holder.tvUserAvatar.setText("👤");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && user.id != null) {
                listener.onUserClick(user.id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {

        TextView tvUserAvatar;
        TextView tvUsername;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            tvUserAvatar = itemView.findViewById(R.id.tvUserAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
        }
    }
}