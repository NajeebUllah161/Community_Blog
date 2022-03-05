package com.example.communityfeedapp.edit_dialogues;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.databinding.ActivityEditPostDialogueBinding;
import com.example.communityfeedapp.models.Post;
import com.example.communityfeedapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class EditPostDialogue extends AppCompatActivity {

    ActivityEditPostDialogueBinding binding;
    Uri uri;
    Bitmap bitmapImg;
    FirebaseAuth auth;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    ProgressDialog progressDialog;
    String postImg, postTitle, postDescription;
    long timeStamp;
    boolean hasImage = false;

    public EditPostDialogue() {
        // Required empty public constructor
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditPostDialogueBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        progressDialog = new ProgressDialog(this);

        timeStamp = getIntent().getLongExtra("postTimeStampId", 0);
        postImg = getIntent().getStringExtra("postImg");
        postTitle = getIntent().getStringExtra("postTitle");
        postDescription = getIntent().getStringExtra("postDesc");

        /*
        Log.d("timeStamp", String.valueOf(timeStamp));
        Log.d("postImg", String.valueOf(postImg));
        Log.d("postTitle", String.valueOf(postTitle));
        Log.d("postDesc", String.valueOf(postDescription));
        */

        setupFunctions();
        populateData();
    }

    private void populateData() {
        if (postImg != null) {
            new DownloadImage().execute();
            //Log.d("postImg", postImg);
            //Picasso.get().load(postImg).placeholder(R.drawable.placeholder).into(binding.postImage);
        } else {
            binding.postImage.setVisibility(View.GONE);
            binding.removeImg.setVisibility(View.GONE);
        }

        String title = postTitle;
        if (title.equals("")) {
            binding.postTitle.setVisibility(View.GONE);
        } else {
            binding.postTitle.setText(Html.fromHtml("<b>" + postTitle + "</b>"));
            binding.postTitle.setVisibility(View.VISIBLE);
        }

        String description = postDescription;
        if (description.equals("")) {
            binding.postDescription.setVisibility(View.GONE);
        } else {
            binding.postDescription.setText(postDescription);
            binding.postDescription.setVisibility(View.VISIBLE);
        }

    }

    private void setupFunctions() {

        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Post Updating");
        progressDialog.setMessage("Please Wait...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseDatabase.getReference().child("Users/" + auth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            Picasso.get()
                                    .load(user.getProfileImage())
                                    .placeholder(R.drawable.placeholder)
                                    .into(binding.profileImgAddPost);
                            binding.userNameAddPost.setText(user.getName());
                            binding.userProfessionAddPost.setText(user.getProfession());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.postTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String title = binding.postTitle.getText().toString();
                String description = binding.postDescription.getText().toString();
                if (!title.isEmpty() || !description.isEmpty() || uri != null) {
                    setButtonEnabled();
                } else {
                    setButtonDisabled();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        binding.postDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String title = binding.postTitle.getText().toString();
                String description = binding.postDescription.getText().toString();
                if (!description.isEmpty() || !title.isEmpty() || uri != null) {
                    setButtonEnabled();
                } else {
                    setButtonDisabled();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        binding.addImg.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 10);
        });

        binding.removeImg.setOnClickListener(view -> {
            binding.postImage.setImageResource(0);
            hasImage = false;
            binding.removeImg.setVisibility(View.GONE);
        });

        binding.postBtn.setOnClickListener(view -> {

            progressDialog.show();

            final StorageReference storageReference = firebaseStorage.getReference().child("posts")
                    .child(auth.getCurrentUser().getUid())
                    .child(new Date().getTime() + "");

            if (uri != null) {
                //Log.d("Checkpoint", "if");
                storageReference.putFile(uri).addOnSuccessListener(taskSnapshot -> {
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        Post post = new Post();
                        post.setPostImage(uri.toString());
                        post.setPostedBy(auth.getCurrentUser().getUid());
                        post.setCreatedAt(new Date().toString());
                        post.setPostTitle(binding.postTitle.getText().toString());
                        post.setPostDescription(binding.postDescription.getText().toString());
                        post.setPostedAt(timeStamp);

                        firebaseDatabase.getReference().child("posts").child(String.valueOf(timeStamp))
                                .setValue(post).addOnSuccessListener(unused -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Post Edited Successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }).addOnFailureListener(e -> progressDialog.dismiss());
                    });
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to Edit Post", Toast.LENGTH_SHORT).show();
                });
            } else if (hasImage) {
                //Log.d("Checkpoint", "else if");
                Uri uriDownloaded = getImageUri(this, bitmapImg);
                storageReference.putFile(uriDownloaded).addOnSuccessListener(taskSnapshot -> {
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        Post post = new Post();
                        post.setPostImage(uri.toString());
                        post.setPostedBy(auth.getCurrentUser().getUid());
                        post.setCreatedAt(new Date().toString());
                        post.setPostTitle(binding.postTitle.getText().toString());
                        post.setPostDescription(binding.postDescription.getText().toString());
                        post.setPostedAt(timeStamp);

                        firebaseDatabase.getReference().child("posts").child(String.valueOf(timeStamp))
                                .setValue(post).addOnSuccessListener(unused -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Post Edited Successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }).addOnFailureListener(e -> progressDialog.dismiss());
                    });
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to Edit Post", Toast.LENGTH_SHORT).show();
                });
            } else {

                //Log.d("Checkpoint", "else");
                Post post = new Post();
                post.setPostedBy(auth.getCurrentUser().getUid());
                post.setCreatedAt(new Date().toString());
                post.setPostTitle(binding.postTitle.getText().toString());
                post.setPostDescription(binding.postDescription.getText().toString());
                post.setPostedAt(timeStamp);

                firebaseDatabase.getReference().child("posts").child(String.valueOf(timeStamp))
                        .setValue(post).addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Post Edited Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }).addOnFailureListener(e -> progressDialog.dismiss());
            }
        });

    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 10) {
                if (data.getData() != null) {
                    uri = data.getData();
                    binding.postImage.setImageURI(uri);
                    binding.postImage.setVisibility(View.VISIBLE);
                    binding.removeImg.setVisibility(View.VISIBLE);
                    setButtonEnabled();
                }
            }
        }
    }

    private void setButtonEnabled() {
        binding.postBtn.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.follow_btn_bg));
        binding.postBtn.setTextColor(this.getResources().getColor(R.color.white));
        binding.postBtn.setEnabled(true);
    }

    private void setButtonDisabled() {
        binding.postBtn.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.follow_active_btn));
        binding.postBtn.setTextColor(this.getResources().getColor(R.color.gray));
        binding.postBtn.setEnabled(false);
    }

    private void networkThread() {
        Bitmap imgBitmap = null;
        URL newUrl = null;
        try {
            newUrl = new URL(postImg);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            imgBitmap = BitmapFactory.decodeStream(newUrl.openConnection().getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap finalImgBitmap = imgBitmap;
        runOnUiThread(() -> {
            bitmapImg = finalImgBitmap;
            binding.postImage.setImageBitmap(finalImgBitmap);
            hasImage = true;
        });
    }

    @SuppressLint("StaticFieldLeak")
    class DownloadImage extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            networkThread();
            return null;
        }
    }

}