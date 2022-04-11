package com.example.communityfeedapp.fragments;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.adapters.PostAdapter;
import com.example.communityfeedapp.adapters.StoryAdapter;
import com.example.communityfeedapp.databinding.FragmentHomeBinding;
import com.example.communityfeedapp.models.FollowModel;
import com.example.communityfeedapp.models.Post;
import com.example.communityfeedapp.models.Story;
import com.example.communityfeedapp.models.User;
import com.example.communityfeedapp.models.UserStories;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.OnMenuItemClickListener;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment {

    ArrayList<Story> storyArrayList = new ArrayList<>();
    ArrayList<Post> postList = new ArrayList<>();
    ArrayList<Post> postListSolved = new ArrayList<>();
    ArrayList<Post> postListUnSolved = new ArrayList<>();
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    FirebaseAuth auth;
    ActivityResultLauncher<String> galleryLauncher;
    ProgressDialog dialog;
    FragmentHomeBinding binding;
    PowerMenu powerMenu;
    PostAdapter postAdapter;

    private final OnMenuItemClickListener<PowerMenuItem> onMenuItemClickListener = new OnMenuItemClickListener<PowerMenuItem>() {
        @Override
        public void onItemClick(int position, PowerMenuItem item) {
            //Toast.makeText(getContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
            powerMenu.setSelectedPosition(position); // change selected item
            if (item.getTitle().equals("Solved")) {

                postAdapter = new PostAdapter(postListSolved, getContext());
                binding.dashboardRv.setAdapter(postAdapter);
                binding.dashboardRv.hideShimmerAdapter();
                loadProfileImg();
                postAdapter.notifyDataSetChanged();

            } else if (item.getTitle().equals("UnSolved")) {

                postAdapter = new PostAdapter(postListUnSolved, getContext());
                binding.dashboardRv.setAdapter(postAdapter);
                binding.dashboardRv.hideShimmerAdapter();
                loadProfileImg();
                postAdapter.notifyDataSetChanged();

            } else {

                postAdapter = new PostAdapter(postList, getContext());
                binding.dashboardRv.setAdapter(postAdapter);
                binding.dashboardRv.hideShimmerAdapter();
                loadProfileImg();
                postAdapter.notifyDataSetChanged();

            }
            powerMenu.dismiss();
        }
    };

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialog = new ProgressDialog(getContext());
        firebaseDatabase = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        abcFunction();
        setupPowerMenu();

    }

    private void abcFunction() {

        List<String> followers = new ArrayList<>();
        firebaseDatabase.getReference().child("Users").child(auth.getCurrentUser().getUid())
                .child("followers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot: snapshot.getChildren()){
                    FollowModel followModel = dataSnapshot.getValue(FollowModel.class);
                    followers.add(followModel.getFollowedBy());
                }
                Log.d("Followers",followers.toString());

            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }

    private void setupPowerMenu() {

        List<PowerMenuItem> list = new ArrayList<>();
        list.add(new PowerMenuItem("All"));
        list.add(new PowerMenuItem("Solved"));
        list.add(new PowerMenuItem("UnSolved"));

        powerMenu = new PowerMenu.Builder(getContext())
                .addItemList(list) // list has "Novel", "Poetry", "Art"
                .setAnimation(MenuAnimation.SHOWUP_BOTTOM_RIGHT) // Animation start point (TOP | LEFT).
                .setMenuRadius(10f) // sets the corner radius.
                .setMenuShadow(10f) // sets the shadow.
                .setTextColor(ContextCompat.getColor(getContext(), R.color.teal_700))
                .setTextGravity(Gravity.LEFT)
                .setTextTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
                .setSelectedTextColor(Color.WHITE)
                .setMenuColor(Color.WHITE)
                .setSelectedMenuColor(ContextCompat.getColor(getContext(), R.color.black))
                .setOnMenuItemClickListener(onMenuItemClickListener)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.dashboardRv.showShimmerAdapter();

        // clear arraylists to avoid duplication of data
        clearArrayLists();

        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Story Uploading");
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);

        // Setting up Story RecyclerView

        StoryAdapter adapter = new StoryAdapter(storyArrayList, getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, true);
        binding.storyRv.setLayoutManager(linearLayoutManager);
        binding.storyRv.setNestedScrollingEnabled(false);
        binding.storyRv.setAdapter(adapter);

        firebaseDatabase.getReference()
                .child("stories")
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            storyArrayList.clear();
                            for (DataSnapshot storySnapshot : snapshot.getChildren()) {
                                Story story = new Story();
                                story.setStoryBy(storySnapshot.getKey());
                                story.setStoryAt(storySnapshot.child("postedBy").getValue(Long.class));

                                ArrayList<UserStories> stories = new ArrayList<>();
                                for (DataSnapshot snapshot1 : storySnapshot.child("userStories").getChildren()) {
                                    UserStories userStories = snapshot1.getValue(UserStories.class);
                                    stories.add(userStories);
                                }
                                story.setStories(stories);
                                storyArrayList.add(story);
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Setting up Dashboard RecyclerView

        postAdapter = new PostAdapter(postList, getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, true);
        binding.dashboardRv.setLayoutManager(layoutManager);
        binding.dashboardRv.setNestedScrollingEnabled(false);

        firebaseDatabase.getReference().child("posts").addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                postListSolved.clear();
                postListUnSolved.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post post = dataSnapshot.getValue(Post.class);
                    post.setPostId(dataSnapshot.getKey());
                    postList.add(post);
                    if (post.isSolved()) {
                        //Log.d("Solved", "solved");
                        postListSolved.add(post);
                    } else {
                        postListUnSolved.add(post);
                        //Log.d("Solved", "not");
                    }
                }
                binding.dashboardRv.setAdapter(postAdapter);
                binding.dashboardRv.hideShimmerAdapter();
                loadProfileImg();
                postAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

//        binding.isSolved.setOnCheckedChangeListener((compoundButton, b) -> {
//            if (b) {
//                firebaseDatabase.getReference().child("posts").addValueEventListener(new ValueEventListener() {
//                    @SuppressLint("NotifyDataSetChanged")
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        postList.clear();
//                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
//                            Post post = dataSnapshot.getValue(Post.class);
//                            post.setPostId(dataSnapshot.getKey());
//                            if (post.isSolved()) {
//                                Log.d("Solved", "solved");
//                                postList.add(post);
//                            } else {
//                                Log.d("Solved", "not");
//                            }
//                        }
//                        binding.dashboardRv.setAdapter(postAdapter);
//                        binding.dashboardRv.hideShimmerAdapter();
//                        loadProfileImg();
//                        postAdapter.notifyDataSetChanged();
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//            } else {
//                firebaseDatabase.getReference().child("posts").addValueEventListener(new ValueEventListener() {
//                    @SuppressLint("NotifyDataSetChanged")
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        postList.clear();
//                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
//                            Post post = dataSnapshot.getValue(Post.class);
//                            post.setPostId(dataSnapshot.getKey());
//                            postList.add(post);
//                        }
//                        binding.dashboardRv.setAdapter(postAdapter);
//                        binding.dashboardRv.hideShimmerAdapter();
//                        loadProfileImg();
//                        postAdapter.notifyDataSetChanged();
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//
//        });

        binding.addStoryImg.setOnClickListener(view1 -> galleryLauncher.launch("image/*"));

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                binding.addStoryImg.setImageURI(result);
                dialog.show();
                final StorageReference reference = firebaseStorage.getReference()
                        .child("stories")
                        .child(FirebaseAuth.getInstance().getUid())
                        .child(new Date().getTime() + "");
                reference.putFile(result).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Story story = new Story();
                                story.setStoryAt(new Date().getTime());
                                firebaseDatabase.getReference()
                                        .child("stories")
                                        .child(FirebaseAuth.getInstance().getUid())
                                        .child("postedBy")
                                        .setValue(story.getStoryAt()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        UserStories stories = new UserStories(uri.toString(), story.getStoryAt());

                                        firebaseDatabase.getReference()
                                                .child("stories")
                                                .child(FirebaseAuth.getInstance().getUid())
                                                .child("userStories")
                                                .push()
                                                .setValue(stories);
                                        dialog.dismiss();

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                dialog.dismiss();
                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        binding.filter.setOnClickListener(view -> powerMenu.showAsDropDown(view));

        binding.profileImage.setOnClickListener(view -> ((BottomNavigationView) getActivity().findViewById(R.id.bottom_navigation_bar)).setSelectedItemId(R.id.item_userprofile));
        return binding.getRoot();
    }

    private void loadProfileImg() {

        FirebaseDatabase.getInstance()
                .getReference()
                .child("Users/" + Objects.requireNonNull(auth.getCurrentUser()).getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    Picasso.get()
                            .load(user.getProfileImage())
                            .placeholder(R.drawable.placeholder)
                            .into(binding.profileImage);

                } else {
                    Toast.makeText(getContext(), "No user exists", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearArrayLists() {
        storyArrayList.clear();
        postList.clear();
    }
}
