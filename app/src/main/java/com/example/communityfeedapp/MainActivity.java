package com.example.communityfeedapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.communityfeedapp.activities.LoginActivity;
import com.example.communityfeedapp.databinding.ActivityMainBinding;
import com.example.communityfeedapp.fragments.AddPostFragment;
import com.example.communityfeedapp.fragments.HomeFragment;
import com.example.communityfeedapp.fragments.NotificationFragment;
import com.example.communityfeedapp.fragments.ProfileFragment;
import com.example.communityfeedapp.fragments.SearchFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    FirebaseAuth auth = FirebaseAuth.getInstance();

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        MainActivity.this.setTitle("My Profile");
        // When we open the application first
        // time the fragment should be shown to the user
        // in this case it is home fragment
        FragmentTransaction defaultFragmentTransaction = getSupportFragmentManager().beginTransaction();
        binding.toolbar.setVisibility(View.GONE);
        defaultFragmentTransaction.replace(R.id.container_framelayout, new HomeFragment());
        defaultFragmentTransaction.commit();

        Log.d("CheckingErr","onCreateMainActivity");
        binding.bottomNavigationBar.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

            switch (itemId) {
                case R.id.item_home:
                    binding.toolbar.setVisibility(View.GONE);
                    fragmentTransaction.replace(R.id.container_framelayout, new HomeFragment());
                    break;
                case R.id.item_notification:
                    binding.toolbar.setVisibility(View.GONE);
                    fragmentTransaction.replace(R.id.container_framelayout, new NotificationFragment());
                    break;
                case R.id.item_add_post:
                    binding.toolbar.setVisibility(View.GONE);
                    fragmentTransaction.replace(R.id.container_framelayout, new AddPostFragment());
                    break;
                case R.id.item_search:
                    binding.toolbar.setVisibility(View.GONE);
                    fragmentTransaction.replace(R.id.container_framelayout, new SearchFragment());
                    break;
                case R.id.item_userprofile:
                    binding.toolbar.setVisibility(View.VISIBLE);
                    fragmentTransaction.replace(R.id.container_framelayout, new ProfileFragment());
                    break;
            }
            fragmentTransaction.commit();
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.setting:
                auth.signOut();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("CheckingErr","onResumeMainActivity");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("CheckingErr","onPauseMainActivity");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("CheckingErr","onDestroyMainActivity");
        //FirebaseDatabase.getInstance().goOffline();
        finishAffinity();
    }
}