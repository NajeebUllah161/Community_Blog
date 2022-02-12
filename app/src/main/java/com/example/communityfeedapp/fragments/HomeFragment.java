package com.example.communityfeedapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.adapters.PostAdapter;
import com.example.communityfeedapp.adapters.StoryAdapter;
import com.example.communityfeedapp.models.Post;
import com.example.communityfeedapp.models.StoryModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    RecyclerView storyRv, dashboardRv;
    ArrayList<StoryModel> storyModelArrayList = new ArrayList<>();
    ArrayList<Post> postList = new ArrayList<>();
    FirebaseDatabase firebaseDatabase;
    FirebaseAuth auth;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseDatabase = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // clear arraylists to avoid duplication of data
        clearArrayLists();

        // Setting up Story RecyclerView
        storyRv = view.findViewById(R.id.storyRv);
        storyModelArrayList.add(new StoryModel(
                R.drawable.status_img,
                R.drawable.ic_live,
                R.drawable.profile,
                "John Doe"));
        storyModelArrayList.add(new StoryModel(
                R.drawable.post_6,
                R.drawable.ic_live,
                R.drawable.profile3,
                "Ashley Joe"));
        storyModelArrayList.add(new StoryModel(
                R.drawable.post_5,
                R.drawable.ic_live,
                R.drawable.profile2,
                "James Hudson"));
        storyModelArrayList.add(new StoryModel(
                R.drawable.post_3,
                R.drawable.ic_live,
                R.drawable.post_7,
                "Leo Santos"));
        storyModelArrayList.add(new StoryModel(
                R.drawable.post_4,
                R.drawable.ic_live,
                R.drawable.post_8,
                "Callum"));

        StoryAdapter adapter = new StoryAdapter(storyModelArrayList, getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        storyRv.setLayoutManager(linearLayoutManager);
        storyRv.setNestedScrollingEnabled(false);
        storyRv.setAdapter(adapter);


        // Setting up Dashboard RecyclerView
        dashboardRv = view.findViewById(R.id.dashboard_Rv);

        PostAdapter postAdapter = new PostAdapter(postList, getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        dashboardRv.setLayoutManager(layoutManager);
        dashboardRv.setNestedScrollingEnabled(false);
        dashboardRv.setAdapter(postAdapter);

        firebaseDatabase.getReference().child("posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post post = dataSnapshot.getValue(Post.class);
                    postList.add(post);
                }
                postAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return view;
    }

    private void clearArrayLists() {
        storyModelArrayList.clear();
        postList.clear();

    }
}
