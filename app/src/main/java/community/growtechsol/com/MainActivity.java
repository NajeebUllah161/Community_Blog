package community.growtechsol.com;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.andreseko.SweetAlert.SweetAlertDialog;
import com.example.flatdialoglibrary.dialog.FlatDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

import community.growtechsol.com.activities.LoginActivity;
import community.growtechsol.com.databinding.ActivityMainBinding;
import community.growtechsol.com.fragments.AddPostFragment;
import community.growtechsol.com.fragments.HomeFragment;
import community.growtechsol.com.fragments.NotificationFragment;
import community.growtechsol.com.fragments.ProfileFragment;
import community.growtechsol.com.fragments.SearchFragment;
import community.growtechsol.com.models.VersionModel;
import community.growtechsol.com.utils.helper;

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
        checkAppUpdate();

        // When we open the application first
        // time the fragment should be shown to the user
        // in this case it is home fragment
        FragmentTransaction defaultFragmentTransaction = getSupportFragmentManager().beginTransaction();
        binding.toolbar.setVisibility(View.GONE);
        defaultFragmentTransaction.replace(R.id.container_framelayout, new HomeFragment());
        defaultFragmentTransaction.commit();

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
        binding.feedback.setOnClickListener(view -> {
            final FlatDialog flatDialog = new FlatDialog(MainActivity.this);
            flatDialog.setTitle("Feedback")
                    .setSubtitle("Write your feedback here")
                    .setLargeTextField("")
                    .setLargeTextFieldHint("Write here....")
                    .isCancelable(true)
                    .setBackgroundColor(Color.parseColor("#FF018786"))
                    .setFirstButtonColor(Color.parseColor("#FFFFFF"))
                    .setSecondButtonColor(Color.parseColor("#FFFFFF"))
                    .setFirstButtonTextColor(Color.parseColor("#000000"))
                    .setSecondButtonTextColor(Color.parseColor("#000000"))
                    .setFirstButtonText("SUBMIT")
                    .setSecondButtonText("CANCEL")
                    .withFirstButtonListner(view1 -> {
                        String feedbackText = flatDialog.getLargeTextField();
                        if (!feedbackText.isEmpty()) {
                            submitFeedback(feedbackText);
                        }
                        flatDialog.dismiss();
                        //submitFeedback();

                    })
                    .withSecondButtonListner(view12 -> flatDialog.dismiss())
                    .show();
        });

    }

    private void submitFeedback(String feedbackText) {
        Date date = new Date();
        String todayDate = (String) DateFormat.format("yyyy-MM-dd", date);
        String timeNow = (String) DateFormat.format("hh:mm:ss", date);

        FirebaseDatabase.getInstance().getReference()
                .child("userFeedbacks")
                .child(auth.getCurrentUser().getUid())
                .child(todayDate)
                .child(timeNow)
                .child("feedbackText")
                .setValue(feedbackText)
                .addOnSuccessListener(unused -> Toast.makeText(MainActivity.this, "Feedback submitted!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show());
    }

    private void checkAppUpdate() {
        if (helper.isInternetAvailable(this)) {
            FirebaseDatabase.getInstance().getReference().child("vcs")
                    .child("version").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        VersionModel versionModel = snapshot.getValue(VersionModel.class);
                        Log.d("VersionTag", "Version" + versionModel.getVersion() + " Severity" + versionModel.getSeverity());
                        try {
                            String currentVersion = MainActivity.this.getPackageManager().getPackageInfo(MainActivity.this.getPackageName(), 0).versionName;
                            if (!currentVersion.equals(versionModel.getVersion() + "")) {
                                if (!versionModel.getSeverity().contains("High")) {
                                    new SweetAlertDialog(MainActivity.this, SweetAlertDialog.CUSTOM_IMAGE_TYPE)
                                            .setTitleText("Update Available")
                                            .setCustomImage(getDrawable(R.drawable.ic_update))
                                            .setContentText("Current Version: " + currentVersion + "\nLatest Version: " + versionModel.getVersion() + "\nPriority: " + versionModel.getSeverity())
                                            .setConfirmText("Update Now")
                                            .setCancelText("Later")
                                            .setConfirmClickListener(sDialog -> {
                                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=community.growtechsol.com")));
                                                sDialog.dismissWithAnimation();
                                            })
                                            .setCancelClickListener(SweetAlertDialog::cancel)
                                            .show();
                                } else {
                                    SweetAlertDialog dialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.CUSTOM_IMAGE_TYPE)
                                            .setTitleText("Update Available")
                                            .setCustomImage(getDrawable(R.drawable.ic_update))
                                            .setContentText("Current Version: " + currentVersion + "\nLatest Version: " + versionModel.getVersion() + "\nPriority: " + versionModel.getSeverity())
                                            .setConfirmText("Update")
                                            .setConfirmClickListener(sDialog -> {
                                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=community.growtechsol.com")));
                                                sDialog.dismissWithAnimation();
                                            });
                                    dialog.setCancelable(false);
                                    dialog.setCanceledOnTouchOutside(false);
                                    dialog.show();
                                }
                            }

                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }

                    }
                }

                @Override
                public void onCancelled(@NonNull @NotNull DatabaseError error) {

                }
            });

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAppUpdate();
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
    public void onBackPressed() {
        new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Exit Application?")
                .setContentText("Do you want to close this application?")
                .setConfirmText("Yes")
                .setConfirmClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();
                    finishAffinity();
                })
                .setCancelButton("No", SweetAlertDialog::dismissWithAnimation)
                .show();
    }

}