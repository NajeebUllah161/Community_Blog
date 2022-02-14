package com.example.communityfeedapp.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.activities.CommentActivity;
import com.example.communityfeedapp.databinding.DashboardRvSampleBinding;
import com.example.communityfeedapp.models.Notification;
import com.example.communityfeedapp.models.Post;
import com.example.communityfeedapp.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    ArrayList<Post> postModelArrayList;
    Context context;

    public PostAdapter(ArrayList<Post> list, Context context) {
        this.postModelArrayList = list;
        this.context = context;
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
        Picasso.get()
                .load(model.getPostImage())
                .placeholder(R.drawable.placeholder)
                .into(holder.binding.postImg);

        holder.binding.like.setText(model.getPostLikes() + "");
        holder.binding.comment.setText(model.getCommentCount() + "");
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
        holder.binding.comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, CommentActivity.class);
                intent.putExtra("postId", model.getPostId());
                intent.putExtra("postedBy", model.getPostedBy());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
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