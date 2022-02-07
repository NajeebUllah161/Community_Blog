package com.example.communityfeedapp.fragments;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.adapters.DashboardAdapter;
import com.example.communityfeedapp.adapters.StoryAdapter;
import com.example.communityfeedapp.models.DashboardModel;
import com.example.communityfeedapp.models.StoryModel;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    RecyclerView storyRv, dashboardRv;
    ArrayList<StoryModel> storyModelArrayList = new ArrayList<>();
    ArrayList<DashboardModel> dashboardModelArrayList = new ArrayList<>();

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
        dashboardRv = view.findViewById(R.id.dashboard_Rv);

        return view;
    }

    private void clearArrayLists() {
        storyModelArrayList.clear();
        dashboardModelArrayList.clear();

    }

    @Override
    public void onResume() {
        super.onResume();

        new LoadData().execute();

    }

    private void setupRecyclerViews() {

        // Clear ArrayLists
        clearArrayLists();

        // Setting up Story RecyclerView
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
        dashboardModelArrayList.add(new DashboardModel(R.drawable.post_7, R.drawable.post_4, R.drawable.saved,
                "Denis Kane",
                "Traveller",
                "464",
                "12",
                "15"));
        dashboardModelArrayList.add(new DashboardModel(R.drawable.profile2, R.drawable.post_6, R.drawable.unsaved,
                "Lisa Melis",
                "Vlogger",
                "763",
                "53",
                "31"));
        dashboardModelArrayList.add(new DashboardModel(R.drawable.profile3, R.drawable.post_3, R.drawable.saved,
                "Jake Miller",
                "Economist",
                "1.2k",
                "53`",
                "566"));

        dashboardModelArrayList.add(new DashboardModel(R.drawable.profile, R.drawable.post_5, R.drawable.unsaved,
                "Madara Kupce",
                "Social Worker",
                "556",
                "153",
                "131"));


        /*
        Log.d("Dashboard", " "
                + "\nIndex 1 : "
                + "Profile:" + dashboardModelArrayList.get(0).getProfile() + " "
                + "Post Image:" + dashboardModelArrayList.get(0).getPostImage() + " "
                + "Save:" + dashboardModelArrayList.get(0).getSave() + " "
                + "Name:" + dashboardModelArrayList.get(0).getName() + " "
                + "About:" + dashboardModelArrayList.get(0).getAbout() + " "
                + "Like:" + dashboardModelArrayList.get(0).getLike() + " "
                + "Comment:" + dashboardModelArrayList.get(0).getComment() + " "
                + "Share:" + dashboardModelArrayList.get(0).getShare() + " "
                + "\n"

                + "Index 2 : "
                + "Profile:" + dashboardModelArrayList.get(1).getProfile() + " "
                + "Post Image:" + dashboardModelArrayList.get(1).getPostImage() + " "
                + "Save:" + dashboardModelArrayList.get(1).getSave() + " "
                + "Name:" + dashboardModelArrayList.get(1).getName() + " "
                + "About:" + dashboardModelArrayList.get(1).getAbout() + " "
                + "Like:" + dashboardModelArrayList.get(1).getLike() + " "
                + "Comment:" + dashboardModelArrayList.get(1).getComment() + " "
                + "Share:" + dashboardModelArrayList.get(1).getShare() + " "
                + ""
        );
        */

        DashboardAdapter dashboardAdapter = new DashboardAdapter(dashboardModelArrayList, getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        dashboardRv.setLayoutManager(layoutManager);
        // dashboardRv.setNestedScrollingEnabled(false);
        dashboardRv.setAdapter(dashboardAdapter);

    }

    @SuppressLint("StaticFieldLeak")
    public class LoadData extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            setupRecyclerViews();
            return null;
        }
    }

}
