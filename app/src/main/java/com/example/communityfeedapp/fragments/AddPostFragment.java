package com.example.communityfeedapp.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.databinding.FragmentAddPostBinding;
import com.example.communityfeedapp.models.Post;
import com.example.communityfeedapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.app.Activity.RESULT_OK;

public class AddPostFragment extends Fragment {

    public static final int RequestPermissionCode = 1;
    FragmentAddPostBinding binding;
    Uri uri;
    FirebaseAuth auth;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    ProgressDialog progressDialog;
    //Recording
    String AudioSavePathInDevice = null;
    MediaRecorder mediaRecorder;
    Random random;
    String RandomAudioFileName = "ABCDEFGHIJKLMNOP";
    MediaPlayer mediaPlayer;
    int length;

    public AddPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        progressDialog = new ProgressDialog(getContext());

        //audio recording
        random = new Random();

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAddPostBinding.inflate(inflater, container, false);

        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Post Uploading");
        progressDialog.setMessage("Please Wait...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseDatabase.getReference().child("Users/" + auth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            Picasso.get()
                                    .load(user.getProfileImage())
                                    .placeholder(R.drawable.placeholder)
                                    .into(binding.profileImgAddPost);
                            binding.userNameAddPost.setText(user.getName());
                            binding.userProfessionAddPost.setText(user.getProfession());
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
                if (!title.isEmpty() || !description.isEmpty() || uri != null) {
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
                String header = binding.postTitle.getText().toString();
                String description = binding.postDescription.getText().toString();
                if (!description.isEmpty() || !header.isEmpty() || uri != null) {
                    setButtonEnabled();
                } else {
                    setButtonDisabled();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        binding.addImg.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 10);
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

        binding.play.setOnClickListener(view -> {
            playRecording();
            binding.play.setVisibility(View.GONE);
            binding.pause.setVisibility(View.VISIBLE);
        });

        binding.pause.setOnClickListener(view -> {
            pauseRecording();
        });

        binding.resume.setOnClickListener(view -> resumeRecording());

        binding.removeRecording.setOnClickListener(view -> {
            binding.audioContainer.setVisibility(View.GONE);
            binding.removeRecording.setVisibility(View.GONE);
            mediaPlayer = null;
            AudioSavePathInDevice = null;
        });

        binding.removeImg.setOnClickListener(view -> {
            removeImgFromPost();
        });

        binding.postBtn.setOnClickListener(view -> {

            progressDialog.show();

            final StorageReference storageReference = firebaseStorage.getReference().child("postRecordings")
                    .child(auth.getCurrentUser().getUid())
                    .child(new Date().getTime() + "");

            if (mediaPlayer != null && AudioSavePathInDevice != null) {
                Log.d("Checkpoint", "mediaPlayer and Path NOT NULL");
                Uri recordingUri = Uri.fromFile(new File(AudioSavePathInDevice));
                storageReference.putFile(recordingUri).addOnSuccessListener(taskSnapshot -> {
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        uploadPostImgAudioAndData(uri);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to download audio Url", Toast.LENGTH_SHORT).show();
                    });

                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to Upload Audio", Toast.LENGTH_SHORT).show();
                });
            } else {
                Log.d("Checkpoint", "mediaPlayer and Path ARE NULL");
                uploadPostImgAndData();
            }

        });

        return binding.getRoot();
    }

    private void removeImgFromPost() {
        uri = null;
        binding.postImage.setImageURI(null);
        binding.postImage.setImageResource(0);
        binding.removeImg.setVisibility(View.GONE);
    }

    private void uploadPostImgAudioAndData(Uri audioUri) {

        final StorageReference storageReference = firebaseStorage.getReference().child("posts")
                .child(auth.getCurrentUser().getUid())
                .child(new Date().getTime() + "");

        if (this.uri != null) {
            storageReference.putFile(this.uri).addOnSuccessListener(taskSnapshot -> {
                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    Post post = new Post();
                    post.setPostImage(uri.toString());
                    post.setPostRecording(audioUri.toString());
                    post.setPostedBy(auth.getCurrentUser().getUid());
                    post.setCreatedAt(new Date().toString());
                    post.setPostTitle(binding.postTitle.getText().toString());
                    post.setPostDescription(binding.postDescription.getText().toString());
                    post.setPostedAt(new Date().getTime());

                    firebaseDatabase.getReference().child("posts").child(String.valueOf(post.getPostedAt()))
                            .setValue(post).addOnSuccessListener(unused -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Posted Successfully", Toast.LENGTH_SHORT).show();
                        //switchFragment();
                    }).addOnFailureListener(e -> progressDialog.dismiss());
                });
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Failed to Post", Toast.LENGTH_SHORT).show();
            });
        } else {
            Post post = new Post();
            post.setPostRecording(audioUri.toString());
            post.setPostedBy(auth.getCurrentUser().getUid());
            post.setCreatedAt(new Date().toString());
            post.setPostTitle(binding.postTitle.getText().toString());
            post.setPostDescription(binding.postDescription.getText().toString());
            post.setPostedAt(new Date().getTime());

            firebaseDatabase.getReference().child("posts").child(String.valueOf(post.getPostedAt()))
                    .setValue(post).addOnSuccessListener(unused -> {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Posted Successfully", Toast.LENGTH_SHORT).show();
                //switchFragment();
            }).addOnFailureListener(e -> progressDialog.dismiss());
        }
    }

    private void uploadPostImgAndData() {

        final StorageReference storageReference = firebaseStorage.getReference().child("posts")
                .child(auth.getCurrentUser().getUid())
                .child(new Date().getTime() + "");

        if (uri != null) {
            storageReference.putFile(uri).addOnSuccessListener(taskSnapshot -> {
                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    Post post = new Post();
                    post.setPostImage(uri.toString());
                    post.setPostedBy(auth.getCurrentUser().getUid());
                    post.setCreatedAt(new Date().toString());
                    post.setPostTitle(binding.postTitle.getText().toString());
                    post.setPostDescription(binding.postDescription.getText().toString());
                    post.setPostedAt(new Date().getTime());

                    firebaseDatabase.getReference().child("posts").child(String.valueOf(post.getPostedAt()))
                            .setValue(post).addOnSuccessListener(unused -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Posted Successfully", Toast.LENGTH_SHORT).show();
                        //switchFragment();
                    }).addOnFailureListener(e -> progressDialog.dismiss());
                });
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Failed to Post", Toast.LENGTH_SHORT).show();
            });
        } else {
            Post post = new Post();
            post.setPostedBy(auth.getCurrentUser().getUid());
            post.setCreatedAt(new Date().toString());
            post.setPostTitle(binding.postTitle.getText().toString());
            post.setPostDescription(binding.postDescription.getText().toString());
            post.setPostedAt(new Date().getTime());

            firebaseDatabase.getReference().child("posts").child(String.valueOf(post.getPostedAt()))
                    .setValue(post).addOnSuccessListener(unused -> {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Posted Successfully", Toast.LENGTH_SHORT).show();
                //switchFragment();
            }).addOnFailureListener(e -> progressDialog.dismiss());
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
            mediaPlayer.setOnCompletionListener(mediaPlayer -> {
                binding.pause.setVisibility(View.GONE);
                binding.play.setVisibility(View.VISIBLE);
            });
        } else {
            Log.d("AddPostFragment", "Audio is not recorded");
        }
    }

    private void stopRecording() {

        if (mediaRecorder != null) {
            binding.recordingStatus.setVisibility(View.INVISIBLE);
            binding.play.setVisibility(View.VISIBLE);
            Log.d("Length", String.valueOf(length));
            mediaRecorder.stop();
            Toast.makeText(getContext(), "Recording Completed", Toast.LENGTH_SHORT).show();
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
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
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
        int result = ContextCompat.checkSelfPermission(requireContext().getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(requireContext().getApplicationContext(),
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
        ActivityCompat.requestPermissions((Activity) requireContext(), new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == RequestPermissionCode) {
            if (grantResults.length > 0) {
                boolean StoragePermission = grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED;
                boolean RecordPermission = grantResults[1] ==
                        PackageManager.PERMISSION_GRANTED;

                if (StoragePermission && RecordPermission) {
                    Toast.makeText(getContext(), "Permission Granted",
                            Toast.LENGTH_SHORT).show();
                } else {
                    binding.audioContainer.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 10) {
                if (data.getData() != null) {
                    uri = data.getData();
                    binding.postImage.setImageURI(uri);
                    binding.postImage.setVisibility(View.VISIBLE);
                    binding.removeImg.setVisibility(View.VISIBLE);
                    setButtonEnabled();
                }
            }
        }
    }

    private void setButtonEnabled() {
        binding.postBtn.setBackgroundDrawable(ContextCompat.getDrawable(getContext(), R.drawable.follow_btn_bg));
        binding.postBtn.setTextColor(getContext().getResources().getColor(R.color.white));
        binding.postBtn.setEnabled(true);
    }

    private void setButtonDisabled() {
        binding.postBtn.setBackgroundDrawable(ContextCompat.getDrawable(getContext(), R.drawable.follow_active_btn));
        binding.postBtn.setTextColor(getContext().getResources().getColor(R.color.gray));
        binding.postBtn.setEnabled(false);
    }
}