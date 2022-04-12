package com.example.communityfeedapp.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.viewHolder> {

    Context context;
    ArrayList<Comment> list;
    String postId;
    String commentId;
    MediaPlayer player;
    int length;

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

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {

        Comment comment = list.get(position);
        String timeOfComment = TimeAgo.using(comment.getCommentedAt());
        holder.binding.time.setText(timeOfComment);
        holder.binding.like.setText(comment.getLikesCount() + "");
        holder.binding.dislike.setText(comment.getDislikesCount() + "");

        // Setup Audio Recording
        if (comment.getCommentRecording() != null) {
            holder.binding.audioContainer.setVisibility(View.VISIBLE);
            holder.binding.playAudio.setOnClickListener(view -> {
                try {
                    player = new MediaPlayer();
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    player.setDataSource(comment.getCommentRecording());
                    player.prepare();
                    player.start();
                    holder.binding.playAudio.setVisibility(View.GONE);
                    holder.binding.pauseAudio.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.d("Exception", e.getMessage());
                }
                player.setOnCompletionListener(mediaPlayer -> {
                    holder.binding.playAudio.setVisibility(View.VISIBLE);
                    holder.binding.pauseAudio.setVisibility(View.GONE);
                });
            });

            // Pause recording
            holder.binding.pauseAudio.setOnClickListener(view -> {
                if (player != null) {
                    player.pause();
                    length = player.getCurrentPosition();
                    holder.binding.pauseAudio.setVisibility(View.GONE);
                    holder.binding.resumeAudio.setVisibility(View.VISIBLE);
                } else {
                    Log.d("PostAdapter", "Player is null");
                }
            });

            // Resume recording
            holder.binding.resumeAudio.setOnClickListener(view -> {
                if (player != null) {
                    holder.binding.resumeAudio.setVisibility(View.GONE);
                    holder.binding.pauseAudio.setVisibility(View.VISIBLE);
                    player.seekTo(length);
                    player.start();
                    player.setOnCompletionListener(mediaPlayer -> {
                        holder.binding.pauseAudio.setVisibility(View.GONE);
                        holder.binding.playAudio.setVisibility(View.VISIBLE);
                    });
                } else {
                    Log.d("PostAdapter", "Player is null");
                }
            });
        } else {
            holder.binding.audioContainer.setVisibility(View.GONE);
        }

        //Log.d("CommentedBy", comment.getCommentedBy());
        FirebaseDatabase.getInstance().getReference()
                .child("posts/" + postId + "/comments/" + comment.getCommentedAt())
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Comment comment = snapshot.getValue(Comment.class);
                        if (comment.isVerified()) {
                            //Log.d("IsVerified", String.valueOf(comment.isVerified()));
                            holder.binding.commentCheckbox.setVisibility(View.GONE);
                            holder.binding.verifiedImgView.setVisibility(View.VISIBLE);
                            holder.binding.commentCheckbox.setChecked(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        FirebaseDatabase.getInstance().getReference()
                .child("posts/" + postId)
                .child("/solved")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        if (snapshot.getValue().equals(false)) {
                            Log.d("SnapshotSolved", String.valueOf(snapshot.getValue()));
                            FirebaseDatabase.getInstance().getReference()
                                    .child("posts/" + postId + "/postedBy")
                                    .addValueEventListener(new ValueEventListener() {
                                        @SuppressLint("NotifyDataSetChanged")
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                                            Log.d("Snapshot", String.valueOf(snapshot.getValue()));
                                            FirebaseAuth auth = FirebaseAuth.getInstance();
                                            //&& !auth.getCurrentUser().getUid().equals(comment.getCommentedBy())
                                            if (auth.getCurrentUser().getUid().equals(snapshot.getValue())) {

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
                                                                .child("Users")
                                                                .child(comment.getCommentedBy())
                                                                .child("userPerks")
                                                                .setValue(ServerValue.increment(1)).addOnSuccessListener(unused -> {
                                                            Toast.makeText(context, "Rating updated", Toast.LENGTH_SHORT).show();
                                                        }).addOnFailureListener(e -> {
                                                            Toast.makeText(context, "Failed to update rating", Toast.LENGTH_SHORT).show();
                                                        });

                                                        FirebaseDatabase.getInstance().getReference()
                                                                .child("posts/" + postId)
                                                                .child("/solved")
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

        FirebaseDatabase.getInstance().getReference()
                .child("posts/" + postId + "/comments")
                .child(comment.getCommentedAt() + "")
                .child("likes")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            if (snapshot.getValue().equals(true)) {
                                //Log.d("Najeeb", " Exists and Liked " + snapshot);
                                fillLike(holder.binding);
                                holder.binding.dislike.setOnClickListener(view -> {

                                    Map<String, Object> likeDislikeCount = new HashMap<>();
                                    likeDislikeCount.put("dislikesCount", ServerValue.increment(1));

                                    //Map<String, Object> likesCount = new HashMap<>();
                                    likeDislikeCount.put("likesCount", ServerValue.increment(-1));

                                    FirebaseDatabase.getInstance().getReference()
                                            .child("posts/" + postId + "/comments")
                                            .child(comment.getCommentedAt() + "")
                                            .updateChildren(likeDislikeCount);

//                                    FirebaseDatabase.getInstance().getReference()
//                                            .child("posts/" + postId + "/comments")
//                                            .child(comment.getCommentedAt() + "")
//                                            .updateChildren(likesCount);

                                    FirebaseDatabase.getInstance().getReference()
                                            .child("posts/" + postId + "/comments")
                                            .child(comment.getCommentedAt() + "")
                                            .child("likes")
                                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                            .setValue(false);

                                    fillDislike(holder.binding);
                                });
                            } else {
                                Log.d("Najeeb", " Exists and Disliked " + snapshot);
                                fillDislike(holder.binding);
                                holder.binding.like.setOnClickListener(view -> {

                                    Map<String, Object> likeDislikeCount = new HashMap<>();
                                    likeDislikeCount.put("likesCount", ServerValue.increment(1));

                                    //Map<String, Object> dislikesCount = new HashMap<>();
                                    likeDislikeCount.put("dislikesCount", ServerValue.increment(-1));

                                    FirebaseDatabase.getInstance().getReference()
                                            .child("posts/" + postId + "/comments")
                                            .child(comment.getCommentedAt() + "")
                                            .updateChildren(likeDislikeCount);

//                                    FirebaseDatabase.getInstance().getReference()
//                                            .child("posts/" + postId + "/comments")
//                                            .child(comment.getCommentedAt() + "")
//                                            .updateChildren(dislikesCount);

                                    FirebaseDatabase.getInstance().getReference()
                                            .child("posts/" + postId + "/comments")
                                            .child(comment.getCommentedAt() + "")
                                            .child("likes")
                                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                            .setValue(true);

                                    fillLike(holder.binding);
                                });
                            }
                        } else {

                            holder.binding.like.setOnClickListener(view -> {

                                Map<String, Object> commentLikesDislikes = new HashMap<>();
                                commentLikesDislikes.put("likesCount", ServerValue.increment(1));

//                                Map<String, Object> dislikesCount = new HashMap<>();
                                commentLikesDislikes.put("dislikesCount", ServerValue.increment(-1));

                                FirebaseDatabase.getInstance().getReference()
                                        .child("posts/" + postId + "/comments")
                                        .child(comment.getCommentedAt() + "")
                                        .updateChildren(commentLikesDislikes);

//                                FirebaseDatabase.getInstance().getReference()
//                                        .child("posts/" + postId + "/comments")
//                                        .child(comment.getCommentedAt() + "")
//                                        .updateChildren(dislikesCount);

                                FirebaseDatabase.getInstance().getReference()
                                        .child("posts/" + postId + "/comments")
                                        .child(comment.getCommentedAt() + "")
                                        .child("likes")
                                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(true);

                                fillLike(holder.binding);
                            });
                            holder.binding.dislike.setOnClickListener(view -> {

                                Map<String, Object> dislikesCount = new HashMap<>();
                                dislikesCount.put("dislikesCount", ServerValue.increment(1));

//                                Map<String, Object> likesCount = new HashMap<>();
//                                dislikesCount.put("likesCount", ServerValue.increment(-1));

                                FirebaseDatabase.getInstance().getReference()
                                        .child("posts/" + postId + "/comments")
                                        .child(comment.getCommentedAt() + "")
                                        .updateChildren(dislikesCount);

//                                FirebaseDatabase.getInstance().getReference()
//                                        .child("posts/" + postId + "/comments")
//                                        .child(comment.getCommentedAt() + "")
//                                        .updateChildren(likesCount);

                                FirebaseDatabase.getInstance().getReference()
                                        .child("posts/" + postId + "/comments")
                                        .child(comment.getCommentedAt() + "")
                                        .child("likes")
                                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(false);

                                fillDislike(holder.binding);
                            });
                            Log.d("Najeeb", "Doesn't exist" + snapshot);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {

                    }
                });

        holder.binding.verifiedImgView.setOnClickListener(view -> new SimpleTooltip.Builder(context)
                .anchorView(view)
                .text("Verified")
                .gravity(Gravity.TOP)
                .backgroundColor(Color.parseColor("#FF018786"))
                .arrowColor(Color.parseColor("#FF018786"))
                .animated(true)
                .transparentOverlay(false)
                .build()
                .show());

    }

    private void fillDislike(CommentSampleBinding commentSampleBinding) {
        commentSampleBinding.dislike.setEnabled(false);
        commentSampleBinding.like.setEnabled(true);
        commentSampleBinding.dislike.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_comment_dislike_filled, 0, 0, 0);
        commentSampleBinding.like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_commentlike, 0, 0, 0);
    }

    private void fillLike(CommentSampleBinding commentSampleBinding) {
        commentSampleBinding.like.setEnabled(false);
        commentSampleBinding.dislike.setEnabled(true);
        commentSampleBinding.like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_comment_like_filled, 0, 0, 0);
        commentSampleBinding.dislike.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_commentdislike, 0, 0, 0);
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
