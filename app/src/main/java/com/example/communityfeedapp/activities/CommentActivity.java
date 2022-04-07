package com.example.communityfeedapp.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
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

    //final boolean[] isOwner = new boolean[1];
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
//        postImage = intent.getStringExtra("postImage");
//        postTitle = intent.getStringExtra("postTitle");
//        postDescription = intent.getStringExtra("postDescription");
//        postLikes = intent.getStringExtra("postLikes");
//        commentCount = intent.getStringExtra("commentCount");
//        userProfileImg = intent.getStringExtra("userProfileImg");

        //setData();
        firebaseDatabase.getReference()
                .child("posts/" + postId)
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint({"SetTextI18n", "CheckResult"})
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Post post = snapshot.getValue(Post.class);
                        if (post != null) {
//                            Picasso.get()
//                                    .load(post.getPostImage())
//                                    .placeholder(R.drawable.placeholder)
//                                    .into(binding.imgCommentScreen);

                            RequestOptions requestOptions = new RequestOptions();
                            requestOptions.placeholder(R.drawable.placeholder);
                            if (!CommentActivity.this.isFinishing()) {
                                Glide.with(CommentActivity.this)
                                        .setDefaultRequestOptions(requestOptions)
                                        .load(post.getPostImage()).into(binding.imgCommentScreen);
                            }

                            binding.headerCommentScreen.setText(Html.fromHtml("<b>" + post.getPostTitle() + "</b>"));
                            binding.descCommentScreen.setText(post.getPostDescription());
                            binding.like.setText(post.getPostLikes() + "");
                            binding.comment.setText(post.getCommentCount() + "");
                        }
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

        FirebaseDatabase.getInstance().getReference()
                .child("posts/" + postId + "/likes/" + auth.getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.getValue()!=null){
                            Log.d("Snapshot", String.valueOf(snapshot.getValue()));
                            binding.like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_heart_filled, 0, 0, 0);

                        }
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
                    .child(comment.getCommentedAt() + "")
                    .setValue(comment)
                    .addOnSuccessListener(unused -> {
                            }
                    ).addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());

            firebaseDatabase.getReference()
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
                                        hideKeyboard(CommentActivity.this);


                                    }).addOnFailureListener(e -> {
                                Toast.makeText(CommentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                finish();

                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(CommentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        CommentAdapter commentAdapter = new CommentAdapter(this, list, postId);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
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

//    private void setData() {
//        Picasso.get()
//                .load(userProfileImg)
//                .placeholder(R.drawable.placeholder)
//                .into(binding.profileImgComment);
//        binding.nameCommentScreen.setText(postedBy);
//
//        RequestOptions requestOptions = new RequestOptions();
//        requestOptions.placeholder(R.drawable.placeholder);
//        Glide.with(CommentActivity.this)
//                .setDefaultRequestOptions(requestOptions)
//                .load(postImage).into(binding.imgCommentScreen);
//
//        binding.headerCommentScreen.setText(Html.fromHtml("<b>" + postTitle + "</b>"));
//        binding.descCommentScreen.setText(postDescription);
//        binding.like.setText(postLikes + "");
//        binding.comment.setText(commentCount + "");
//
//    }

//    private void setOwner(boolean b) {
//        isOwner[0] = b;
//    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}