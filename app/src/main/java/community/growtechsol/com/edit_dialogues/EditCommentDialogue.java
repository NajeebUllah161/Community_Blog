package community.growtechsol.com.edit_dialogues;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import community.growtechsol.com.R;
import community.growtechsol.com.databinding.ActivityEditCommentDialogueBinding;
import community.growtechsol.com.fragments.AddPostFragment;
import community.growtechsol.com.models.User;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class EditCommentDialogue extends AppCompatActivity {

    ActivityEditCommentDialogueBinding binding;
    String commentedBy, commentData, commentRecording, postId, downloadedRecordingLocation;
    long commentedAt;
    FirebaseStorage firebaseStorage;
    FirebaseDatabase firebaseDatabase;
    FirebaseAuth auth;
    ProgressDialog progressDialog;

    //Recording
    String AudioSavePathInDevice = null;
    MediaRecorder mediaRecorder;
    Random random;
    String RandomAudioFileName = "ABCDEFGHIJKLMNOP";
    MediaPlayer mediaPlayer;
    int length;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditCommentDialogueBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        Intent intent = getIntent();
        commentedBy = intent.getStringExtra("commentedBy");
        commentData = intent.getStringExtra("commentData");
        commentedAt = intent.getLongExtra("commentedAt", 0);
        postId = intent.getStringExtra("postId");
        commentRecording = intent.getStringExtra("commentRecording");

        setupFunctions();
    }

    private void setupFunctions() {
        progressDialog = new ProgressDialog(this);
        random = new Random();

        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Post Updating");
        progressDialog.setMessage("Please Wait...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        populateExistingDate();
        setupEventListeners();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupEventListeners() {

        binding.addRecording.setOnTouchListener((view, motionEvent) -> {
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

        binding.commentDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String commentData = binding.commentDescription.getText().toString();
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

        binding.postBtn.setOnClickListener(view -> {

            progressDialog.show();

            final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("commentRecordings")
                    .child(commentedAt + "")
                    .child(auth.getCurrentUser().getUid());

            if (mediaRecorder != null && AudioSavePathInDevice != null) {
                Uri recordingUri = Uri.fromFile(new File(AudioSavePathInDevice));
                storageReference.putFile(recordingUri).addOnSuccessListener(taskSnapshot -> {
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        Map<String, Object> comment = new HashMap<>();
                        comment.put("commentRecording", uri + "");
                        comment.put("commentBody", binding.commentDescription.getText().toString());

                        firebaseDatabase.getReference()
                                .child("posts/" + postId + "/comments")
                                .child(commentedAt + "")
                                .updateChildren(comment)
                                .addOnSuccessListener(unused -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(this, "Comment Edited Successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                }).addOnFailureListener(e -> progressDialog.dismiss());

                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to download audio Url", Toast.LENGTH_SHORT).show();
                    });

                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to Upload Audio", Toast.LENGTH_SHORT).show();
                });
            } else {
                Map<String, Object> comment = new HashMap<>();
                comment.put("commentBody", binding.commentDescription.getText().toString());

                firebaseDatabase.getReference()
                        .child("posts/" + postId + "/comments")
                        .child(commentedAt + "")
                        .updateChildren(comment)
                        .addOnSuccessListener(unused -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Comment Edited Successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }).addOnFailureListener(e -> progressDialog.dismiss());

            }
        });

    }

    private void populateExistingDate() {

        //Populate user data
        firebaseDatabase.getReference().child("Users")
                .child(commentedBy)
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint({"SetTextI18n", "CheckResult"})
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);

                        binding.userNameEditComment.setText(user.getName() + "");
                        binding.userProfessionEditComment.setText(user.getProfession() + "");
                        RequestOptions requestOptions = new RequestOptions();
                        requestOptions.placeholder(R.drawable.placeholder);
                        if (!EditCommentDialogue.this.isFinishing()) {
                            Glide.with(EditCommentDialogue.this)
                                    .setDefaultRequestOptions(requestOptions)
                                    .load(user.getProfileImage()).into(binding.profileImgEditComment);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {

                    }
                });

        //Populate comment data

        binding.commentDescription.setText(commentData);
        binding.commentDescription.setSelection(commentData.length());

        // setup audio if any

        if (commentRecording != null) {
            binding.audioContainer.setVisibility(View.VISIBLE);
            binding.play.setVisibility(View.VISIBLE);
        }

    }

    private void removeRecording() {
        binding.audioContainer.setVisibility(View.GONE);
        binding.removeRecording.setVisibility(View.GONE);
        mediaPlayer = null;
        AudioSavePathInDevice = null;
        downloadedRecordingLocation = null;

        if (binding.commentDescription.getText().toString().isEmpty()) {
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
            Log.d("EditCommentDialogue", "Audio is not recorded");
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
            Log.d("EditCommentDialogue", "Audio is not recorded");
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
            //Toast.makeText(getContext(),"Recording Playing",Toast.LENGTH_LONG).show();

        } else {

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(commentRecording);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mediaPlayer.start();
            Log.d("EditCommentDialogue", "Audio is not recorded");
        }
        mediaPlayer.setOnCompletionListener(mediaPlayer -> {
            binding.pause.setVisibility(View.GONE);
            binding.play.setVisibility(View.VISIBLE);
        });
    }

    private void stopRecording() {

        boolean isAudio = true;
        if (mediaRecorder != null) {
            binding.recordingStatus.setVisibility(View.INVISIBLE);
            binding.play.setVisibility(View.VISIBLE);
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
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
            binding.audioContainer.setVisibility(View.GONE);
            binding.removeRecording.setVisibility(View.GONE);
        }
    }

    private void startRecording() {

        if (commentRecording != null) {
            downloadedRecordingLocation = null;
            binding.audioContainer.setVisibility(View.GONE);
            binding.removeRecording.setVisibility(View.GONE);
        }

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
            //Toast.makeText(getContext(),"Recording started",Toast.LENGTH_LONG).show();
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
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, AddPostFragment.RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AddPostFragment.RequestPermissionCode) {
            if (grantResults.length > 0) {
                boolean StoragePermission = grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED;
                boolean RecordPermission = grantResults[1] ==
                        PackageManager.PERMISSION_GRANTED;

                if (StoragePermission && RecordPermission) {
                    Toast.makeText(this, "Permission Granted",
                            Toast.LENGTH_SHORT).show();
                } else {
                    binding.audioContainer.setVisibility(View.GONE);
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void setButtonEnabled() {
        binding.postBtn.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.follow_btn_bg));
        binding.postBtn.setTextColor(this.getResources().getColor(R.color.white));
        binding.postBtn.setEnabled(true);
    }

    private void setButtonDisabled() {
        binding.postBtn.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.follow_active_btn));
        binding.postBtn.setTextColor(this.getResources().getColor(R.color.gray));
        binding.postBtn.setEnabled(false);
    }

}