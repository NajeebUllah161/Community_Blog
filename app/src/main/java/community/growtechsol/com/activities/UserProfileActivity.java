package community.growtechsol.com.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import community.growtechsol.com.R;
import community.growtechsol.com.adapters.FollowersAdapter;
import community.growtechsol.com.adapters.FollowingAdapter;
import community.growtechsol.com.databinding.ActivityUserProfileBinding;
import community.growtechsol.com.models.FollowModel;
import community.growtechsol.com.models.Following;
import community.growtechsol.com.models.Popularity;
import community.growtechsol.com.models.User;
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip;

public class UserProfileActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    ActivityUserProfileBinding binding;
    ArrayList<FollowModel> list;
    ArrayList<Following> followingList;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    FirebaseAuth auth;
    String userId;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseDatabase = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        userId = getIntent().getStringExtra("userId");

        setupFunctions();
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        String date;
        if ((monthOfYear + 1) > 9) {
            if (dayOfMonth > 9) {
                date = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
            } else {
                date = year + "-" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
            }
        } else {
            if (dayOfMonth > 9) {
                date = year + "-" + "0" + (monthOfYear + 1) + "-" + dayOfMonth;
            } else {
                date = year + "-" + "0" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
            }
        }
        binding.setDate.setText(date);
        getAdminActivity(date);
    }

    private void getAdminActivity(String date) {
        DatabaseReference dbr = firebaseDatabase.getReference().child("Users")
                .child(userId)
                .child("adminActivity")
                .child(date);

        dbr.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    Popularity popularity = snapshot.getValue(Popularity.class);
                    binding.likes.setText(popularity.getUserUpVotes() + "");
                    binding.dislikes.setText(popularity.getUserDownVotes() + "");
                } else {
                    binding.likes.setText("0");
                    binding.dislikes.setText("0");
                }

            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setupFunctions() {
        setFollowers();
        setFollowing();
        setupUserData();
        setupCalender();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setupCalender() {
        binding.setDate.setOnClickListener(view -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dpd = DatePickerDialog.newInstance(
                    UserProfileActivity.this,
                    now.get(Calendar.YEAR), // Initial year selection
                    now.get(Calendar.MONTH), // Initial month selection
                    now.get(Calendar.DAY_OF_MONTH) // Inital day selection
            );
            dpd.show(getSupportFragmentManager(), "Datepickerdialog");

        });
    }

    private void checkForSuperAdmin(boolean otherUserIsAdmin) {
        if (otherUserIsAdmin) {
            firebaseDatabase.getReference().child("Users")
                    .child(auth.getCurrentUser().getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                            User user = snapshot.getValue(User.class);
                            // If is SuperAdmin
                            if (user.isSuperAdmin()) {
                                binding.textView12.setVisibility(View.INVISIBLE);
                                binding.followersCount.setVisibility(View.GONE);
                                binding.textView13.setVisibility(View.GONE);
                                binding.followingCount.setVisibility(View.GONE);
                                binding.myFriendRv.setVisibility(View.GONE);
                                binding.followingRv.setVisibility(View.GONE);

                                binding.activityContainer.setVisibility(View.VISIBLE);
                                binding.adminActivityText.setVisibility(View.VISIBLE);

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull @NotNull DatabaseError error) {
                            Toast.makeText(UserProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void setupUserData() {
        // Fetch User data from firebase database
        firebaseDatabase.getReference().child("Users/" + userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User getUser = snapshot.getValue(User.class);
                            if (getUser != null) {
                                Picasso.get()
                                        .load(getUser.getCoverPhoto())
                                        .placeholder(R.drawable.placeholder)
                                        .into(binding.coverPhoto);

                                Picasso.get()
                                        .load(getUser.getProfileImage())
                                        .placeholder(R.drawable.placeholder)
                                        .into(binding.profileImgOfUser);
                                binding.userName.setText(getUser.getName());
                                binding.profession.setText(getUser.getProfession());
                                binding.followersTv.setText(getUser.getFollowersCount() + "");
                                binding.userPerks.setText(getUser.getUserPerks() + "");
                                binding.userPosts.setText(getUser.getTotalPosts() + "");
                                if (getUser.getFollowersCount() == 0) {
                                    binding.textView12.setVisibility(View.INVISIBLE);
                                    binding.followersCount.setVisibility(View.GONE);
                                    binding.myFriendRv.setVisibility(View.GONE);
                                }else{
                                    binding.followersCount.setText(" (" + getUser.getFollowersCount() + ")");
                                }
                                if (getUser.getFollowingCount() == 0) {
                                    binding.textView13.setVisibility(View.GONE);
                                    binding.followingCount.setVisibility(View.GONE);
                                    binding.followingRv.setVisibility(View.GONE);
                                }else{
                                    checkFollowers(getUser.getFollowersCount());
                                    binding.followingCount.setText(" (" + getUser.getFollowingCount() + ")");
                                }
                                if (getUser.getFollowersCount() == 0 && getUser.getFollowingCount() == 0) {
                                    binding.placeholderTxt.setVisibility(View.VISIBLE);
                                }
                                checkForSuperAdmin(getUser.isAdmin());
                                if (getUser.isAdmin()) {
                                    setupVerificationTick();
                                    binding.adminLikes.setText(getUser.getUserUpVotes() + "");
                                    binding.adminDislikes.setText(getUser.getUserDownVotes() + "");
                                } else {
                                    binding.popularityView.setVisibility(View.GONE);
                                    binding.popularitySvg.setVisibility(View.GONE);
                                    binding.view10.setVisibility(View.GONE);
                                    binding.textView8.setVisibility(View.GONE);
                                    binding.adminLikes.setVisibility(View.GONE);
                                    binding.adminDislikes.setVisibility(View.GONE);
                                    binding.slash.setVisibility(View.GONE);
                                    setConstraints();
                                }
                                setupImgClickListeners(getUser);

                            } else {
                                Toast.makeText(UserProfileActivity.this, "No user exists", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void checkFollowers(int followersCount) {
        if(followersCount == 0){
            binding.marginStabilizerUp.setVisibility(View.INVISIBLE);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(binding.parentOfUserProfile);
            constraintSet.connect(R.id.textView13, ConstraintSet.TOP, R.id.marginStabilizerUp, ConstraintSet.BOTTOM, 0);
            constraintSet.applyTo(binding.parentOfUserProfile);
        }
    }

    private void setFollowers() {

        list = new ArrayList<>();

        FollowersAdapter adapter = new FollowersAdapter(list, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL,
                false);
        binding.myFriendRv.setLayoutManager(linearLayoutManager);
        binding.myFriendRv.setAdapter(adapter);

        firebaseDatabase.getReference().child("Users/" + userId + "/followers")
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        list.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            FollowModel followModel = dataSnapshot.getValue(FollowModel.class);
                            list.add(followModel);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setFollowing() {

        followingList = new ArrayList<>();

        FollowingAdapter adapter = new FollowingAdapter(followingList, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL,
                false);
        binding.followingRv.setLayoutManager(linearLayoutManager);
        binding.followingRv.setAdapter(adapter);

        firebaseDatabase.getReference().child("Users/" + userId + "/following")
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        followingList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Following following = dataSnapshot.getValue(Following.class);
                            Log.d("Following", following.getFollowing() + " " + following.getFollowedAt());
                            followingList.add(following);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupVerificationTick() {

        binding.verifiedAccountIcon.setVisibility(View.VISIBLE);

        binding.verifiedAccountIcon.setOnClickListener(view -> new SimpleTooltip.Builder(this)
                .anchorView(view)
                .text("Admin")
                .gravity(Gravity.TOP)
                .textColor(Color.parseColor("#FFFFFF"))
                .backgroundColor(Color.parseColor("#79018786"))
                .arrowColor(Color.parseColor("#FF018786"))
                .animated(true)
                .transparentOverlay(true)
                .build()
                .show());

    }

    private void setConstraints() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(binding.parentOfUserProfile);
        constraintSet.connect(R.id.textView7, ConstraintSet.END, R.id.parentOfUserProfile, ConstraintSet.END, 0);
        constraintSet.connect(R.id.callView, ConstraintSet.END, R.id.parentOfUserProfile, ConstraintSet.END, 0);
        constraintSet.applyTo(binding.parentOfUserProfile);
    }

    private void setupImgClickListeners(User getUser) {

        binding.profileImgOfUser.setOnClickListener(v -> {

            Intent intent = new Intent(this, PostImageZoomActivity.class);
            intent.putExtra("postImage", getUser.getProfileImage());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        });

        binding.coverPhoto.setOnClickListener(v -> {

            Intent intent = new Intent(this, PostImageZoomActivity.class);
            intent.putExtra("postImage", getUser.getCoverPhoto());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        });

    }

}