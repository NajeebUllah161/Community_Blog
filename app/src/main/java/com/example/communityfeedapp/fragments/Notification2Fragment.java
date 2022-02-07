package com.example.communityfeedapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.adapters.NotificationAdapter;
import com.example.communityfeedapp.models.NotificationModel;

import java.util.ArrayList;

public class Notification2Fragment extends Fragment {

    RecyclerView recyclerView;
    ArrayList<NotificationModel> list;

    public Notification2Fragment() {
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
        View view = inflater.inflate(R.layout.fragment_notification2, container, false);


        recyclerView = view.findViewById(R.id.notification2Rv);

        list = new ArrayList<>();

        list.add(new NotificationModel(R.drawable.profile2, "<b>Najeeb Ullah</b> has commented on your photo", "just now"));
        list.add(new NotificationModel(R.drawable.profile3, "<b>Ibraheem Khan</b> has liked your photo", "2 minutes ago"));
        list.add(new NotificationModel(R.drawable.profile, "<b>Aroosa</b> has commented on your photo", "5 minutes ago"));
        list.add(new NotificationModel(R.drawable.post_3, "<b>Najeeb Ullah</b> has commented on your photo", "just now"));
        list.add(new NotificationModel(R.drawable.post_4, "<b>Habib</b> has liked you post", "just now"));
        list.add(new NotificationModel(R.drawable.post_5, "<b>Khurram</b> has commented on your photo", "just now"));
        list.add(new NotificationModel(R.drawable.post_6, "<b>Asim</b> started following you", "just now"));
        list.add(new NotificationModel(R.drawable.post_7, "<b>Raja Abdullah</b> has commented on your photo", "just now"));
        list.add(new NotificationModel(R.drawable.post_8, "<b>Maskeen</b> has commented on your photo", "just now"));
        list.add(new NotificationModel(R.drawable.post_9, "<b>Salman</b> has mentioned you in a comment", "just now"));
        list.add(new NotificationModel(R.drawable.post_3, "<b>Abdul Jameel</b> has commented on your photo", "just now"));

        NotificationAdapter adapter = new NotificationAdapter(list, getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);


        return view;
    }
}