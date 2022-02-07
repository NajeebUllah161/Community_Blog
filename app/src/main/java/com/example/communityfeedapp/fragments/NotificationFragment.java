package com.example.communityfeedapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.adapters.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;

public class NotificationFragment extends Fragment {


    ViewPager viewPager;
    TabLayout tabLayout;

    public NotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Checkpoint", "On Root Navigation fragment");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        viewPager = view.findViewById(R.id.viewPager);
        viewPager.setAdapter(new ViewPagerAdapter(getChildFragmentManager()));

        tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        return view;
    }
}