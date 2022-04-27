package community.growtechsol.com.edit_dialogues;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.vansuita.pickimage.bean.PickResult;
import com.vansuita.pickimage.bundle.PickSetup;
import com.vansuita.pickimage.dialog.PickImageDialog;
import com.vansuita.pickimage.listeners.IPickResult;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import community.growtechsol.com.R;
import community.growtechsol.com.databinding.ActivityEditPostDialogueBinding;
import community.growtechsol.com.fragments.AddPostFragment;
import community.growtechsol.com.models.Post;
import community.growtechsol.com.models.User;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class EditPostDialogue extends AppCompatActivity implements IPickResult {

    ActivityEditPostDialogueBinding binding;
    Uri uri;
    FirebaseAuth auth;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    ProgressDialog progressDialog;
    String postImg, postTitle, postDescription, postRecording, downloadedRecordingLocation, recTime,cropNameSent;
    long timeStamp;
    boolean hasImage = false;
    List<String> cropList = new ArrayList<>();
    private String cropName = "";

    //Recording
    String AudioSavePathInDevice = null;
    MediaRecorder mediaRecorder;
    Random random;
    String RandomAudioFileName = "ABCDEFGHIJKLMNOP";
    MediaPlayer mediaPlayer;
    int length;

    public EditPostDialogue() {
        // Required empty public constructor
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditPostDialogueBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        progressDialog = new ProgressDialog(this);

        //audio recording
        random = new Random();

        Intent intent = getIntent();
        timeStamp = intent.getLongExtra("postTimeStampId", 0);
        postImg = intent.getStringExtra("postImg");
        postTitle = intent.getStringExtra("postTitle");
        postDescription = intent.getStringExtra("postDesc");
        postRecording = intent.getStringExtra("postRecording");
        recTime = intent.getStringExtra("recTime");
        cropNameSent = intent.getStringExtra("cropNameSent");

        setupFunctions();
        populateData();
        setupCropList();

    }

    private void setupCropList() {

        String[] your_array = getResources().getStringArray(R.array.crop_list);

        cropList.addAll(Arrays.asList(your_array));
        SmartMaterialSpinner<String> crop = binding.crop;
        crop.setItem(cropList);

        crop.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                cropName = cropList.get(position);
                Toast.makeText(EditPostDialogue.this, cropName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

    }

    @SuppressLint("CheckResult")
    private void populateData() {
        if (postImg != null) {
            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.placeholder);
            Glide.with(this)
                    .setDefaultRequestOptions(requestOptions)
                    .load(postImg).into(binding.postImage);

        } else {
            binding.postImage.setVisibility(View.GONE);
            binding.removeImg.setVisibility(View.GONE);
        }

        if (postRecording != null) {
            binding.audioContainer.setVisibility(View.VISIBLE);
            binding.removeRecording.setVisibility(View.VISIBLE);
            binding.play.setVisibility(View.VISIBLE);

        } else {
            binding.audioContainer.setVisibility(View.GONE);
            binding.removeRecording.setVisibility(View.GONE);
        }

        String title = postTitle;
        if (!title.equals("")) {
            binding.postTitle.setText(Html.fromHtml("<b>" + postTitle + "</b>"));
            binding.postTitle.setVisibility(View.VISIBLE);
        } else {
            //binding.postTitle.setVisibility(View.GONE);
            Log.d("EditPostDialogue", "Complementary else");
        }

        String description = postDescription;
        if (!description.equals("")) {
            binding.postDescription.setText(postDescription);
            binding.postDescription.setVisibility(View.VISIBLE);
        } else {
            //binding.postDescription.setVisibility(View.GONE);
            Log.d("EditPostDialogue", "Complementary else");
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupFunctions() {

        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Post Updating");
        progressDialog.setMessage("Please Wait...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        if ((postTitle == null || postTitle.isEmpty()) && (postDescription == null || postDescription.isEmpty()) && (postImg == null || postImg.isEmpty()) && (postRecording == null || postRecording.isEmpty())) {
            setButtonDisabled();
        } else {
            setButtonEnabled();
        }

        firebaseDatabase.getReference().child("Users/" + auth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            Picasso.get()
                                    .load(user.getProfileImage())
                                    .placeholder(R.drawable.placeholder)
                                    .into(binding.profileImgEditPost);
                            binding.userNameEditPost.setText(user.getName());
                            binding.userProfessionEditPost.setText(user.getProfession());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.postTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String title = binding.postTitle.getText().toString();
                String description = binding.postDescription.getText().toString();
                if (!title.isEmpty() || !description.isEmpty() || uri != null || hasImage || AudioSavePathInDevice != null) {
                    setButtonEnabled();
                } else {
                    setButtonDisabled();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        binding.postDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String title = binding.postTitle.getText().toString();
                String description = binding.postDescription.getText().toString();
                if (!description.isEmpty() || !title.isEmpty() || uri != null || hasImage || AudioSavePathInDevice != null) {
                    setButtonEnabled();
                } else {
                    setButtonDisabled();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        binding.addImg.setOnClickListener(view -> setPickImage());

        binding.removeImg.setOnClickListener(view -> {
            removeImage();
        });

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

        binding.postBtn.setOnClickListener(view -> {

            progressDialog.show();

            final StorageReference storageReference = firebaseStorage.getReference().child("postRecordings")
                    .child(auth.getCurrentUser().getUid())
                    .child(recTime + "");

            if (mediaRecorder != null && AudioSavePathInDevice != null) {
                Log.d("Checkpoint", "mediaPlayer and Path NOT NULL");
                Uri recordingUri = Uri.fromFile(new File(AudioSavePathInDevice));
                storageReference.putFile(recordingUri).addOnSuccessListener(taskSnapshot -> {
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        uploadPostImgAudioAndData(uri);
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to download audio Url", Toast.LENGTH_SHORT).show();
                    });

                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to Upload Audio", Toast.LENGTH_SHORT).show();
                });
            } else {
                Log.d("Checkpoint", "mediaPlayer and Path ARE NULL");
                uploadPostImgAndData();
            }
        });
    }

    private void setPickImage() {
        @SuppressLint("WrongConstant")
        PickSetup setup = new PickSetup()
                .setTitle("Choose Option")
                .setTitleColor(Color.BLACK)
                .setCameraButtonText("Capture")
                .setGalleryButtonText("Gallery")
                .setIconGravity(Gravity.RIGHT)
                .setButtonOrientation(LinearLayoutCompat.HORIZONTAL)
                .setBackgroundColor(Color.WHITE)
                .setCancelText("Cancel");

        PickImageDialog pickImageDialog = PickImageDialog.build(setup).show(this);

    }

    private void removeImage() {
        uri = null;
        binding.postImage.setImageURI(null);
        binding.postImage.setImageResource(0);
        hasImage = false;
        binding.removeImg.setVisibility(View.GONE);

        if (binding.postTitle.getText().toString().isEmpty() && binding.postDescription.getText().toString().isEmpty() && mediaPlayer == null) {
            setButtonDisabled();
        }
    }

    private void uploadPostImgAudioAndData(Uri audioUri) {

        final StorageReference storageReference = firebaseStorage.getReference().child("posts")
                .child(auth.getCurrentUser().getUid())
                .child(recTime + "");

        if (this.uri != null) {
            storageReference.putFile(this.uri).addOnSuccessListener(taskSnapshot -> {
                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    Post post = new Post();
                    post.setPostImage(uri.toString());
                    post.setPostRecording(audioUri.toString());
                    post.setRecTime(recTime);
                    post.setPostedBy(auth.getCurrentUser().getUid());
                    post.setCreatedAt(new Date().toString());
                    post.setPostTitle(binding.postTitle.getText().toString());
                    if((cropName.equals(""))) {
                        post.setCropName(cropNameSent);
                    }else{
                        post.setCropName(cropName);
                    }
                    post.setPostDescription(binding.postDescription.getText().toString());
                    post.setPostedAt(timeStamp);

                    firebaseDatabase.getReference().child("posts").child(String.valueOf(timeStamp))
                            .setValue(post).addOnSuccessListener(unused -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Post Edited Successfully", Toast.LENGTH_SHORT).show();
                        //switchFragment();
                    }).addOnFailureListener(e -> progressDialog.dismiss());
                });
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to Post", Toast.LENGTH_SHORT).show();
            });
        } else {
            Post post = new Post();
            post.setPostImage(postImg);
            post.setPostRecording(audioUri.toString());
            post.setRecTime(recTime);
            post.setPostedBy(auth.getCurrentUser().getUid());
            post.setCreatedAt(new Date().toString());
            if((cropName.equals(""))) {
                post.setCropName(cropNameSent);
            }else{
                post.setCropName(cropName);
            }
            post.setPostTitle(binding.postTitle.getText().toString());
            post.setPostDescription(binding.postDescription.getText().toString());
            post.setPostedAt(timeStamp);

            firebaseDatabase.getReference().child("posts").child(String.valueOf(timeStamp))
                    .setValue(post).addOnSuccessListener(unused -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Posted Successfully", Toast.LENGTH_SHORT).show();
                //switchFragment();
            }).addOnFailureListener(e -> progressDialog.dismiss());
        }
    }

    private void uploadPostImgAndData() {

        final StorageReference storageReference = firebaseStorage.getReference().child("posts")
                .child(auth.getCurrentUser().getUid())
                .child(recTime + "");

        if (uri != null) {
            Log.d("Checkpoint", "if");
            storageReference.putFile(uri).addOnSuccessListener(taskSnapshot -> {
                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {

                    Post post = new Post();
                    post.setPostImage(uri.toString());
                    post.setPostRecording(postRecording);
                    post.setRecTime(recTime);
                    post.setPostedBy(auth.getCurrentUser().getUid());
                    post.setCreatedAt(new Date().toString());
                    post.setPostTitle(binding.postTitle.getText().toString());
                    if((cropName.equals(""))) {
                        post.setCropName(cropNameSent);
                    }else{
                        post.setCropName(cropName);
                    }
                    post.setPostDescription(binding.postDescription.getText().toString());
                    post.setPostedAt(timeStamp);

                    firebaseDatabase.getReference().child("posts").child(String.valueOf(timeStamp))
                            .setValue(post).addOnSuccessListener(unused -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Post Edited Successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }).addOnFailureListener(e -> progressDialog.dismiss());
                });
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to Edit Post", Toast.LENGTH_SHORT).show();
            });
        } else {

            Map<String, Object> post = new HashMap<>();
            post.put("postImage", postImg);
            post.put("postRecording", postRecording);
            post.put("recTime", recTime);
            post.put("postedBy", auth.getCurrentUser().getUid());
            post.put("createdAt", new Date().toString());
            post.put("postTitle", binding.postTitle.getText().toString());
            if((cropName.equals(""))) {
                post.put("cropName",cropNameSent);
            }else{
                post.put("cropName",cropName);
            }
            post.put("postDescription", binding.postDescription.getText().toString());
            post.put("postedAt", timeStamp);

            firebaseDatabase.getReference().child("posts").child(String.valueOf(timeStamp))
                    .updateChildren(post).addOnSuccessListener(unused -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Post Edited Successfully", Toast.LENGTH_SHORT).show();
                finish();
            }).addOnFailureListener(e -> progressDialog.dismiss());
        }
    }

    private void removeRecording() {
        binding.audioContainer.setVisibility(View.GONE);
        binding.removeRecording.setVisibility(View.GONE);
        mediaPlayer = null;
        AudioSavePathInDevice = null;
        downloadedRecordingLocation = null;

        if (binding.postTitle.getText().toString().isEmpty() && binding.postDescription.getText().toString().isEmpty() && uri == null && !hasImage) {
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

        } else {

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(postRecording);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mediaPlayer.start();
            Log.d("AddPostFragment", "Audio is not recorded");
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
            Log.d("Length", String.valueOf(length));
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                Log.d("CheckAudio", "check" + AudioSavePathInDevice + " " + mediaPlayer + " " + mediaRecorder);
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

        if (postRecording != null) {
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
            } catch (IllegalStateException | IOException e) {
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

    @Override
    public void onPickResult(PickResult r) {
        uri = r.getUri();
        binding.postImage.setImageBitmap(r.getBitmap());
        binding.postImage.setVisibility(View.VISIBLE);
        binding.removeImg.setVisibility(View.VISIBLE);
        setButtonEnabled();
    }

}