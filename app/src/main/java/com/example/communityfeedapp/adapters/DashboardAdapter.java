package com.example.communityfeedapp.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.models.DashboardModel;

import java.util.ArrayList;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardViewHolder> {

    ArrayList<DashboardModel> dashboardModelArrayList;
    Context context;

    public DashboardAdapter(ArrayList<DashboardModel> list, Context context) {
        this.dashboardModelArrayList = list;
        this.context = context;
    }

    @NonNull
    @Override
    public DashboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.dashboard_rv_sample, parent, false);
        return new DashboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DashboardViewHolder holder, int position) {

        DashboardModel model = dashboardModelArrayList.get(position);

        holder.profile.setImageResource(model.getProfile());
        holder.postImage.setImageResource(model.getPostImage());
        holder.save.setImageResource(model.getSave());
        holder.name.setText(model.getName());
        holder.about.setText(model.getAbout());
        holder.like.setText(model.getLike());
        holder.comment.setText(model.getComment());
        holder.share.setText(model.getShare());

    }

    @Override
    public int getItemCount() {
        Log.d("Count Dashboard", String.valueOf(dashboardModelArrayList.size()));
        return dashboardModelArrayList.size();
    }

    public static class DashboardViewHolder extends RecyclerView.ViewHolder {

        ImageView profile, postImage, save;
        TextView name, about, like, comment, share;

        public DashboardViewHolder(@NonNull View itemView) {
            super(itemView);

            profile = itemView.findViewById(R.id.profile_image_dashboard);
            postImage = itemView.findViewById(R.id.postImg);
            save = itemView.findViewById(R.id.save);
            name = itemView.findViewById(R.id.userName_dashboard);
            about = itemView.findViewById(R.id.aboutDb_txt);
            like = itemView.findViewById(R.id.like);
            comment = itemView.findViewById(R.id.comment);
            share = itemView.findViewById(R.id.share);
        }
    }
}
