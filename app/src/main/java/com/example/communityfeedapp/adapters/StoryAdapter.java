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
import com.example.communityfeedapp.models.StoryModel;

import java.util.ArrayList;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    ArrayList<StoryModel> storyModelArrayList;
    Context context;

    public StoryAdapter(ArrayList<StoryModel> list, Context context) {
        this.storyModelArrayList = list;
        this.context = context;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.story_rv_design, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        StoryModel storyModel = storyModelArrayList.get(position);
        holder.storyImg.setImageResource(storyModel.getStory());
        holder.profile.setImageResource(storyModel.getProfile());
        holder.storyType.setImageResource(storyModel.getStoryType());
        holder.name.setText(storyModel.getName());

    }

    @Override
    public int getItemCount() {
        return storyModelArrayList.size();
    }

    public class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView storyImg, profile, storyType;
        TextView name;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);

            storyImg = itemView.findViewById(R.id.story_itemview);
            profile = itemView.findViewById(R.id.profile_image_itemview);
            storyType = itemView.findViewById(R.id.storyType);
            name = itemView.findViewById(R.id.name);
        }
    }
}
