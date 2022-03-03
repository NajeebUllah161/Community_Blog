package com.example.communityfeedapp.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.databinding.FragmentAddPostBinding;
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

import java.util.Date;

import static android.app.Activity.RESULT_OK;

public class AddPostFragment extends Fragment {

    FragmentAddPostBinding binding;
    Uri uri;
    FirebaseAuth auth;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    ProgressDialog progressDialog;

    public AddPostFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        progressDialog = new ProgressDialog(getContext());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAddPostBinding.inflate(inflater, container, false);

        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Post Uploading");
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

        binding.postHeader.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String header = binding.postHeader.getText().toString();
                String description = binding.postDescription.getText().toString();
                if (!header.isEmpty() || !description.isEmpty()) {
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
                String header = binding.postHeader.getText().toString();
                String description = binding.postDescription.getText().toString();
                if (!description.isEmpty() || !header.isEmpty()) {
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

        binding.postBtn.setOnClickListener(view -> {

            progressDialog.show();

            final StorageReference storageReference = firebaseStorage.getReference().child("posts")
                    .child(auth.getCurrentUser().getUid())
                    .child(new Date().getTime() + "");

            if (uri != null) {
                storageReference.putFile(uri).addOnSuccessListener(taskSnapshot -> {
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        Post post = new Post();
                        post.setPostImage(uri.toString());
                        post.setPostedBy(auth.getCurrentUser().getUid());
                        post.setPostHeader(binding.postHeader.getText().toString());
                        post.setPostDescription(binding.postDescription.getText().toString());
                        post.setPostedAt(new Date().getTime());

                        firebaseDatabase.getReference().child("posts")
                                .push()
                                .setValue(post).addOnSuccessListener(unused -> {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Posted Successfully", Toast.LENGTH_SHORT).show();
                            //switchFragment();
                        }).addOnFailureListener(e -> progressDialog.dismiss());
                    });
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to Post", Toast.LENGTH_SHORT).show();
                });
            } else {
                Post post = new Post();
                post.setPostedBy(auth.getCurrentUser().getUid());
                post.setPostHeader(binding.postHeader.getText().toString());
                post.setPostDescription(binding.postDescription.getText().toString());
                post.setPostedAt(new Date().getTime());

                firebaseDatabase.getReference().child("posts")
                        .push()
                        .setValue(post).addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Posted Successfully", Toast.LENGTH_SHORT).show();
                    //switchFragment();
                }).addOnFailureListener(e -> progressDialog.dismiss());
            }
        });

        return binding.getRoot();
    }

    private void switchFragment() {
        getActivity().findViewById(R.id.postBtn).setVisibility(View.GONE);
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.addPost, new HomeFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
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
                    setButtonEnabled();

                }
            }
        }
    }

    private void setButtonEnabled() {
        binding.postBtn.setBackgroundDrawable(ContextCompat.getDrawable(getContext(), R.drawable.follow_btn_bg));
        binding.postBtn.setTextColor(getContext().getResources().getColor(R.color.white));
        binding.postBtn.setEnabled(true);
    }


    private void setButtonDisabled() {
        binding.postBtn.setBackgroundDrawable(ContextCompat.getDrawable(getContext(), R.drawable.follow_active_btn));
        binding.postBtn.setTextColor(getContext().getResources().getColor(R.color.gray));
        binding.postBtn.setEnabled(false);
    }
}