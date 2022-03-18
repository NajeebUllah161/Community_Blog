package com.example.communityfeedapp.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.databinding.CommentSampleBinding;
import com.example.communityfeedapp.models.Comment;
import com.example.communityfeedapp.models.User;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.viewHolder> {

    Context context;
    ArrayList<Comment> list;
    String postId;
    String commentId;
    boolean isOwner;

    public CommentAdapter(Context context, ArrayList<Comment> list, String postId) {
        this.context = context;
        this.list = list;
        this.postId = postId;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.comment_sample, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {

        Comment comment = list.get(position);
        String timeOfComment = TimeAgo.using(comment.getCommentedAt());
        holder.binding.time.setText(timeOfComment);

        FirebaseDatabase.getInstance().getReference()
                .child("posts/" + postId + "/comments/" + comment.getCommentedAt())
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Comment comment = snapshot.getValue(Comment.class);
                        if (comment.isVerified()) {
                            Log.d("IsVerified", String.valueOf(comment.isVerified()));
                            holder.binding.commentCheckbox.setVisibility(View.VISIBLE);
                            holder.binding.commentCheckbox.setChecked(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        FirebaseDatabase.getInstance().getReference()
                .child("posts/" + postId + "/" + FirebaseAuth.getInstance()
                        .getCurrentUser().getUid() + "/isSolved")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        if (snapshot.getValue() == null) {
                            Log.d("Snapshot", String.valueOf(snapshot.getValue()));
                            FirebaseDatabase.getInstance().getReference()
                                    .child("posts/" + postId + "/postedBy")
                                    .addValueEventListener(new ValueEventListener() {
                                        @SuppressLint("NotifyDataSetChanged")
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                                            Log.d("Snapshot", String.valueOf(snapshot.getValue()));
                                            FirebaseAuth auth = FirebaseAuth.getInstance();
                                            if (auth.getCurrentUser().getUid().equals(snapshot.getValue()) && !auth.getCurrentUser().getUid().equals(comment.getCommentedBy())) {

                                                //Log.d("CommentedBy", comment.getCommentedBy());
                                                holder.binding.commentSample.setOnClickListener(view -> {
                                                    if (!holder.binding.commentCheckbox.isChecked()) {

                                                        holder.binding.commentCheckbox.setChecked(true);
                                                        holder.binding.commentCheckbox.setVisibility(View.VISIBLE);

                                                        FirebaseDatabase.getInstance().getReference()
                                                                .child("posts/" + postId + "/comments/" + comment.getCommentedAt())
                                                                .addValueEventListener(new ValueEventListener() {
                                                                    @SuppressLint("NotifyDataSetChanged")
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        Log.d("Key", snapshot.getKey());
                                                                        commentId = snapshot.getKey();
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                        Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });

                                                        comment.setVerified(true);
                                                        FirebaseDatabase.getInstance().getReference()
                                                                .child("posts/" + postId + "/comments")
                                                                .child(comment.getCommentedAt() + "")
                                                                .setValue(comment);

                                                        FirebaseDatabase.getInstance().getReference()
                                                                .child("posts/" + postId)
                                                                .child("/postStatus")
                                                                .child("/isSolved")
                                                                .setValue(true).addOnSuccessListener(unused -> ((Activity) context).finish())
                                                                .addOnFailureListener(e -> {
                                                                    Toast.makeText(context, "Failed to verify comment due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                });
                                                    }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {

                    }
                });

        FirebaseDatabase.getInstance()
                .getReference()
                .child("Users/" + comment.getCommentedBy()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                Picasso.get()
                        .load(user.getProfileImage())
                        .placeholder(R.drawable.placeholder)
                        .into(holder.binding.profileImgOfCommenter);
                holder.binding.commentData.setText(Html.fromHtml("<b>" + user.getName() + "</b>" + "  " + comment.getCommentBody()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class viewHolder extends RecyclerView.ViewHolder {

        CommentSampleBinding binding;

        public viewHolder(@NonNull View itemView) {
            super(itemView);
            binding = CommentSampleBinding.bind(itemView);
        }
    }
}
