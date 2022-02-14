package com.example.communityfeedapp.fragments;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.adapters.PostAdapter;
import com.example.communityfeedapp.adapters.StoryAdapter;
import com.example.communityfeedapp.models.Post;
import com.example.communityfeedapp.models.Story;
import com.example.communityfeedapp.models.User;
import com.example.communityfeedapp.models.UserStories;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.Date;

public class HomeFragment extends Fragment {

    RecyclerView storyRv, dashboardRv;
    ArrayList<Story> storyArrayList = new ArrayList<>();
    ArrayList<Post> postList = new ArrayList<>();
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    FirebaseAuth auth;
    RoundedImageView addStoryImg;
    ActivityResultLauncher<String> galleryLauncher;
    ProgressDialog dialog;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialog = new ProgressDialog(getContext());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // clear arraylists to avoid duplication of data
        clearArrayLists();

        firebaseDatabase = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Story Uploading");
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);


        // Setting up Story RecyclerView
        storyRv = view.findViewById(R.id.storyRv);

        StoryAdapter adapter = new StoryAdapter(storyArrayList, getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        storyRv.setLayoutManager(linearLayoutManager);
        storyRv.setNestedScrollingEnabled(false);
        storyRv.setAdapter(adapter);

        firebaseDatabase.getReference().child("stories")
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
        dashboardRv = view.findViewById(R.id.dashboard_Rv);

        PostAdapter postAdapter = new PostAdapter(postList, getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, true);
        dashboardRv.setLayoutManager(layoutManager);
        dashboardRv.setNestedScrollingEnabled(false);
        dashboardRv.setAdapter(postAdapter);

        firebaseDatabase.getReference().child("posts").addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post post = dataSnapshot.getValue(Post.class);
                    post.setPostId(dataSnapshot.getKey());
                    postList.add(post);
                }
                postAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        addStoryImg = view.findViewById(R.id.addStoryImg);
        addStoryImg.setOnClickListener(view1 -> galleryLauncher.launch("image/*"));

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent()
                , result -> {
                    addStoryImg.setImageURI(result);
                    dialog.show();
                    final StorageReference storageReference = firebaseStorage.getReference()
                            .child("stories/" + auth.getCurrentUser().getUid() + "/" + new Date().getTime() + "");
                    storageReference.putFile(result)
                            .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                Story story = new Story();
                                story.setStoryAt(new Date().getTime());
                                firebaseDatabase.getReference()
                                        .child("stories/" + auth.getCurrentUser().getUid() + "/postedBy")
                                        .setValue(story.getStoryAt())
                                        .addOnSuccessListener(unused -> {
                                            UserStories userStories = new UserStories(uri.toString(), story.getStoryAt());
                                            firebaseDatabase.getReference()
                                                    .child("stories/" + auth.getCurrentUser().getUid() + "/userStories")
                                                    .push()
                                                    .setValue(userStories)
                                                    .addOnSuccessListener(unused1 -> dialog.dismiss()).addOnFailureListener(e -> {
                                                dialog.dismiss();
                                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                        }).addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
                            }).addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show())).addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
                });

        return view;
    }

    private void clearArrayLists() {
        storyArrayList.clear();
        postList.clear();
    }
}
