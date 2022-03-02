package com.example.communityfeedapp.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.databinding.StoryRvDesignBinding;
import com.example.communityfeedapp.models.Story;
import com.example.communityfeedapp.models.User;
import com.example.communityfeedapp.models.UserStories;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import omari.hamza.storyview.StoryView;
import omari.hamza.storyview.callback.StoryClickListeners;
import omari.hamza.storyview.model.MyStory;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    ArrayList<Story> storyArrayList;
    Context context;

    public StoryAdapter(ArrayList<Story> list, Context context) {
        this.storyArrayList = list;
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
        Story story = storyArrayList.get(position);
        if (story.getStories().size() > 0) {
            UserStories lastStory = story.getStories().get(story.getStories().size() - 1);
            Picasso.get()
                    .load(lastStory.getImage())
                    .into(holder.binding.storyImg);
            holder.binding.statusCircle.setPortionsCount(story.getStories().size());

            FirebaseDatabase.getInstance().getReference()
                    .child("Users/" + story.getStoryBy())
                    .addValueEventListener(new ValueEventListener() {
                        @Override

                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User user = snapshot.getValue(User.class);
                            Picasso.get()
                                    .load(user.getProfileImage())
                                    .placeholder(R.drawable.placeholder)
                                    .into(holder.binding.storyProfileImg);
                            holder.binding.storyNameTxt.setText(user.getName());
                            holder.binding.storyImg.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    //StoryView Implementation
                                    ArrayList<MyStory> myStories = new ArrayList<>();

                                    for (UserStories stories : story.getStories()) {
                                        myStories.add(new MyStory(
                                                stories.getImage()
                                        ));
                                    }

                                    new StoryView.Builder(((AppCompatActivity) context).getSupportFragmentManager())
                                            .setStoriesList(myStories) // Required
                                            .setStoryDuration(5000) // Default is 2000 Millis (2 Seconds)
                                            .setTitleText(user.getName()) // Default is Hidden
                                            .setSubtitleText("") // Default is Hidden
                                            .setTitleLogoUrl(user.getProfileImage()) // Default is Hidden
                                            .setStoryClickListeners(new StoryClickListeners() {
                                                @Override
                                                public void onDescriptionClickListener(int position) {
                                                    //your action
                                                }

                                                @Override
                                                public void onTitleIconClickListener(int position) {
                                                    //your action
                                                }
                                            }) // Optional Listeners
                                            .build() // Must be called before calling show method
                                            .show();

                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }

    @Override
    public int getItemCount() {

        Log.d("Count Story", String.valueOf(storyArrayList.size()));
        return storyArrayList.size();
    }

    public class StoryViewHolder extends RecyclerView.ViewHolder {

        StoryRvDesignBinding binding;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = StoryRvDesignBinding.bind(itemView);

        }
    }
}
