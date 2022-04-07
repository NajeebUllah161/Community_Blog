package com.example.communityfeedapp.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.adapters.FollowersAdapter;
import com.example.communityfeedapp.databinding.FragmentProfileBinding;
import com.example.communityfeedapp.models.FollowModel;
import com.example.communityfeedapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    private static final int COVER_PHOTO_REQUEST_CODE = 11;
    private static final int PROFILE_PHOTO_REQUEST_CODE = 12;
    ArrayList<FollowModel> list;
    FragmentProfileBinding binding;
    FirebaseAuth auth;
    FirebaseStorage firebaseStorage;
    FirebaseDatabase firebaseDatabase;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        auth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        list = new ArrayList<>();

        FollowersAdapter adapter = new FollowersAdapter(list, getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL,
                false);
        binding.myFriendRv.setLayoutManager(linearLayoutManager);
        binding.myFriendRv.setAdapter(adapter);

        firebaseDatabase.getReference().child("Users/" + auth.getCurrentUser().getUid() + "/followers")
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        list.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            FollowModel followModel = dataSnapshot.getValue(FollowModel.class);
                            list.add(followModel);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        // Fetch User data from firebase database
        firebaseDatabase.getReference().child("Users/" + auth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User getUser = snapshot.getValue(User.class);
                            if (getUser != null) {
                                Picasso.get()
                                        .load(getUser.getCoverPhoto())
                                        .placeholder(R.drawable.placeholder)
                                        .into(binding.coverPhoto);

                                Picasso.get()
                                        .load(getUser.getProfileImage())
                                        .placeholder(R.drawable.placeholder)
                                        .into(binding.profileImgOfUser);
                                binding.userName.setText(getUser.getName());
                                binding.profession.setText(getUser.getProfession());
                                binding.followersTv.setText(getUser.getFollowersCount() + "");
                                Log.d("User", String.valueOf(getUser.getUserPerks()));
                                binding.userPerks.setText(getUser.getUserPerks() + "");
                                binding.userPosts.setText(getUser.getTotalPosts() + "");
                                Log.d("userPosts",getUser.getTotalPosts() + "");

                            } else {
                                Toast.makeText(getContext(), "No user exists", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        binding.changeCoverPhoto.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, COVER_PHOTO_REQUEST_CODE);
        });

        binding.profileImgOfUser.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, PROFILE_PHOTO_REQUEST_CODE);
        });

        return binding.getRoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == COVER_PHOTO_REQUEST_CODE) {
                if (data.getData() != null) {
                    Uri uri = data.getData();
                    binding.coverPhoto.setImageURI(uri);


                    final StorageReference storageReference = firebaseStorage
                            .getReference()
                            .child("cover_photos/" + auth.getCurrentUser().getUid() + "/cover_photo");
                    // Log.d("CurrentUser",auth.getCurrentUser().getUid());

                    storageReference.putFile(uri)
                            .addOnSuccessListener(taskSnapshot -> {
                                Toast.makeText(getContext(),
                                        "Cover photo saved",
                                        Toast.LENGTH_SHORT).show();
                                storageReference.getDownloadUrl()
                                        .addOnSuccessListener(uri1 -> firebaseDatabase
                                                .getReference()
                                                .child("Users/" + auth.getCurrentUser().getUid() + "/coverPhoto")
                                                .setValue(uri1.toString()));
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Error uploading file : " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());

                }
            } else if (requestCode == PROFILE_PHOTO_REQUEST_CODE) {
                if (data.getData() != null) {
                    Uri uri = data.getData();
                    binding.profileImgOfUser.setImageURI(uri);

                    final StorageReference storageReference = firebaseStorage
                            .getReference()
                            .child("profile_images/" + auth.getCurrentUser().getUid() + "/profile_image");

                    storageReference.putFile(uri)
                            .addOnSuccessListener(taskSnapshot -> {
                                Toast.makeText(getContext(),
                                        "Profile image saved",
                                        Toast.LENGTH_SHORT).show();
                                storageReference.getDownloadUrl()
                                        .addOnSuccessListener(uri1 -> firebaseDatabase
                                                .getReference()
                                                .child("Users/" + auth.getCurrentUser().getUid() + "/profileImage")
                                                .setValue(uri1.toString()));
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Error uploading file : " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());

                }
            }
        }
    }
}