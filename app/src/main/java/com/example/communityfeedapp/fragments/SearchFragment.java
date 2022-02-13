package com.example.communityfeedapp.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.communityfeedapp.adapters.UserAdapter;
import com.example.communityfeedapp.databinding.FragmentSearchBinding;
import com.example.communityfeedapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class SearchFragment extends Fragment {

    FragmentSearchBinding binding;
    ArrayList<User> list = new ArrayList<User>();
    FirebaseAuth auth;
    FirebaseDatabase firebaseDatabase;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentSearchBinding.inflate(inflater, container, false);

        UserAdapter userAdapter = new UserAdapter(getContext(), list);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        binding.usersRv.setLayoutManager(linearLayoutManager);
        binding.usersRv.setAdapter(userAdapter);

        firebaseDatabase.getReference().child("Users").addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    user.setUserId(dataSnapshot.getKey());
                    if (!dataSnapshot.getKey().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        list.add(user);
                    }

                }
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        return binding.getRoot();
    }
}