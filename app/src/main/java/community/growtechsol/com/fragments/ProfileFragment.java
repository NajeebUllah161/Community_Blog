package community.growtechsol.com.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.flatdialoglibrary.dialog.FlatDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import community.growtechsol.com.R;
import community.growtechsol.com.activities.PostImageZoomActivity;
import community.growtechsol.com.adapters.FollowersAdapter;
import community.growtechsol.com.adapters.FollowingAdapter;
import community.growtechsol.com.databinding.FragmentProfileBinding;
import community.growtechsol.com.models.FollowModel;
import community.growtechsol.com.models.Following;
import community.growtechsol.com.models.User;
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    private static final int COVER_PHOTO_REQUEST_CODE = 11;
    private static final int PROFILE_PHOTO_REQUEST_CODE = 12;
    ArrayList<FollowModel> list;
    ArrayList<Following> followingList;
    FragmentProfileBinding binding;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    FirebaseAuth auth;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        firebaseDatabase = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        setupFunctions();

        return binding.getRoot();
    }

    private void setupFunctions() {

        setFollowers();
        setFollowing();
        setupUserData();

    }

    private void setupUserData() {
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
                                binding.userPerks.setText(getUser.getUserPerks() + "");
                                binding.userPosts.setText(getUser.getTotalPosts() + "");
                                if (getUser.getFollowersCount() == 0) {
                                    binding.textView12.setVisibility(View.INVISIBLE);
                                    binding.followersCount.setVisibility(View.GONE);
                                    binding.myFriendRv.setVisibility(View.GONE);
                                } else {
                                    binding.followersCount.setText(" (" + getUser.getFollowersCount() + ")");
                                }
                                if (getUser.getFollowingCount() == 0) {
                                    binding.textView13.setVisibility(View.GONE);
                                    binding.followingCount.setVisibility(View.GONE);
                                    binding.followingRv.setVisibility(View.GONE);
                                } else {
                                    binding.followingCount.setText(" (" + getUser.getFollowingCount() + ")");
                                }
                                if(getUser.getFollowersCount() == 0 && getUser.getFollowingCount() == 0){
                                 binding.placeholderTxt.setVisibility(View.VISIBLE);
                                }
                                if (getUser.isAdmin()) {
                                    setupVerificationTick();
                                    binding.adminLikes.setText(getUser.getUserUpVotes() + "");
                                    binding.adminDislikes.setText(getUser.getUserDownVotes() + "");
                                } else {
                                    binding.popularityView.setVisibility(View.GONE);
                                    binding.popularitySvg.setVisibility(View.GONE);
                                    binding.view10.setVisibility(View.GONE);
                                    binding.textView8.setVisibility(View.GONE);
                                    binding.adminLikes.setVisibility(View.GONE);
                                    binding.adminDislikes.setVisibility(View.GONE);
                                    binding.slash.setVisibility(View.GONE);
                                    setConstraints();
                                }
                                setupImgClickListeners(getUser);

                            } else {
                                Toast.makeText(getContext(), "No user exists", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void setFollowers() {

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
                        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupImgClickListeners(User getUser) {

        binding.profileImgOfUser.setOnClickListener(v -> {

            final FlatDialog flatDialog = new FlatDialog(getContext());
            flatDialog.setTitle("Select from gallery/View profile picture")
                    .setFirstButtonText("Select from gallery")
                    .setSecondButtonText("View profile image")
                    .setThirdButtonText("Cancel")
                    .isCancelable(true)
                    .setBackgroundColor(Color.parseColor("#79018786"))
                    .setFirstButtonColor(Color.parseColor("#FF018786"))
                    .setSecondButtonColor(Color.parseColor("#FF018786"))
                    .setThirdButtonColor(Color.parseColor("#FF018786"))
                    .withFirstButtonListner(view -> {

                        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        galleryIntent.setType("image/*");
                        startActivityForResult(Intent.createChooser(galleryIntent, "Select Picture"), PROFILE_PHOTO_REQUEST_CODE);
                        flatDialog.dismiss();

                    })
                    .withSecondButtonListner(view -> {

                        Intent intent = new Intent(getContext(), PostImageZoomActivity.class);
                        intent.putExtra("postImage", getUser.getProfileImage());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getContext().startActivity(intent);

                        flatDialog.dismiss();
                    })
                    .withThirdButtonListner(view -> flatDialog.dismiss())
                    .show();
        });

        binding.changeCoverPhoto.setOnClickListener(v -> {

            final FlatDialog flatDialog = new FlatDialog(getContext());
            flatDialog.setTitle("Select from gallery/View cover photo")
                    .setFirstButtonText("Select from gallery")
                    .setSecondButtonText("View profile image")
                    .setThirdButtonText("Cancel")
                    .isCancelable(true)
                    .setBackgroundColor(Color.parseColor("#79018786"))
                    .setFirstButtonColor(Color.parseColor("#FF018786"))
                    .setSecondButtonColor(Color.parseColor("#FF018786"))
                    .setThirdButtonColor(Color.parseColor("#FF018786"))
                    .withFirstButtonListner(view -> {

                        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        galleryIntent.setType("image/*");
                        startActivityForResult(galleryIntent, COVER_PHOTO_REQUEST_CODE);

                        flatDialog.dismiss();
                    })
                    .withSecondButtonListner(view -> {

                        Intent intent = new Intent(getContext(), PostImageZoomActivity.class);
                        intent.putExtra("postImage", getUser.getCoverPhoto());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getContext().startActivity(intent);

                        flatDialog.dismiss();
                    })
                    .withThirdButtonListner(view -> flatDialog.dismiss())
                    .show();
        });

    }

    private void setFollowing() {

        followingList = new ArrayList<>();

        FollowingAdapter adapter = new FollowingAdapter(followingList, getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL,
                false);
        binding.followingRv.setLayoutManager(linearLayoutManager);
        binding.followingRv.setAdapter(adapter);

        firebaseDatabase.getReference().child("Users/" + auth.getCurrentUser().getUid() + "/following")
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        followingList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Following following = dataSnapshot.getValue(Following.class);
                            Log.d("Following", following.getFollowing() + " " + following.getFollowedAt());
                            followingList.add(following);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupVerificationTick() {

        binding.verifiedAccountIcon.setVisibility(View.VISIBLE);

        binding.verifiedAccountIcon.setOnClickListener(view -> new SimpleTooltip.Builder(getContext())
                .anchorView(view)
                .text("Admin")
                .gravity(Gravity.TOP)
                .textColor(Color.parseColor("#FFFFFF"))
                .backgroundColor(Color.parseColor("#79018786"))
                .arrowColor(Color.parseColor("#FF018786"))
                .animated(true)
                .transparentOverlay(true)
                .build()
                .show());

    }

    private void setConstraints() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(binding.parentOfProfileFragment);
        constraintSet.connect(R.id.textView7, ConstraintSet.END, R.id.parentOfProfileFragment, ConstraintSet.END, 0);
        constraintSet.connect(R.id.callView, ConstraintSet.END, R.id.parentOfProfileFragment, ConstraintSet.END, 0);
        constraintSet.applyTo(binding.parentOfProfileFragment);
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