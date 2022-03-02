package com.example.communityfeedapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.adapters.CommentAdapter;
import com.example.communityfeedapp.databinding.ActivityCommentBinding;
import com.example.communityfeedapp.models.Comment;
import com.example.communityfeedapp.models.Notification;
import com.example.communityfeedapp.models.Post;
import com.example.communityfeedapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

public class CommentActivity extends AppCompatActivity {

    ActivityCommentBinding binding;
    Intent intent;
    String postId, postedBy;
    FirebaseDatabase firebaseDatabase;
    FirebaseAuth auth;
    ArrayList<Comment> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivityCommentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        intent = getIntent();

        // Setting up toolbar
        setSupportActionBar(binding.toolbarCommentActivity);
        CommentActivity.this.setTitle("Comments");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseDatabase = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        postId = intent.getStringExtra("postId");
        postedBy = intent.getStringExtra("postedBy");

        firebaseDatabase.getReference()
                .child("posts/" + postId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Post post = snapshot.getValue(Post.class);
                        Picasso.get()
                                .load(post.getPostImage())
                                .placeholder(R.drawable.placeholder)
                                .into(binding.imgCommentScreen);
                        binding.descCommentScreen.setText(post.getPostDescription());
                        binding.like.setText(post.getPostLikes() + "");
                        binding.comment.setText(post.getCommentCount() + "");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CommentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        firebaseDatabase.getReference()
                .child("Users/" + postedBy)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        Picasso.get()
                                .load(user.getProfileImage())
                                .placeholder(R.drawable.placeholder)
                                .into(binding.profileImgComment);
                        binding.nameCommentScreen.setText(user.getName());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CommentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        binding.postCommentBtn.setOnClickListener(view -> {

            Comment comment = new Comment();
            comment.setCommentBody(binding.commentEt.getText().toString());
            comment.setCommentedAt(new Date().getTime());
            comment.setCommentedBy(FirebaseAuth.getInstance().getCurrentUser().getUid());

            firebaseDatabase.getReference()
                    .child("posts/" + postId + "/comments")
                    .push()
                    .setValue(comment)
                    .addOnSuccessListener(unused -> firebaseDatabase.getReference()
                            .child("posts/" + postId + "/commentCount")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    int commentCount = 0;
                                    if (snapshot.exists()) {
                                        commentCount = snapshot.getValue(Integer.class);

                                    }
                                    firebaseDatabase.getReference()
                                            .child("posts/" + postId + "/commentCount")
                                            .setValue(commentCount + 1)
                                            .addOnSuccessListener(unused1 -> {
                                                binding.commentEt.setText("");
                                                Toast.makeText(CommentActivity.this, "Commented", Toast.LENGTH_SHORT).show();

                                                Notification notification = new Notification();
                                                notification.setNotificationBy(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                                notification.setNotificaitonAt(new Date().getTime());
                                                notification.setPostId(postId);
                                                notification.setPostedBy(postedBy);
                                                notification.setNotificationType("comment");

                                                FirebaseDatabase.getInstance().getReference()
                                                        .child("notification")
                                                        .child(postedBy)
                                                        .push()
                                                        .setValue(notification);

                                            }).addOnFailureListener(e -> Toast.makeText(CommentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(CommentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }));
        });

        CommentAdapter commentAdapter = new CommentAdapter(this, list);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        binding.commentRv.setLayoutManager(linearLayoutManager);
        binding.commentRv.setAdapter(commentAdapter);

        firebaseDatabase.getReference()
                .child("posts/" + postId + "/comments")
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        list.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Comment comment = dataSnapshot.getValue(Comment.class);
                            list.add(comment);
                        }
                        commentAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CommentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}