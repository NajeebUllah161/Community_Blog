package com.example.communityfeedapp.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.activities.CommentActivity;
import com.example.communityfeedapp.databinding.DashboardRvSampleBinding;
import com.example.communityfeedapp.edit_dialogues.EditPostDialogue;
import com.example.communityfeedapp.models.Notification;
import com.example.communityfeedapp.models.Post;
import com.example.communityfeedapp.models.User;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.OnMenuItemClickListener;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    ArrayList<Post> postModelArrayList;
    Context context;
    MaterialCheckBox isChecked;
    PowerMenu powerMenu;
    Intent intent;
    private final OnMenuItemClickListener<PowerMenuItem> onMenuItemClickListener = new OnMenuItemClickListener<PowerMenuItem>() {
        @Override
        public void onItemClick(int position, PowerMenuItem item) {
            Toast.makeText(context, item.getTitle(), Toast.LENGTH_SHORT).show();
            powerMenu.setSelectedPosition(position); // change selected item
            if (item.getTitle().equals("Edit profile")) {
                context.startActivity(intent);
            }
            powerMenu.dismiss();
        }
    };
    MediaPlayer player;
    int length;

    public PostAdapter(ArrayList<Post> list, Context context, MaterialCheckBox checked) {
        this.postModelArrayList = list;
        this.context = context;
        this.isChecked = checked;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.dashboard_rv_sample, parent, false);
        return new PostViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {

        Post model = postModelArrayList.get(position);

        isChecked.setOnCheckedChangeListener((compoundButton, b) -> {
            Log.d("Checked", String.valueOf(b));
        });

        // Check to restrict user to only edit his/her post
        if (model.getPostedBy().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))
            holder.binding.verticalDotsPost.setVisibility(View.VISIBLE);

        // Setup Image from Firebase
        if (model.getPostImage() != null) {
            Picasso.get()
                    .load(model.getPostImage())
                    .placeholder(R.drawable.placeholder)
                    .into(holder.binding.postImg);
        } else {
            holder.binding.postImg.setVisibility(View.GONE);
        }

        // Setup Audio Recording
        if (model.getPostRecording() != null) {
            holder.binding.audioContainer.setVisibility(View.VISIBLE);
            holder.binding.playAudio.setOnClickListener(view -> {
                try {
                    player = new MediaPlayer();
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    player.setDataSource(model.getPostRecording());
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

        // Populate likes and comments from Firebase
        holder.binding.like.setText(model.getPostLikes() + "");
        holder.binding.comment.setText(model.getCommentCount() + "");

        // Setup post Title from Firebase
        String header = model.getPostTitle();
        if (header.equals("")) {
            holder.binding.postTitleDesign.setVisibility(View.GONE);
        } else {
            holder.binding.postTitleDesign.setText(Html.fromHtml("<b>" + model.getPostTitle() + "</b>"));
            holder.binding.postTitleDesign.setVisibility(View.VISIBLE);
        }

        // Setup post description from Firebase
        String description = model.getPostDescription();
        if (description.equals("")) {
            holder.binding.postDescriptionDesign.setVisibility(View.GONE);
        } else {
            holder.binding.postDescriptionDesign.setText(model.getPostDescription());
            holder.binding.postDescriptionDesign.setVisibility(View.VISIBLE);
        }

        FirebaseDatabase.getInstance()
                .getReference()
                .child("Users/" + model.getPostedBy()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    Picasso.get()
                            .load(user.getProfileImage())
                            .placeholder(R.drawable.placeholder)
                            .into(holder.binding.profileImageDashboard);

                    holder.binding.userNameDashboard.setText(user.getName());
                    holder.binding.aboutDbTv.setText(user.getProfession());
                } else {
                    Toast.makeText(context, "No user exists", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        FirebaseDatabase.getInstance()
                .getReference()
                .child("posts/" + model.getPostId() + "/likes/" + FirebaseAuth
                        .getInstance()
                        .getCurrentUser()
                        .getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            holder.binding.like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_heart_filled, 0, 0, 0);
                        } else {
                            holder.binding.like.setOnClickListener(view -> FirebaseDatabase.getInstance().getReference()
                                    .child("posts/" + model.getPostId() + "/likes/" + FirebaseAuth.getInstance()
                                            .getCurrentUser()
                                            .getUid()).setValue(true).addOnSuccessListener(unused -> FirebaseDatabase.getInstance().getReference()
                                            .child("posts/" + model.getPostId() + "/postLikes")
                                            .setValue(model.getPostLikes() + 1)
                                            .addOnSuccessListener(unused1 -> {
                                                holder.binding.like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_heart_filled, 0, 0, 0);
                                                Notification notification = new Notification();

                                                notification.setNotificationBy(FirebaseAuth
                                                        .getInstance()
                                                        .getCurrentUser()
                                                        .getUid());
                                                notification.setNotificaitonAt(new Date().getTime());
                                                notification.setPostId(model.getPostId());
                                                notification.setPostedBy(model.getPostedBy());
                                                notification.setNotificationType("like");

                                                FirebaseDatabase.getInstance().getReference()
                                                        .child("notification")
                                                        .child(model.getPostedBy())
                                                        .push()
                                                        .setValue(notification);
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show()))
                                    .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show()));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        holder.binding.comment.setOnClickListener(view -> {
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("postId", model.getPostId());
            intent.putExtra("postedBy", model.getPostedBy());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });

        handlePowerMenu(holder, model);

    }

    private void handlePowerMenu(PostViewHolder holder, Post model) {

        List<PowerMenuItem> list = new ArrayList<>();
        list.add(new PowerMenuItem("Edit profile"));
        list.add(new PowerMenuItem("Share"));
        list.add(new PowerMenuItem("Settings"));

        powerMenu = new PowerMenu.Builder(context)
                .addItemList(list) // list has "Novel", "Poetry", "Art"
                .setAnimation(MenuAnimation.SHOWUP_BOTTOM_RIGHT) // Animation start point (TOP | LEFT).
                .setMenuRadius(10f) // sets the corner radius.
                .setMenuShadow(10f) // sets the shadow.
                .setTextColor(ContextCompat.getColor(context, R.color.teal_700))
                .setTextGravity(Gravity.LEFT)
                .setTextTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
                .setSelectedTextColor(Color.WHITE)
                .setMenuColor(Color.WHITE)
                .setSelectedMenuColor(ContextCompat.getColor(context, R.color.black))
                .setOnMenuItemClickListener(onMenuItemClickListener)
                .build();

        holder.binding.verticalDotsPost.setOnClickListener(view -> {

            intent = new Intent(context, EditPostDialogue.class);
            intent.putExtra("postTimeStampId", model.getPostedAt());
            intent.putExtra("postImg", model.getPostImage());
            intent.putExtra("postTitle", model.getPostTitle());
            intent.putExtra("postDesc", model.getPostDescription());
            intent.putExtra("postRecording", model.getPostRecording());
            intent.putExtra("recTime",model.getRecTime());

            //Log.d("PostIdTimeStamp", String.valueOf(model.getPostedAt()));
            powerMenu.showAsDropDown(view);
            //Log.d("Equal",model.getPostedBy()+" " + FirebaseAuth.getInstance().getCurrentUser().getUid());

        });
    }

    @Override
    public int getItemCount() {
        // Log.d("Count Dashboard", String.valueOf(postModelArrayList.size()));
        return postModelArrayList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {

        DashboardRvSampleBinding binding;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            binding = DashboardRvSampleBinding.bind(itemView);

        }
    }
}
