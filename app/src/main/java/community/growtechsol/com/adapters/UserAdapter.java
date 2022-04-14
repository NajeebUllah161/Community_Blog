package community.growtechsol.com.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import community.growtechsol.com.R;
import community.growtechsol.com.databinding.UserSampleBinding;
import community.growtechsol.com.models.FollowModel;
import community.growtechsol.com.models.Notification;
import community.growtechsol.com.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.viewHolder> {
    Context context;
    ArrayList<User> list;

    public UserAdapter(Context context, ArrayList<User> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_sample, parent, false);


        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {

        User user = list.get(position);
        Picasso.get()
                .load(user.getProfileImage())
                .placeholder(R.drawable.placeholder)
                .into(holder.binding.profileImgUserSample);

        holder.binding.nameUserSample.setText(user.getName());
        holder.binding.professionUserSample.setText(user.getProfession());

        FirebaseDatabase.getInstance()
                .getReference()
                .child("Users/" + user.getUserId() + "/followers/" + FirebaseAuth.getInstance().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            holder.binding.followBtn.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.follow_active_btn));
                            holder.binding.followBtn.setText("Following");
                            holder.binding.followBtn.setTextColor(context.getResources().getColor(R.color.gray));
                            holder.binding.followBtn.setEnabled(false);
                        } else {
                            // Handling follow button
                            holder.binding.followBtn.setOnClickListener(v -> {
                                FollowModel followModel = new FollowModel();
                                followModel.setFollowedBy(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                followModel.setFollowedAt(new Date().getTime());
                                // Log.d("Checkpoint",followModel.getFollowedBy());

                                FirebaseDatabase.getInstance().getReference()
                                        .child("Users/" + user.getUserId() + "/followers/" + FirebaseAuth
                                                .getInstance()
                                                .getCurrentUser()
                                                .getUid())
                                        .setValue(followModel)
                                        .addOnSuccessListener(unused -> FirebaseDatabase.getInstance().getReference()
                                                .child("Users/" + user.getUserId() + "/followersCount")
                                                .setValue(user.getFollowersCount() + 1)
                                                .addOnSuccessListener(unused1 -> {
                                                    holder.binding.followBtn.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.follow_active_btn));
                                                    holder.binding.followBtn.setText("Following");
                                                    holder.binding.followBtn.setTextColor(context.getResources().getColor(R.color.gray));
                                                    holder.binding.followBtn.setEnabled(false);
                                                    Toast.makeText(context, "You Followed: " + user.getName(), Toast.LENGTH_SHORT).show();

                                                    Notification notification = new Notification();
                                                    notification.setNotificationBy(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                                    notification.setNotificaitonAt(new Date().getTime());
                                                    notification.setNotificationType("follow");

                                                    FirebaseDatabase.getInstance()
                                                            .getReference()
                                                            .child("notification")
                                                            .child(user.getUserId())
                                                            .push()
                                                            .setValue(notification);

                                                })
                                                .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show()))
                                        .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show());

                            });


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
        UserSampleBinding binding;

        public viewHolder(@NonNull View itemView) {
            super(itemView);

            binding = UserSampleBinding.bind(itemView);
        }
    }
}