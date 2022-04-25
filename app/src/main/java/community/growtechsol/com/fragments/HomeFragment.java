package community.growtechsol.com.fragments;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.OnMenuItemClickListener;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import community.growtechsol.com.R;
import community.growtechsol.com.adapters.PostAdapter;
import community.growtechsol.com.adapters.StoryAdapter;
import community.growtechsol.com.databinding.FragmentHomeBinding;
import community.growtechsol.com.models.Post;
import community.growtechsol.com.models.Story;
import community.growtechsol.com.models.User;
import community.growtechsol.com.models.UserStories;

public class HomeFragment extends Fragment {

    ArrayList<Story> storyArrayList = new ArrayList<>();
    ArrayList<Post> postList = new ArrayList<>();
    ArrayList<Post> postListSolved = new ArrayList<>();
    ArrayList<Post> postListUnSolved = new ArrayList<>();
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    FirebaseAuth auth;
    ActivityResultLauncher<String> galleryLauncher;
    ProgressDialog dialog;
    FragmentHomeBinding binding;
    PowerMenu powerMenu;
    PostAdapter postAdapter;
    String postFilterType = "all";

    private final OnMenuItemClickListener<PowerMenuItem> onMenuItemClickListener = new OnMenuItemClickListener<PowerMenuItem>() {
        @Override
        public void onItemClick(int position, PowerMenuItem item) {
            powerMenu.setSelectedPosition(position);
            if (item.getTitle().equals("Solved")) {
                postAdapter = new PostAdapter(postListSolved, getContext());
                binding.dashboardRv.setAdapter(postAdapter);
                binding.dashboardRv.hideShimmerAdapter();
                loadProfileImg();
                postAdapter.notifyDataSetChanged();
                postFilterType = "solved";
            } else if (item.getTitle().equals("UnSolved")) {
                postAdapter = new PostAdapter(postListUnSolved, getContext());
                binding.dashboardRv.setAdapter(postAdapter);
                binding.dashboardRv.hideShimmerAdapter();
                loadProfileImg();
                postAdapter.notifyDataSetChanged();
                postFilterType = "unsolved";
            } else {
                postAdapter = new PostAdapter(postList, getContext());
                binding.dashboardRv.setAdapter(postAdapter);
                binding.dashboardRv.hideShimmerAdapter();
                loadProfileImg();
                postAdapter.notifyDataSetChanged();
                postFilterType = "all";
            }
            powerMenu.dismiss();
        }
    };
    private DatabaseReference mDatabase;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialog = new ProgressDialog(getContext());
        firebaseDatabase = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        mDatabase = FirebaseDatabase.getInstance().getReferenceFromUrl("https://communityfeedapp-default-rtdb.firebaseio.com/").getRef();
        setFireBaseNotificationId();
        setupPowerMenu();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        setupFunctions();


        return binding.getRoot();
    }

    private void setupFunctions() {

        setupAdapters();
        setupEventListeners();
        setupFloatingMenu();

    }

    private void setupFloatingMenu() {

    }

    private void setupEventListeners() {

        binding.addStoryImg.setOnClickListener(view1 -> galleryLauncher.launch("image/*"));

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
            binding.addStoryImg.setImageURI(result);
            dialog.show();
            final StorageReference reference = firebaseStorage.getReference()
                    .child("stories")
                    .child(FirebaseAuth.getInstance().getUid())
                    .child(new Date().getTime() + "");
            reference.putFile(result).addOnSuccessListener(taskSnapshot -> reference.getDownloadUrl().addOnSuccessListener(uri -> {
                Story story = new Story();
                story.setStoryAt(new Date().getTime());
                firebaseDatabase.getReference()
                        .child("stories")
                        .child(FirebaseAuth.getInstance().getUid())
                        .child("postedBy")
                        .setValue(story.getStoryAt()).addOnSuccessListener(unused -> {
                    UserStories stories = new UserStories(uri.toString(), story.getStoryAt());

                    firebaseDatabase.getReference()
                            .child("stories")
                            .child(FirebaseAuth.getInstance().getUid())
                            .child("userStories")
                            .push()
                            .setValue(stories);
                    dialog.dismiss();

                }).addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                dialog.dismiss();
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            })).addOnFailureListener(e -> {
                dialog.dismiss();
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

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

        binding.profileImage.setOnClickListener(view -> ((BottomNavigationView) getActivity().findViewById(R.id.bottom_navigation_bar)).setSelectedItemId(R.id.item_userprofile));

    }

    private void setupAdapters() {

        binding.dashboardRv.showShimmerAdapter();

        // clear arraylists to avoid duplication of data
        clearArrayLists();

        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Story Uploading");
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);

        // Setting up Story RecyclerView

        StoryAdapter adapter = new StoryAdapter(storyArrayList, getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, true);
        binding.storyRv.setLayoutManager(linearLayoutManager);
        binding.storyRv.setNestedScrollingEnabled(false);
        binding.storyRv.setAdapter(adapter);

        firebaseDatabase.getReference()
                .child("stories")
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            storyArrayList.clear();
                            for (DataSnapshot storySnapshot : snapshot.getChildren()) {
                                Story story = new Story();
                                story.setStoryBy(storySnapshot.getKey());
                                story.setStoryAt(storySnapshot.child("postedBy").getValue(Long.class));

                                ArrayList<UserStories> stories = new ArrayList<>();
                                for (DataSnapshot snapshot1 : storySnapshot.child("userStories").getChildren()) {
                                    UserStories userStories = snapshot1.getValue(UserStories.class);
                                    stories.add(userStories);
                                }
                                story.setStories(stories);
                                storyArrayList.add(story);
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Setting up Dashboard RecyclerView

        postAdapter = new PostAdapter(postList, getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, true);
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
                    //if (post.isAllowed()) {
                    postList.add(post);
                    if (post.isSolved()) {
                        //Log.d("Solved", "solved");
                        postListSolved.add(post);
                    } else {
                        postListUnSolved.add(post);
                        //Log.d("Solved", "not");
                    }
                    //}
                }
                binding.dashboardRv.setAdapter(postAdapter);
                binding.dashboardRv.hideShimmerAdapter();
                loadProfileImg();
                postAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), "Not Data Found!", Toast.LENGTH_SHORT).show();
        } else {
            postAdapter.setFilteredList(filteredList);
        }
    }

    private void setFireBaseNotificationId() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult().getToken();
                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("notification_id", token);
                mDatabase.child("system").child("notification").child(auth.getCurrentUser().getUid()).setValue(hashMap);
            }
        });
    }

    private void setupPowerMenu() {

        List<PowerMenuItem> list = new ArrayList<>();
        list.add(new PowerMenuItem("All"));
        list.add(new PowerMenuItem("Solved"));
        list.add(new PowerMenuItem("UnSolved"));

        powerMenu = new PowerMenu.Builder(getContext())
                .addItemList(list) // list has "Novel", "Poetry", "Art"
                .setAnimation(MenuAnimation.SHOWUP_BOTTOM_RIGHT) // Animation start point (TOP | LEFT).
                .setMenuRadius(10f) // sets the corner radius.
                .setMenuShadow(10f) // sets the shadow.
                .setTextColor(ContextCompat.getColor(getContext(), R.color.teal_700))
                .setTextGravity(Gravity.LEFT)
                .setTextTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
                .setSelectedTextColor(Color.WHITE)
                .setMenuColor(Color.WHITE)
                .setSelectedMenuColor(ContextCompat.getColor(getContext(), R.color.black))
                .setOnMenuItemClickListener(onMenuItemClickListener)
                .build();
    }

    private void loadProfileImg() {

        firebaseDatabase
                .getReference()
                .child("Users/" + Objects.requireNonNull(auth.getCurrentUser()).getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    Picasso.get()
                            .load(user.getProfileImage())
                            .placeholder(R.drawable.placeholder)
                            .into(binding.profileImage);

                } else {
                    Toast.makeText(getContext(), "No user exists", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearArrayLists() {
        storyArrayList.clear();
        postList.clear();
    }
}
