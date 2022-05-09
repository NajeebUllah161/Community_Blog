package community.growtechsol.com.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.icu.util.Calendar;
import android.os.AsyncTask;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.OnMenuItemClickListener;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;
import com.squareup.picasso.Picasso;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import community.growtechsol.com.R;
import community.growtechsol.com.adapters.FollowersAdapter;
import community.growtechsol.com.adapters.FollowingAdapter;
import community.growtechsol.com.databinding.ActivityUserProfileBinding;
import community.growtechsol.com.models.FollowModel;
import community.growtechsol.com.models.Following;
import community.growtechsol.com.models.Popularity;
import community.growtechsol.com.models.User;
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static community.growtechsol.com.fragments.AddPostFragment.FCM_SEND;
import static community.growtechsol.com.fragments.AddPostFragment.JSON;

public class UserProfileActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    ActivityUserProfileBinding binding;
    ArrayList<FollowModel> list;
    ArrayList<Following> followingList;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    FirebaseAuth auth;
    String userId;
    PowerMenu powerMenu;
    OkHttpClient mClient;

    private final OnMenuItemClickListener<PowerMenuItem> onMenuItemClickListener = new OnMenuItemClickListener<PowerMenuItem>() {
        @Override
        public void onItemClick(int position, PowerMenuItem item) {
            powerMenu.setSelectedPosition(position);
            if (item.getTitle().equals("Promote to Admin")) {

                Map<String, Object> makeAdmin = new HashMap<>();

                makeAdmin.put("admin", true);
                firebaseDatabase.getReference().child("Users")
                        .child(userId)
                        .updateChildren(makeAdmin)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(getApplicationContext(), binding.userName.getText().toString() + " is an admin now", Toast.LENGTH_LONG).show();
                            sendNotification();
                        });
            } else if (item.getTitle().equals("Demote from Admin")) {
                Map<String, Object> makeAdmin = new HashMap<>();

                makeAdmin.put("admin", false);
                firebaseDatabase.getReference().child("Users")
                        .child(userId)
                        .updateChildren(makeAdmin)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(getApplicationContext(), binding.userName.getText().toString() + " has been demoted from admin role", Toast.LENGTH_LONG).show();
                            finish();
                            startActivity(getIntent());
                        });
            }
            powerMenu.dismiss();
        }
    };

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
                Toast.makeText(UserProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setupFunctions() {
        setFollowers();
        setFollowing();
        setupUserData();
        setupCalender();
        setupMakeAdmin();
        setupEventListeners();
    }

    private void setupEventListeners() {
        binding.totalPosts.setOnClickListener(view -> {
            Intent intent = new Intent(UserProfileActivity.this,UserPostsActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    private void setupMakeAdmin() {
        firebaseDatabase.getReference().child("Users")
                .child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user.isAdmin()) {
                            setupPowerMenu("demote");

                        } else {
                            setupPowerMenu("promote");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {
                        Toast.makeText(UserProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });


    }

    private void setupPowerMenu(String userPromotionStatus) {
        List<PowerMenuItem> list = new ArrayList<>();

        if (userPromotionStatus.equals("promote")) {
            list.add(new PowerMenuItem("Promote to Admin"));
        } else if (userPromotionStatus.equals("demote")) {
            list.add(new PowerMenuItem("Demote from Admin"));
        }

        powerMenu = new PowerMenu.Builder(this)
                .addItemList(list)
                .setAnimation(MenuAnimation.SHOWUP_BOTTOM_RIGHT)
                .setMenuRadius(10f)
                .setMenuShadow(10f)
                .setTextColor(ContextCompat.getColor(this, R.color.teal_700))
                .setTextGravity(Gravity.START)
                .setTextTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
                .setSelectedTextColor(Color.WHITE)
                .setMenuColor(Color.WHITE)
                .setSelectedMenuColor(ContextCompat.getColor(this, R.color.black))
                .setOnMenuItemClickListener(onMenuItemClickListener)
                .build();

        binding.makeAdmin.setOnClickListener(view -> {
            powerMenu.showAsDropDown(view);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setupCalender() {
        binding.setDate.setOnClickListener(view -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dpd = DatePickerDialog.newInstance(
                    UserProfileActivity.this,
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            );
            dpd.show(getSupportFragmentManager(), "Datepickerdialog");
        });
    }

    private void checkForSuperAdmin(User getUser) {
        firebaseDatabase.getReference().child("Users")
                .child(auth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        // If is SuperAdmin
                        if (user.isSuperAdmin() && getUser.isAdmin()) {

                            ConstraintSet constraintSet = new ConstraintSet();
                            constraintSet.clone(binding.parentOfUserProfile);
                            if (getUser.getFollowersCount() == 0) {
                                constraintSet.clear(R.id.activityContainer, ConstraintSet.BOTTOM);
                                constraintSet.connect(R.id.textView13, ConstraintSet.TOP, R.id.activityContainer, ConstraintSet.BOTTOM, 8);
                                constraintSet.applyTo(binding.parentOfUserProfile);
                            } else {
                                constraintSet.clear(R.id.activityContainer, ConstraintSet.BOTTOM);
                                constraintSet.connect(R.id.textView12, ConstraintSet.TOP, R.id.activityContainer, ConstraintSet.BOTTOM, 8);
                                constraintSet.applyTo(binding.parentOfUserProfile);
                            }

                            binding.activityContainer.setVisibility(View.VISIBLE);
                            binding.adminActivityText.setVisibility(View.VISIBLE);

                        }
                        if (user.isSuperAdmin() && !getUser.isSuperAdmin()) {
                            binding.makeAdmin.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {
                        Toast.makeText(UserProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void setupUserData() {
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
                                } else {
                                    binding.followersCount.setText(" (" + getUser.getFollowersCount() + ")");
                                }
                                if (getUser.getFollowingCount() == 0) {
                                    binding.textView13.setVisibility(View.GONE);
                                    binding.followingCount.setVisibility(View.GONE);
                                    binding.followingRv.setVisibility(View.GONE);
                                } else {
                                    checkFollowers(getUser.getFollowersCount());
                                    binding.followingCount.setText(" (" + getUser.getFollowingCount() + ")");
                                }
                                if (getUser.getFollowersCount() == 0 && getUser.getFollowingCount() == 0) {
                                    binding.placeholderTxt.setVisibility(View.VISIBLE);

                                    if (getUser.isAdmin()) {

                                        ConstraintSet constraintSet = new ConstraintSet();
                                        constraintSet.clone(binding.parentOfUserProfile);
                                        constraintSet.connect(R.id.placeholderTxt, ConstraintSet.TOP, R.id.activityContainer, ConstraintSet.BOTTOM, 22);
                                        constraintSet.applyTo(binding.parentOfUserProfile);

                                    }
                                }
                                checkForSuperAdmin(getUser);
                                if (getUser.isAdmin()) {
                                    setupVerificationTick(getUser);
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
                        Toast.makeText(UserProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkFollowers(int followersCount) {
        if (followersCount == 0) {
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

    private void setupVerificationTick(User getUser) {

        if (getUser.isAdmin() && !getUser.isSuperAdmin()) {
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
        } else if (getUser.isSuperAdmin()) {
            binding.verifiedAccountIcon.setImageResource(R.drawable.is_checked_super);
            binding.verifiedAccountIcon.setVisibility(View.VISIBLE);
            binding.verifiedAccountIcon.setOnClickListener(view -> new SimpleTooltip.Builder(this)
                    .anchorView(view)
                    .text("Super Admin")
                    .gravity(Gravity.TOP)
                    .textColor(Color.parseColor("#FFFFFF"))
                    .backgroundColor(Color.parseColor("#79018786"))
                    .arrowColor(Color.parseColor("#FF018786"))
                    .animated(true)
                    .transparentOverlay(false)
                    .build()
                    .show());
        } else {
            Log.d("ProfileFragment", "Not admin or superAdmin");
        }

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

    private void sendNotification() {
        mClient = new OkHttpClient();
        ArrayList<String> notification_id = new ArrayList<>();

        DatabaseReference databaseReference = firebaseDatabase.getReference().child("system")
                .child("notification")
                .child(userId)
                .child("notification_id");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                notification_id.add(String.valueOf(snapshot.getValue()));

                JSONArray regArray = new JSONArray(notification_id);
                sendMessage(regArray, "Congratulations", "You have been promoted to an Admin", "icon", "message");

            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {
                Toast.makeText(UserProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        finish();
        startActivity(getIntent());
    }

    @SuppressLint("StaticFieldLeak")
    public void sendMessage(final JSONArray recipients, final String title, final String body, final String icon, final String message) {

        new AsyncTask<String, String, String>() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            protected String doInBackground(String... params) {
                try {
                    JSONObject root = new JSONObject();
                    JSONObject notification = new JSONObject();
                    notification.put("body", body);
                    notification.put("title", title);
                    notification.put("icon", icon);

                    JSONObject data = new JSONObject();
                    data.put("message", message);
                    root.put("notification", notification);
                    root.put("data", data);
                    root.put("registration_ids", recipients);

                    String result = postToFCM(root.toString());
                    Log.d("TAG", "Result: " + result);
                    return result;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            @SuppressLint("StaticFieldLeak")
            @Override
            protected void onPostExecute(String result) {
                try {
                    JSONObject resultJson = new JSONObject(result);
                    int success, failure;
                    success = resultJson.getInt("success");
                    failure = resultJson.getInt("failure");
                } catch (@SuppressLint("StaticFieldLeak") JSONException e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    String postToFCM(String bodyString) throws IOException {
        RequestBody body = RequestBody.create(JSON, bodyString);
        Request request = new Request.Builder()
                .url(FCM_SEND)
                .post(body)
                .addHeader("Authorization", "key=AAAAaGx1iT8:APA91bEa9jz6wQVGu1lble4o2DRrVhm5b0omMS1T5F5it6s9KOl2TDoXYPOQxz3I1g6P37-APfKbfGnFYvZ1u0RaYexbgGsZ_ipFoFDIx3kbpBBW1GPJCgcDQMNriXjvMAC-fuf363Ek")
                .build();
        Response response = mClient.newCall(request).execute();
        return response.body().string();
    }

}