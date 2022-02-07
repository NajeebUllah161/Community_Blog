package com.example.communityfeedapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.models.NotificationModel;

import java.util.ArrayList;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.viewHolder> {

    ArrayList<NotificationModel> list;
    Context context;

    public NotificationAdapter(ArrayList<NotificationModel> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.notification2sample, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        NotificationModel model = list.get(position);


        holder.profileImg.setImageResource(model.getProfileImg());
        holder.notificationTitle.setText(model.getNotificationTitle());
        holder.notificationTime.setText(model.getNotificationTime());

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class viewHolder extends RecyclerView.ViewHolder {

        ImageView profileImg;
        TextView notificationTitle, notificationTime;

        public viewHolder(@NonNull View itemView) {
            super(itemView);

            profileImg = itemView.findViewById(R.id.profile_img_notification2Rv);
            notificationTitle = itemView.findViewById(R.id.notificationTitle);
            notificationTime = itemView.findViewById(R.id.notificationTime);

        }
    }
}
