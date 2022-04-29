package community.growtechsol.com.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import community.growtechsol.com.R;
import community.growtechsol.com.activities.CommentActivity;
import community.growtechsol.com.activities.UserProfileActivity;
import community.growtechsol.com.databinding.NotificationRvSampleBinding;
import community.growtechsol.com.models.Notification;
import community.growtechsol.com.models.User;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.viewHolder> {

    ArrayList<Notification> list;
    Context context;

    public NotificationAdapter(ArrayList<Notification> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.notification_rv_sample, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        Notification notification = list.get(position);

        String type = notification.getNotificationType();
        //Log.d("NotificationBy", notification.getNotificationBy());
        FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child(notification.getNotificationBy())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        Picasso.get()
                                .load(user.getProfileImage())
                                .placeholder(R.drawable.placeholder)
                                .into(holder.binding.profileImgNotificationRv);

                        switch (type) {
                            case "like":
                                holder.binding.notificationTitle.setText(Html.fromHtml("<b>" + user.getName() + "</b>" + " Liked your post"));
                                break;
                            case "comment":
                                holder.binding.notificationTitle.setText(Html.fromHtml("<b>" + user.getName() + "</b>" + " Commented on your post"));
                                break;
                            case "mention":
                                holder.binding.notificationTitle.setText(Html.fromHtml("<b>" + user.getName() + "</b>" + " Mentioned you in a comment"));
                                break;
                            case "share":
                                holder.binding.notificationTitle.setText(Html.fromHtml("<b>" + user.getName() + "</b>" + " shared your post"));
                                break;
                            default:
                                holder.binding.notificationTitle.setText(Html.fromHtml("<b>" + user.getName() + "</b>" + " started following you"));
                                break;
                        }
                        String timeOfNotification = TimeAgo.using(notification.getNotificaitonAt());
                        holder.binding.notificationTime.setText(timeOfNotification);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        holder.binding.openNotification.setOnClickListener(view -> {
            if (type.equals("like") || type.equals("comment") || type.equals("mention") || type.equals("share")) {

                checkOpen(notification);

                holder.binding.openNotification.setBackgroundColor(Color.parseColor("#FFFFFF"));
                Intent intent = new Intent(context, CommentActivity.class);
                intent.putExtra("postId", notification.getPostId());
                intent.putExtra("postedBy", notification.getPostedBy());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else if (type.equals("follow")) {
                checkOpen(notification);
                Intent intent = new Intent(context, UserProfileActivity.class);
                intent.putExtra("userId", notification.getNotificationBy());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                Log.d("NotificationAdapter", "Not does not match");
            }
        });
        Boolean checkOpen = notification.isCheckOpen();
        if (checkOpen) {
            holder.binding.openNotification.setBackgroundColor(Color.parseColor("#FFFFFF"));
        } else {
            Log.d("NotificationAdapter", "It is opened");
        }
    }

    private void checkOpen(Notification notification) {
        Map<String, Object> notificationCheck = new HashMap<>();
        notificationCheck.put("checkOpen", true);

        DatabaseReference dr = FirebaseDatabase.getInstance().getReference()
                .child("notification")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(notification.getNotificationId());

        Log.d("DatabaseReference", dr.toString());
        dr.updateChildren(notificationCheck);

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class viewHolder extends RecyclerView.ViewHolder {

        NotificationRvSampleBinding binding;

        public viewHolder(@NonNull View itemView) {
            super(itemView);
            binding = NotificationRvSampleBinding.bind(itemView);
        }
    }
}
