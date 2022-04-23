package community.growtechsol.com.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hendraanggrian.appcompat.widget.Mention;
import com.hendraanggrian.appcompat.widget.MentionArrayAdapter;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import community.growtechsol.com.R;
import community.growtechsol.com.adapters.CommentAdapter;
import community.growtechsol.com.databinding.ActivityCommentBinding;
import community.growtechsol.com.models.Comment;
import community.growtechsol.com.models.Notification;
import community.growtechsol.com.models.Post;
import community.growtechsol.com.models.User;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static community.growtechsol.com.fragments.AddPostFragment.RequestPermissionCode;

public class CommentActivity extends AppCompatActivity {

    ActivityCommentBinding binding;
    Intent intent;
    String postId, postedBy;
    boolean isSolved, isAdmin;
    FirebaseDatabase firebaseDatabase;
    FirebaseAuth auth;
    ArrayList<Comment> list = new ArrayList<>();
    ProgressDialog progressDialog;
    ArrayAdapter<Mention> mentionAdapter;

    //Recording
    String AudioSavePathInDevice = null;
    MediaRecorder mediaRecorder;
    Random random;
    String RandomAudioFileName = "ABCDEFGHIJKLMNOP";
    MediaPlayer mediaPlayer;
    int length;
    String downloadedRecordingLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCommentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        intent = getIntent();

        //audio recording
        random = new Random();
        progressDialog = new ProgressDialog(this);

        //mentionAdapter
        mentionAdapter = new MentionArrayAdapter<>(this);

        // Setting up toolbar
        setSupportActionBar(binding.toolbarCommentActivity);
        CommentActivity.this.setTitle("Comments");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setButtonDisabled();

        firebaseDatabase = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        postId = intent.getStringExtra("postId");
        postedBy = intent.getStringExtra("postedBy");
        isSolved = intent.getBooleanExtra("isSolved", false);
        isAdmin = intent.getBooleanExtra("isAdmin", false);

        setupFunctions();

    }

    private void setupFunctions() {
        setupProgressDialogue();
        loadDataFromFirebase();
        setupAdapters();
        setupEventListeners();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupEventListeners() {

        binding.postCommentRecording.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRecording();
                    break;
                case MotionEvent.ACTION_UP:
                    stopRecording();
                    break;
            }
            return true;
        });

        binding.play.setOnClickListener(view -> playRecording());

        binding.pause.setOnClickListener(view -> pauseRecording());

        binding.resume.setOnClickListener(view -> resumeRecording());

        binding.removeRecording.setOnClickListener(view -> removeRecording());

        binding.commentEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String commentData = binding.commentEt.getText().toString();
                if (!commentData.isEmpty() || AudioSavePathInDevice != null) {
                    setButtonEnabled();
                } else {
                    setButtonDisabled();
                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        binding.postCommentBtn.setOnClickListener(view -> {

            ArrayList<String> mentionList = new ArrayList<>();
            ArrayList<String> mentionListFiltered = new ArrayList<>();
            ArrayList<String> mentionListFinal;
            String commentData = binding.commentEt.getText().toString();
            String[] words = commentData.split(" ");
            for (String word : words)
                if (word.contains("@")) {
                    String finalWords = word.substring(word.lastIndexOf("@"));
                    mentionList.add(finalWords);

                }

            for (int i = 0; i < mentionList.size(); i++) {
                //Log.d("MentionList", mentionList.get(i).replaceAll("@", "") + " " + i);
                mentionListFiltered.add(mentionList.get(i).replaceAll("@", ""));
            }

            mentionListFinal = removeDuplicates(mentionListFiltered);

            progressDialog.show();

            Comment comment = new Comment();
            comment.setCommentBody(binding.commentEt.getText().toString());
            comment.setCommentedAt(new Date().getTime());
            comment.setCommentedBy(FirebaseAuth.getInstance().getCurrentUser().getUid());

            final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("commentRecordings")
                    .child(comment.getCommentedAt() + "")
                    .child(auth.getCurrentUser().getUid());

            if (AudioSavePathInDevice != null) {
                Uri recordingUri = Uri.fromFile(new File(AudioSavePathInDevice));
                storageReference.putFile(recordingUri).addOnSuccessListener(taskSnapshot -> {
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        uploadCommentWithRecording(uri, comment, mentionListFinal);
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to download audio Url", Toast.LENGTH_SHORT).show();
                    });
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to upload comment audio due to" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } else {
                uploadCommentWithoutRecording(comment, mentionListFinal);
            }

        });
    }

    private void setupAdapters() {

        CommentAdapter commentAdapter = new CommentAdapter(this, list, postId, postedBy, isSolved, isAdmin);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        binding.commentRv.setLayoutManager(linearLayoutManager);
        binding.commentRv.setAdapter(commentAdapter);

        firebaseDatabase.getReference()
                .child("posts/" + postId + "/comments")
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        list.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Comment comment = dataSnapshot.getValue(Comment.class);
                            list.add(comment);
                        }
                        commentAdapter.notifyDataSetChanged();

                        FirebaseDatabase.getInstance()
                                .getReference()
                                .child("Users").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                ArrayList<String> commenterNames = new ArrayList<>();
                                ArrayList<String> commenterDesignation = new ArrayList<>();
                                ArrayList<String> commenterPhoto = new ArrayList<>();
                                int size = list.size();

                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                    User user = dataSnapshot.getValue(User.class);

                                    for (int i = 0; i < size; i++) {
                                        Comment comment = list.get(i);
                                        if (dataSnapshot.getKey().equals(comment.getCommentedBy())) {
                                            commenterNames.add(user.getName());
                                            commenterDesignation.add(user.getProfession());
                                            commenterPhoto.add(user.getProfileImage());
                                        }
                                    }

                                    if (dataSnapshot.getKey().equals(postedBy)) {
                                        commenterNames.add(user.getName());
                                        commenterDesignation.add(user.getProfession());
                                        commenterPhoto.add(user.getProfileImage());
                                    }
                                }

                                ArrayList<String> commenterNamesList = removeDuplicates(commenterNames);
                                //ArrayList<String> commenterDesignationList = removeDuplicates(commenterDesignation);
                                ArrayList<String> commenterPhotoList = removeDuplicates(commenterPhoto);

                                int incrementedLength = commenterNamesList.size() - commenterPhotoList.size();
                                for (int i = 0; i < incrementedLength; i++) {
                                    commenterPhotoList.add(null);
                                }
                                mentionAdapter.clear();
                                for (int i = 0; i < commenterNamesList.size(); i++) {
                                    if (commenterPhotoList.get(i) != null) {
                                        mentionAdapter.add(new Mention(commenterNamesList.get(i).replaceAll(" ", ""), "", commenterPhotoList.get(i)));
                                    } else {
                                        mentionAdapter.add(new Mention(commenterNamesList.get(i).replaceAll(" ", ""), "", R.drawable.placeholder));
                                    }
                                }

                                binding.commentEt.setMentionAdapter(mentionAdapter);
                                binding.commentEt.setMentionEnabled(true);
                                //binding.commentEt.setOnMentionClickListener((view, text) -> Toast.makeText(CommentActivity.this, text, Toast.LENGTH_SHORT).show());
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(CommentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CommentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void loadDataFromFirebase() {

        firebaseDatabase.getReference()
                .child("posts/" + postId)
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint({"SetTextI18n", "CheckResult"})
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Post post = snapshot.getValue(Post.class);
                        if (post != null) {

                            RequestOptions requestOptions = new RequestOptions();
                            requestOptions.placeholder(R.drawable.placeholder);
                            if (!CommentActivity.this.isFinishing()) {
                                Glide.with(CommentActivity.this)
                                        .setDefaultRequestOptions(requestOptions)
                                        .load(post.getPostImage()).into(binding.imgCommentScreen);
                            }

                            binding.headerCommentScreen.setText(Html.fromHtml("<b>" + post.getPostTitle() + "</b>"));
                            binding.descCommentScreen.setText(post.getPostDescription());
                            binding.like.setText(post.getPostLikes() + "");
                            binding.comment.setText(post.getCommentCount() + "");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CommentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        firebaseDatabase.getReference()
                .child("Users/" + postedBy)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        Picasso.get()
                                .load(user.getProfileImage())
                                .placeholder(R.drawable.placeholder)
                                .into(binding.profileImgComment);
                        binding.nameCommentScreen.setText(user.getName());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CommentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        firebaseDatabase.getReference()
                .child("posts/" + postId + "/likes/" + auth.getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.getValue() != null) {
                            Log.d("Snapshot", String.valueOf(snapshot.getValue()));
                            binding.like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_heart_filled, 0, 0, 0);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CommentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void setupProgressDialogue() {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Comment Posting");
        progressDialog.setMessage("Please Wait...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
    }

    private void uploadCommentWithoutRecording(Comment comment, ArrayList<String> mentionListFinal) {

        firebaseDatabase.getReference()
                .child("posts/" + postId + "/comments")
                .child(comment.getCommentedAt() + "")
                .setValue(comment)
                .addOnSuccessListener(unused -> {
                            progressDialog.dismiss();
                            binding.commentEt.setText("");
                            binding.commentEt.setSelection(0);
                            removeRecording();
                            Toast.makeText(this, "Commented Successfully!", Toast.LENGTH_SHORT).show();

                            Notification notification = new Notification();
                            notification.setNotificationBy(FirebaseAuth.getInstance().getCurrentUser().getUid());
                            notification.setNotificaitonAt(new Date().getTime());
                            notification.setPostId(postId);
                            notification.setPostedBy(postedBy);
                            notification.setNotificationType("comment");

                            FirebaseDatabase.getInstance().getReference()
                                    .child("notification")
                                    .child(postedBy)
                                    .push()
                                    .setValue(notification);
                            hideKeyboard(CommentActivity.this);

                            sendNotificationToMentionList(mentionListFinal);

                        }
                ).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();

        });

        firebaseDatabase.getReference()
                .child("posts/" + postId + "/commentCount")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int commentCount = 0;
                        if (snapshot.exists()) {
                            commentCount = snapshot.getValue(Integer.class);

                        }
                        firebaseDatabase.getReference()
                                .child("posts/" + postId + "/commentCount")
                                .setValue(commentCount + 1)
                                .addOnSuccessListener(unused1 -> {
                                    //Log.d("CommentActivity", unused1.toString() + "");

                                }).addOnFailureListener(e -> {
                            Toast.makeText(CommentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CommentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadCommentWithRecording(Uri uri, Comment comment, ArrayList<String> mentionListFinal) {

        comment.setCommentRecording(uri.toString());

        firebaseDatabase.getReference()
                .child("posts/" + postId + "/comments")
                .child(comment.getCommentedAt() + "")
                .setValue(comment)
                .addOnSuccessListener(unused -> {
                            progressDialog.dismiss();
                            binding.commentEt.setText("");
                            binding.commentEt.setSelection(0);

                            removeRecording();
                            Toast.makeText(CommentActivity.this, "Commented Successfully!", Toast.LENGTH_SHORT).show();

                            Notification notification = new Notification();
                            notification.setNotificationBy(FirebaseAuth.getInstance().getCurrentUser().getUid());
                            notification.setNotificaitonAt(new Date().getTime());
                            notification.setPostId(postId);
                            notification.setPostedBy(postedBy);
                            notification.setNotificationType("comment");

                            FirebaseDatabase.getInstance().getReference()
                                    .child("notification")
                                    .child(postedBy)
                                    .push()
                                    .setValue(notification);
                            hideKeyboard(CommentActivity.this);

                            sendNotificationToMentionList(mentionListFinal);

                        }
                ).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        });

        firebaseDatabase.getReference()
                .child("posts/" + postId + "/commentCount")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int commentCount = 0;
                        if (snapshot.exists()) {
                            commentCount = snapshot.getValue(Integer.class);

                        }
                        firebaseDatabase.getReference()
                                .child("posts/" + postId + "/commentCount")
                                .setValue(commentCount + 1)
                                .addOnSuccessListener(unused1 -> {
                                    Log.d("CommentActivity", "Comment count incremented");

                                }).addOnFailureListener(e -> {
                            Toast.makeText(CommentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CommentActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendNotificationToMentionList(ArrayList<String> mentionListFinal) {

        ArrayList<String> mentionedUsersIds = new ArrayList<>();

        firebaseDatabase.getReference().child("Users")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            User user = dataSnapshot.getValue(User.class);
                            for (int i = 0; i < mentionListFinal.size(); i++) {
                                if (user.getName().replaceAll(" ", "").equals(mentionListFinal.get(i))) {
                                    mentionedUsersIds.add(dataSnapshot.getKey());
                                }
                            }
                        }

                        for (int i = 0; i < mentionedUsersIds.size(); i++) {
                            Log.d("UsersID's", mentionedUsersIds.get(i));
                            sendNotificationsToMentioned(mentionedUsersIds.get(i));

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {

                    }
                });
    }

    private void sendNotificationsToMentioned(String userId) {
        Notification notification = new Notification();
        notification.setNotificationBy(FirebaseAuth.getInstance().getCurrentUser().getUid());
        notification.setNotificaitonAt(new Date().getTime());
        notification.setPostId(postId);
        notification.setPostedBy(postedBy);
        notification.setNotificationType("mention");

        FirebaseDatabase.getInstance().getReference()
                .child("notification")
                .child(userId)
                .push()
                .setValue(notification);
    }

    private void removeRecording() {
        binding.audioContainer.setVisibility(View.GONE);
        binding.removeRecording.setVisibility(View.GONE);
        mediaPlayer = null;
        AudioSavePathInDevice = null;
        downloadedRecordingLocation = null;

        String commentData = binding.commentEt.getText().toString();

        if (commentData.isEmpty()) {
            setButtonDisabled();
        }

    }

    private void resumeRecording() {
        if (mediaPlayer != null) {

            mediaPlayer.seekTo(length);
            mediaPlayer.start();
            binding.resume.setVisibility(View.GONE);
            binding.pause.setVisibility(View.VISIBLE);

            mediaPlayer.setOnCompletionListener(mediaPlayer -> {
                binding.play.setVisibility(View.VISIBLE);
                binding.pause.setVisibility(View.GONE);
            });
        } else {
            Log.d("AddPostFragment", "Audio is not recorded");
        }
    }

    private void pauseRecording() {
        if (mediaPlayer != null) {
            binding.pause.setVisibility(View.GONE);
            binding.resume.setVisibility(View.VISIBLE);
            mediaPlayer.pause();
            length = mediaPlayer.getCurrentPosition();
            mediaPlayer.setOnCompletionListener(mediaPlayer -> {
                binding.play.setVisibility(View.VISIBLE);
                binding.resume.setVisibility(View.GONE);
            });
        } else {
            Log.d("AddPostFragment", "Audio is not recorded");
        }
    }

    private void playRecording() {

        binding.play.setVisibility(View.GONE);
        binding.pause.setVisibility(View.VISIBLE);

        if (AudioSavePathInDevice != null) {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(AudioSavePathInDevice);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mediaPlayer -> {
                binding.pause.setVisibility(View.GONE);
                binding.play.setVisibility(View.VISIBLE);
            });
        } else {

            Log.d("CommentActivity", "Audio is not recorded");
        }
    }

    private void stopRecording() {

        boolean isAudio = true;
        if (mediaRecorder != null) {
            binding.recordingStatus.setVisibility(View.INVISIBLE);
            binding.play.setVisibility(View.VISIBLE);
            //Log.d("Length", String.valueOf(length));
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                //Log.d("CheckAudio", "check" + AudioSavePathInDevice + " " + mediaPlayer + " " + mediaRecorder);
                isAudio = false;
            }
            if (isAudio) {
                Toast.makeText(this, "Recording Completed", Toast.LENGTH_SHORT).show();
                setButtonEnabled();
            } else {
                Toast.makeText(this, "Error! Record Again..", Toast.LENGTH_SHORT).show();
                AudioSavePathInDevice = null;
                mediaRecorder = null;
                binding.audioContainer.setVisibility(View.GONE);
                binding.removeRecording.setVisibility(View.GONE);
            }
        } else {
            Log.d("AddPostFragment", "Audio is not recorded");
            binding.audioContainer.setVisibility(View.GONE);
            binding.removeRecording.setVisibility(View.GONE);
        }
    }

    private void startRecording() {

        if (checkPermission()) {

            binding.audioContainer.setVisibility(View.VISIBLE);
            binding.removeRecording.setVisibility(View.VISIBLE);
            binding.recordingStatus.setVisibility(View.VISIBLE);
            binding.resume.setVisibility(View.GONE);
            binding.play.setVisibility(View.GONE);

            AudioSavePathInDevice =
                    getExternalCacheDir().getAbsolutePath() + "/" +
                            CreateRandomAudioFileName(5) + "AudioRecording.3gp";

            MediaRecorderReady();

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            requestPermission();
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    public String CreateRandomAudioFileName(int string) {
        StringBuilder stringBuilder = new StringBuilder(string);
        int i = 0;
        while (i < string) {
            stringBuilder.append(RandomAudioFileName.
                    charAt(random.nextInt(RandomAudioFileName.length())));

            i++;
        }
        return stringBuilder.toString();
    }

    @SuppressLint("WrongConstant")
    public void MediaRecorderReady() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(AudioSavePathInDevice);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    private void setButtonEnabled() {
        binding.postCommentBtn.setImageResource(R.drawable.ic_send_comment);
        binding.postCommentBtn.setEnabled(true);
    }

    private void setButtonDisabled() {
        binding.postCommentBtn.setImageResource(R.drawable.ic_comment_send_disabled);
        binding.postCommentBtn.setEnabled(false);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static <T> ArrayList<T> removeDuplicates(ArrayList<T> list) {

        // Create a new LinkedHashSet
        Set<T> set = new LinkedHashSet<>();

        // Add the elements to set
        set.addAll(list);

        // Clear the list
        list.clear();

        // add the elements of set
        // with no duplicates to the list
        list.addAll(set);

        // return the list
        return list;
    }
}