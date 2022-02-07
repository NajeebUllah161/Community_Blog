package com.example.communityfeedapp.adapters;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.communityfeedapp.fragments.Notification2Fragment;
import com.example.communityfeedapp.fragments.RequestFragment;

public class ViewPagerAdapter extends FragmentPagerAdapter {
    public ViewPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            Log.d("Checkpoint","NotificationFragment");
            return new Notification2Fragment();
        } else if (position == 1) {
            Log.d("Checkpoint","RequestFragment");
            return new RequestFragment();
        } else {
            return new Notification2Fragment();
        }
    }


    @Override
    public int getCount() {
        return 2;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        String title = null;
        if (position == 0) {
            title = "NOTIFICATION";
        } else if (position == 1) {
            title = "REQUEST";
        }

        return title;
    }
}
