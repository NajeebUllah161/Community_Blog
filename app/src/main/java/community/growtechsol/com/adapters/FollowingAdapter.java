package community.growtechsol.com.adapters;

import android.content.Context;
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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import community.growtechsol.com.R;
import community.growtechsol.com.databinding.FriendRvSampleBinding;
import community.growtechsol.com.models.FollowModel;
import community.growtechsol.com.models.Following;
import community.growtechsol.com.models.User;

public class FollowingAdapter extends RecyclerView.Adapter<FollowingAdapter.viewHolder> {

    ArrayList<Following> list;
    Context context;

    public FollowingAdapter(ArrayList<Following> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_rv_sample, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NotNull FollowingAdapter.viewHolder holder, int position) {
        Following followingList = list.get(position);
        FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users/" + followingList.getFollowing())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            Picasso.get()
                                    .load(user.getProfileImage())
                                    .placeholder(R.drawable.placeholder)
                                    .into(holder.binding.profileImgFriendRv);

                            holder.binding.profileImgFriendRv.setOnClickListener(view -> Toast.makeText(context, user.getName(), Toast.LENGTH_LONG).show());
                        } else {
                            Toast.makeText(context, "User doesn't exit", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class viewHolder extends RecyclerView.ViewHolder {
        FriendRvSampleBinding binding;

        public viewHolder(@NotNull View itemView) {
            super(itemView);

            binding = FriendRvSampleBinding.bind(itemView);
        }
    }
}
