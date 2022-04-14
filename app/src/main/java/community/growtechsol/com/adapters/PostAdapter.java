package community.growtechsol.com.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import community.growtechsol.com.R;
import community.growtechsol.com.activities.CommentActivity;
import community.growtechsol.com.activities.PostImageZoomActivity;
import community.growtechsol.com.databinding.DashboardRvSampleBinding;
import community.growtechsol.com.edit_dialogues.EditPostDialogue;
import community.growtechsol.com.models.Notification;
import community.growtechsol.com.models.Post;
import community.growtechsol.com.models.User;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.OnMenuItemClickListener;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    ArrayList<Post> postModelArrayList;
    Context context;
    PowerMenu powerMenu;
    Intent intent;
    String postId;
    Post post;

    private final OnMenuItemClickListener<PowerMenuItem> onMenuItemClickListener = new OnMenuItemClickListener<PowerMenuItem>() {
        @Override
        public void onItemClick(int position, PowerMenuItem item) {
            //Toast.makeText(context, item.getTitle(), Toast.LENGTH_SHORT).show();
            powerMenu.setSelectedPosition(position); // change selected item
            if (item.getTitle().equals("Edit post")) {
                context.startActivity(intent);
            } else {
                saveDeletedPost(post);
                FirebaseDatabase.getInstance().getReference().child("posts/" + postId).removeValue().addOnSuccessListener(unused -> {
                    Toast.makeText(context, "Post deleted Successfully!", Toast.LENGTH_SHORT).show();

                    FirebaseDatabase.getInstance().getReference()
                            .child("Users")
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child("totalPosts")
                            .setValue(ServerValue.increment(-1)).addOnSuccessListener(unused1 -> {
                        Log.d("PostAdapter", "remove -1 from totalPostCount");
                    }).addOnFailureListener(e -> {
                        Toast.makeText(context, "Unable to deduct postCount due to" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }).addOnFailureListener(e -> Toast.makeText(context, "Unable to delete post due to!" + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
            powerMenu.dismiss();
        }
    };

    private void saveDeletedPost(Post post) {
        FirebaseDatabase.getInstance().getReference()
                .child("DeletedPosts")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(new Date() + "")
                .setValue(post);
    }

    MediaPlayer player;
    int length;

    public PostAdapter(ArrayList<Post> list, Context context) {
        this.postModelArrayList = list;
        this.context = context;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.dashboard_rv_sample, parent, false);
        return new PostViewHolder(view);
    }

    @SuppressLint({"SetTextI18n", "CheckResult"})
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {

        Post model = postModelArrayList.get(position);

        // Check to restrict user to only edit his/her post
        if ((model.getPostedBy().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) && !model.isSolved())
            holder.binding.verticalDotsPost.setVisibility(View.VISIBLE);

        if (model.getPostShares() != 0) {
            holder.binding.share.setText(model.getPostShares() + "");
        }

        // Setup Image from Firebase
        if (model.getPostImage() != null) {
            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.placeholder);

            Glide.with(context)
                    .setDefaultRequestOptions(requestOptions)
                    .load(model.getPostImage()).into(holder.binding.postImg);

        } else {
            holder.binding.postImg.setVisibility(View.GONE);
            holder.binding.postImageContainer.setVisibility(View.GONE);
        }

        // Setup Audio Recording
        if (model.getPostRecording() != null) {
            holder.binding.audioContainer.setVisibility(View.VISIBLE);
            holder.binding.playAudio.setOnClickListener(view -> {
                try {
                    player = new MediaPlayer();
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    player.setDataSource(model.getPostRecording());
                    player.prepare();
                    player.start();
                    holder.binding.playAudio.setVisibility(View.GONE);
                    holder.binding.pauseAudio.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.d("Exception", e.getMessage());
                }
                player.setOnCompletionListener(mediaPlayer -> {
                    holder.binding.playAudio.setVisibility(View.VISIBLE);
                    holder.binding.pauseAudio.setVisibility(View.GONE);
                });
            });

            // Pause recording
            holder.binding.pauseAudio.setOnClickListener(view -> {
                if (player != null) {
                    player.pause();
                    length = player.getCurrentPosition();
                    holder.binding.pauseAudio.setVisibility(View.GONE);
                    holder.binding.resumeAudio.setVisibility(View.VISIBLE);
                } else {
                    Log.d("PostAdapter", "Player is null");
                }
            });

            // Resume recording
            holder.binding.resumeAudio.setOnClickListener(view -> {
                if (player != null) {
                    holder.binding.resumeAudio.setVisibility(View.GONE);
                    holder.binding.pauseAudio.setVisibility(View.VISIBLE);
                    player.seekTo(length);
                    player.start();
                    player.setOnCompletionListener(mediaPlayer -> {
                        holder.binding.pauseAudio.setVisibility(View.GONE);
                        holder.binding.playAudio.setVisibility(View.VISIBLE);
                    });
                } else {
                    Log.d("PostAdapter", "Player is null");
                }
            });
        } else {
            holder.binding.audioContainer.setVisibility(View.GONE);
        }

        // Populate likes and comments from Firebase
        holder.binding.like.setText(model.getPostLikes() + "");
        holder.binding.comment.setText(model.getCommentCount() + "");

        // Setup post Title from Firebase
        String header = model.getPostTitle();
        if (header.equals("")) {
            holder.binding.postTitleDesign.setVisibility(View.GONE);
        } else {
            holder.binding.postTitleDesign.setText(Html.fromHtml("<b>" + model.getPostTitle() + "</b>"));
            holder.binding.postTitleDesign.setVisibility(View.VISIBLE);
        }

        // Setup post description from Firebase
        String description = model.getPostDescription();
        if (description.equals("")) {
            holder.binding.postDescriptionDesign.setVisibility(View.GONE);
        } else {
            holder.binding.postDescriptionDesign.setText(model.getPostDescription());
            holder.binding.postDescriptionDesign.setVisibility(View.VISIBLE);
        }


        long postedAt = model.getPostedAt();
        if (postedAt == 0) {
            holder.binding.timeOfPost.setVisibility(View.GONE);
        } else {
            Date date = new Date(postedAt);
            long time = date.getTime();
            String timeAgo = TimeAgo.using(time);
            holder.binding.timeOfPost.setText(timeAgo);
            holder.binding.timeOfPost.setVisibility(View.VISIBLE);

        }

        FirebaseDatabase.getInstance()
                .getReference()
                .child("Users/" + model.getPostedBy()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    Picasso.get()
                            .load(user.getProfileImage())
                            .placeholder(R.drawable.placeholder)
                            .into(holder.binding.profileImageDashboard);

                    holder.binding.userNameDashboard.setText(user.getName());
                    holder.binding.aboutDbTv.setText(user.getProfession());
                } else {
                    Toast.makeText(context, "No user exists", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        FirebaseDatabase.getInstance()
                .getReference()
                .child("posts/" + model.getPostId() + "/likes/" + FirebaseAuth
                        .getInstance()
                        .getCurrentUser()
                        .getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            holder.binding.like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_heart_filled, 0, 0, 0);
                            holder.binding.like.setOnClickListener(view -> {
                                FirebaseDatabase.getInstance().getReference()
                                        .child("posts/" + model.getPostId() + "/likes/" + FirebaseAuth.getInstance()
                                                .getCurrentUser()
                                                .getUid()).removeValue().addOnSuccessListener(unused -> Toast.makeText(context, "Disliked", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show());

                                FirebaseDatabase.getInstance().getReference()
                                        .child("posts/" + model.getPostId() + "/postLikes")
                                        .setValue(model.getPostLikes() - 1)
                                        .addOnSuccessListener(unused1 -> {
                                            holder.binding.like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like, 0, 0, 0);

                                        })
                                        .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show());

                            });
                        } else {

                            holder.binding.like.setOnClickListener(view -> {

                                FirebaseDatabase.getInstance().getReference()
                                        .child("posts/" + model.getPostId() + "/likes/" + FirebaseAuth.getInstance()
                                                .getCurrentUser()
                                                .getUid()).setValue(true).addOnSuccessListener(unused -> {

                                }).addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show());

                                FirebaseDatabase.getInstance().getReference()
                                        .child("posts/" + model.getPostId() + "/postLikes")
                                        .setValue(model.getPostLikes() + 1)
                                        .addOnSuccessListener(unused1 -> {
                                            holder.binding.like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_heart_filled, 0, 0, 0);
                                            Notification notification = new Notification();

                                            Log.d("CheckpointLike", "Here");
                                            notification.setNotificationBy(FirebaseAuth
                                                    .getInstance()
                                                    .getCurrentUser()
                                                    .getUid());
                                            notification.setNotificaitonAt(new Date().getTime());
                                            notification.setPostId(model.getPostId());
                                            notification.setPostedBy(model.getPostedBy());
                                            notification.setNotificationType("like");

                                            FirebaseDatabase.getInstance().getReference()
                                                    .child("notification")
                                                    .child(model.getPostedBy())
                                                    .push()
                                                    .setValue(notification).addOnSuccessListener(unused -> {

                                            }).addOnFailureListener(e -> {
                                                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });

                                        })
                                        .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show());
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        holder.binding.comment.setOnClickListener(view -> {
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("postId", model.getPostId());
            intent.putExtra("postedBy", model.getPostedBy());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });

        holder.binding.share.setOnClickListener(view -> {

            FirebaseDatabase.getInstance().getReference()
                    .child("posts/" + model.getPostId() + "/postShares")
                    .setValue(ServerValue.increment(1))
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

            BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.binding.postImg.getDrawable();
            if (bitmapDrawable == null) {
                //post without Image
                shareTextOnly(header, description);
                Log.d("Checkpoint", "withoutImg");
            } else {
                //post with Image

                Log.d("Checkpoint", "withImage");
                Bitmap bitmapPostImg = bitmapDrawable.getBitmap();
                shareImageAndText(header, description, bitmapPostImg);
            }
        });

        holder.binding.postImg.setOnClickListener(view -> {
            Intent intent = new Intent(context, PostImageZoomActivity.class);
            intent.putExtra("postImage", model.getPostImage());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });

        handlePowerMenu(holder, model);

    }

    private void shareTextOnly(String header, String description) {

        String shareBody = header + "\n" + description + "\n\n" + "Visit now : https://play.google.com/store/apps/details?id=growtechsol.com";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Community Feed GrowPak");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        context.startActivity(Intent.createChooser(shareIntent, "Share Via"));

    }

    private void shareImageAndText(String header, String description, Bitmap bitmapPostImg) {

        String shareBody = header + "\n" + description + "\n\n" + "Visit now : https://play.google.com/store/apps/details?id=growtechsol.com";

        Uri uri = saveImageToShare(bitmapPostImg);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Community Feed GrowPak");
        shareIntent.setType("image/png");
        context.startActivity(Intent.createChooser(shareIntent, "Share Via"));

    }

    private Uri saveImageToShare(Bitmap bitmapPostImg) {
        File imageFolder = new File(context.getCacheDir(), "images");
        Uri uri = null;

        try {
            imageFolder.mkdirs();
            File file = new File(imageFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmapPostImg.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context, "community.growtechsol.com.fileprovider", file);

        } catch (Exception e) {
            Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();

        }
        return uri;
    }

    @SuppressLint("RtlHardcoded")
    private void handlePowerMenu(PostViewHolder holder, Post model) {

        List<PowerMenuItem> list = new ArrayList<>();
        list.add(new PowerMenuItem("Edit post"));
        list.add(new PowerMenuItem("Delete post"));

        powerMenu = new PowerMenu.Builder(context)
                .addItemList(list) // list has "Novel", "Poetry", "Art"
                .setAnimation(MenuAnimation.SHOWUP_BOTTOM_RIGHT) // Animation start point (TOP | LEFT).
                .setMenuRadius(10f) // sets the corner radius.
                .setMenuShadow(10f) // sets the shadow.
                .setTextColor(ContextCompat.getColor(context, R.color.teal_700))
                .setTextGravity(Gravity.LEFT)
                .setTextTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
                .setSelectedTextColor(Color.WHITE)
                .setMenuColor(Color.WHITE)
                .setSelectedMenuColor(ContextCompat.getColor(context, R.color.black))
                .setOnMenuItemClickListener(onMenuItemClickListener)
                .build();

        holder.binding.verticalDotsPost.setOnClickListener(view -> {

            intent = new Intent(context, EditPostDialogue.class);
            intent.putExtra("postTimeStampId", model.getPostedAt());
            intent.putExtra("postImg", model.getPostImage());
            intent.putExtra("postTitle", model.getPostTitle());
            intent.putExtra("postDesc", model.getPostDescription());
            intent.putExtra("postRecording", model.getPostRecording());
            intent.putExtra("recTime", model.getRecTime());

            //Log.d("PostIdTimeStamp", String.valueOf(model.getPostedAt()));
            postId = model.getPostId();
            post = model;
            powerMenu.showAsDropDown(view);
            //Log.d("Equal",model.getPostedBy()+" " + FirebaseAuth.getInstance().getCurrentUser().getUid());

        });

    }

    @Override
    public int getItemCount() {
        // Log.d("Count Dashboard", String.valueOf(postModelArrayList.size()));
        return postModelArrayList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {

        DashboardRvSampleBinding binding;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            binding = DashboardRvSampleBinding.bind(itemView);

        }
    }
}