package com.example.nowme.ui.nowme;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nowme.R;
import com.example.nowme.network.dto.CommentResponse;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnCommentClickListener {
        void onCommentClick(CommentResponse comment);
    }

    private final List<CommentResponse> comments = new ArrayList<>();
    private final OnCommentClickListener listener;

    public CommentAdapter(OnCommentClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<CommentResponse> newComments) {
        comments.clear();

        if (newComments != null) {
            comments.addAll(newComments);
        }

        notifyDataSetChanged();
    }

    public void addItems(List<CommentResponse> moreComments) {
        if (moreComments == null || moreComments.isEmpty()) return;
        int start = comments.size();
        comments.addAll(moreComments);
        notifyItemRangeInserted(start, moreComments.size());
    }

    public void addItemAtTop(CommentResponse comment) {
        if (comment == null) return;

        comments.add(0, comment);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.comment, parent, false);

        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        CommentResponse comment = comments.get(position);

        holder.tvAvatar.setText(
                comment.userAvatar != null && !comment.userAvatar.trim().isEmpty()
                        ? comment.userAvatar
                        : "🐹"
        );

        holder.tvUsername.setText(
                comment.username != null ? comment.username : "user"
        );

        holder.tvContent.setText(
                comment.content != null ? comment.content : ""
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCommentClick(comment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView tvAvatar;
        TextView tvUsername;
        TextView tvContent;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);

            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvContent = itemView.findViewById(R.id.tvComment);
        }
    }
}