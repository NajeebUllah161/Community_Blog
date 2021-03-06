package community.growtechsol.com.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import community.growtechsol.com.R;
import community.growtechsol.com.activities.UserProfileActivity;
import community.growtechsol.com.databinding.FriendRvSampleBinding;
import community.growtechsol.com.models.FollowModel;
import community.growtechsol.com.models.User;

public class FollowersAdapter extends RecyclerView.Adapter<FollowersAdapter.viewHolder> {

    ArrayList<FollowModel> list;
    Context context;

    public FollowersAdapter(ArrayList<FollowModel> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_rv_sample, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        FollowModel followModel = list.get(position);

        FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users/" + followModel.getFollowedBy())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            Picasso.get()
                                    .load(user.getProfileImage())
                                    .placeholder(R.drawable.placeholder)
                                    .into(holder.binding.profileImgFriendRv);

                        } else {
                            Toast.makeText(context, "User doesn't exit", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        holder.binding.profileImgFriendRv.setOnClickListener(view -> {
            Intent intent = new Intent(context, UserProfileActivity.class);
            intent.putExtra("userId", followModel.getFollowedBy());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class viewHolder extends RecyclerView.ViewHolder {

        FriendRvSampleBinding binding;

        public viewHolder(@NonNull View itemView) {
            super(itemView);

            binding = FriendRvSampleBinding.bind(itemView);
        }
    }
}