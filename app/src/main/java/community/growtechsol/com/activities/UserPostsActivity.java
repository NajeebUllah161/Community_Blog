package community.growtechsol.com.activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.OnMenuItemClickListener;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import community.growtechsol.com.R;
import community.growtechsol.com.adapters.PostAdapter;
import community.growtechsol.com.databinding.ActivityUserPostsBinding;
import community.growtechsol.com.models.Notification;
import community.growtechsol.com.models.Post;
import community.growtechsol.com.models.Story;
import community.growtechsol.com.models.User;

import static community.growtechsol.com.utils.helper.shared;

public class UserPostsActivity extends AppCompatActivity {

    ArrayList<Story> storyArrayList = new ArrayList<>();
    ArrayList<Post> postList = new ArrayList<>();
    ArrayList<Post> postListSolved = new ArrayList<>();
    ArrayList<Post> postListUnSolved = new ArrayList<>();
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    ActivityResultLauncher<String> galleryLauncher;
    ProgressDialog dialog;
    ActivityUserPostsBinding binding;
    PowerMenu powerMenu;
    PostAdapter postAdapter;
    String userId;
    String postFilterType = "all";
    private final OnMenuItemClickListener<PowerMenuItem> onMenuItemClickListener = new OnMenuItemClickListener<PowerMenuItem>() {
        @Override
        public void onItemClick(int position, PowerMenuItem item) {
            powerMenu.setSelectedPosition(position);
            if (item.getTitle().equals("Solved")) {
                postAdapter = new PostAdapter(postListSolved, UserPostsActivity.this);
                binding.dashboardRv.setAdapter(postAdapter);
                binding.dashboardRv.hideShimmerAdapter();
                loadProfileImg();
                postAdapter.notifyDataSetChanged();
                postFilterType = "solved";
            } else if (item.getTitle().equals("UnSolved")) {
                postAdapter = new PostAdapter(postListUnSolved, UserPostsActivity.this);
                binding.dashboardRv.setAdapter(postAdapter);
                binding.dashboardRv.hideShimmerAdapter();
                loadProfileImg();
                postAdapter.notifyDataSetChanged();
                postFilterType = "unsolved";
            } else {
                postAdapter = new PostAdapter(postList, UserPostsActivity.this);
                binding.dashboardRv.setAdapter(postAdapter);
                binding.dashboardRv.hideShimmerAdapter();
                loadProfileImg();
                postAdapter.notifyDataSetChanged();
                postFilterType = "all";
            }
            powerMenu.dismiss();
        }
    };
    Post postModel;
    List<String> cropList = new ArrayList<>();
    //private DatabaseReference mDatabase;
    private String cropName = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserPostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userId = getIntent().getStringExtra("userId");

        dialog = new ProgressDialog(UserPostsActivity.this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        setupPowerMenu();
        setupFunctions();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (shared) {
            shared = false;
            FirebaseDatabase.getInstance().getReference()
                    .child("posts/" + postModel.getPostId() + "/postShares")
                    .setValue(ServerValue.increment(1))
                    .addOnFailureListener(e -> {
                        Toast.makeText(UserPostsActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

            Notification notification = new Notification();

            notification.setNotificationBy(FirebaseAuth
                    .getInstance()
                    .getCurrentUser()
                    .getUid());
            notification.setNotificaitonAt(new Date().getTime());
            notification.setPostId(postModel.getPostId());
            notification.setPostedBy(postModel.getPostedBy());
            notification.setNotificationType("share");

            FirebaseDatabase.getInstance().getReference()
                    .child("notification")
                    .child(postModel.getPostedBy())
                    .push()
                    .setValue(notification).addOnSuccessListener(unused -> {
            }).addOnFailureListener(e -> {
                Toast.makeText(UserPostsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupFunctions() {
        setupAdapters();
        setupEventListeners();
        setupCropList();
    }

    private void setupCropList() {

        String[] your_array = getResources().getStringArray(R.array.crop_list);

        cropList.addAll(Arrays.asList(your_array));
        SmartMaterialSpinner<String> crop = binding.cropHome;
        crop.setItem(cropList);

        crop.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                cropName = cropList.get(position);
                postAdapter = new PostAdapter(postList, UserPostsActivity.this);
                LinearLayoutManager layoutManager = new LinearLayoutManager(UserPostsActivity.this, LinearLayoutManager.VERTICAL, true);
                binding.dashboardRv.setLayoutManager(layoutManager);
                binding.dashboardRv.setNestedScrollingEnabled(false);

                firebaseDatabase.getReference().child("posts").addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        postList.clear();
                        postListSolved.clear();
                        postListUnSolved.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Post post = dataSnapshot.getValue(Post.class);
                            post.setPostId(dataSnapshot.getKey());
                            if (post.getPostedBy().equals(userId)) {
                                if (post.getCropName().equals(cropName)) {
                                    postList.add(post);
                                    if (post.isSolved()) {
                                        postListSolved.add(post);
                                    } else {
                                        postListUnSolved.add(post);
                                    }
                                }
                            }
                        }
                        binding.dashboardRv.setAdapter(postAdapter);
                        binding.dashboardRv.hideShimmerAdapter();
                        loadProfileImg();
                        postAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserPostsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void setupEventListeners() {

        binding.addStoryImg.setOnClickListener(view1 -> galleryLauncher.launch("image/*"));

        binding.filter.setOnClickListener(view -> powerMenu.showAsDropDown(view));

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });

        binding.profileImage.setOnClickListener(view -> finish());

    }

    private void setupAdapters() {

        binding.dashboardRv.showShimmerAdapter();

        clearArrayLists();

        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Story Uploading");
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);

        loadUserPosts();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> loadUserPosts());

        postAdapter.setOnItemClickListner((isOpen, postModel) -> {
            Toast.makeText(UserPostsActivity.this, "Clicked", Toast.LENGTH_LONG).show();
            this.postModel = postModel;
        });
    }

    private void loadUserPosts() {
        postAdapter = new PostAdapter(postList, UserPostsActivity.this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(UserPostsActivity.this, LinearLayoutManager.VERTICAL, true);
        binding.dashboardRv.setLayoutManager(layoutManager);
        binding.dashboardRv.setNestedScrollingEnabled(false);

        firebaseDatabase.getReference().child("posts").addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                postListSolved.clear();
                postListUnSolved.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post post = dataSnapshot.getValue(Post.class);
                    post.setPostId(dataSnapshot.getKey());
                    if (post.getPostedBy().equals(userId)) {
                        postList.add(post);
                        if (post.isSolved()) {
                            postListSolved.add(post);
                        } else {
                            postListUnSolved.add(post);
                        }
                    }
                }
                binding.dashboardRv.setAdapter(postAdapter);
                binding.dashboardRv.hideShimmerAdapter();
                loadProfileImg();
                postAdapter.notifyDataSetChanged();
                if (binding.swipeRefreshLayout.isRefreshing()) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserPostsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void filterList(String newText) {
        ArrayList<Post> filteredList = new ArrayList<>();

        if (postFilterType.equals("solved")) {
            for (Post post : postListSolved) {
                if (post.getPostTitle().toLowerCase().contains(newText.toLowerCase())) {
                    filteredList.add(post);
                }
            }

        } else if (postFilterType.equals("unsolved")) {
            for (Post post : postListUnSolved) {
                if (post.getPostTitle().toLowerCase().contains(newText.toLowerCase())) {
                    filteredList.add(post);
                }
            }

        } else {
            for (Post post : postList) {
                if (post.getPostTitle().toLowerCase().contains(newText.toLowerCase())) {
                    filteredList.add(post);
                }
            }
        }

        if (filteredList.isEmpty()) {
            Toast.makeText(UserPostsActivity.this, "Not Data Found!", Toast.LENGTH_SHORT).show();
        } else {
            postAdapter.setFilteredList(filteredList);
        }
    }

    private void setupPowerMenu() {

        List<PowerMenuItem> list = new ArrayList<>();
        list.add(new PowerMenuItem("All"));
        list.add(new PowerMenuItem("Solved"));
        list.add(new PowerMenuItem("UnSolved"));

        powerMenu = new PowerMenu.Builder(UserPostsActivity.this)
                .addItemList(list)
                .setAnimation(MenuAnimation.SHOWUP_BOTTOM_RIGHT)
                .setMenuRadius(10f)
                .setMenuShadow(10f)
                .setTextColor(ContextCompat.getColor(UserPostsActivity.this, R.color.teal_700))
                .setTextGravity(Gravity.START)
                .setTextTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
                .setSelectedTextColor(Color.WHITE)
                .setMenuColor(Color.WHITE)
                .setSelectedMenuColor(ContextCompat.getColor(UserPostsActivity.this, R.color.black))
                .setOnMenuItemClickListener(onMenuItemClickListener)
                .build();
    }

    private void loadProfileImg() {

        firebaseDatabase
                .getReference()
                .child("Users/" + userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    Picasso.get()
                            .load(user.getProfileImage())
                            .placeholder(R.drawable.placeholder)
                            .into(binding.profileImage);

                } else {
                    Toast.makeText(UserPostsActivity.this, "No user exists", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserPostsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearArrayLists() {
        storyArrayList.clear();
        postList.clear();
    }

}