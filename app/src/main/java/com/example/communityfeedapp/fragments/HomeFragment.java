package com.example.communityfeedapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.adapters.StoryAdapter;
import com.example.communityfeedapp.models.StoryModel;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    RecyclerView storyRv;
    ArrayList<StoryModel> storyModelArrayList;
    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        storyRv = view.findViewById(R.id.storyRv);
        storyModelArrayList = new ArrayList<>();
        storyModelArrayList.add(new StoryModel(R.drawable.status_img,R.drawable.ic_live,R.drawable.profile,"John Doe"));
        storyModelArrayList.add(new StoryModel(R.drawable.profile,R.drawable.ic_live,R.drawable.status_img,"Ashley Joe"));

        StoryAdapter adapter = new StoryAdapter(storyModelArrayList,getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL,false);
        storyRv.setLayoutManager(linearLayoutManager);
        storyRv.setNestedScrollingEnabled(false);
        storyRv.setAdapter(adapter);

        return view;
    }
}