package com.example.communityfeedapp.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.communityfeedapp.R;
import com.example.communityfeedapp.databinding.FragmentAddPostBinding;
import com.example.communityfeedapp.models.Post;
import com.example.communityfeedapp.models.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.vansuita.pickimage.bundle.PickSetup;
import com.vansuita.pickimage.dialog.PickImageDialog;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class AddPostFragment extends Fragment {

    public static final int RequestPermissionCode = 1;
    public static final String FCM_SEND = "https://fcm.googleapis.com/fcm/send";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
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
    long timeStamp;
    //Broadcast Notification
    OkHttpClient mClient;
    private DatabaseReference mDatabase;

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

        //Broadcast Notification

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

        mDatabase = FirebaseDatabase.getInstance().getReferenceFromUrl("https://communityfeedapp-default-rtdb.firebaseio.com/").getRef();

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
            //            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//            intent.setType("image/*");
//            startActivityForResult(intent, 10);
            setPickImage();

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
            removeRecording();
        });

        binding.removeImg.setOnClickListener(view -> {
            removeImgFromPost();
        });

        setFireBaseNotificationId();

        binding.postBtn.setOnClickListener(view -> {

            progressDialog.show();

            timeStamp = new Date().getTime();
            final StorageReference storageReference = firebaseStorage.getReference().child("postRecordings")
                    .child(auth.getCurrentUser().getUid())
                    .child(new Date().getTime() + "");

            Log.d("CheckNull", AudioSavePathInDevice + " " + mediaRecorder);
            if (mediaRecorder != null && AudioSavePathInDevice != null) {
                //Log.d("Checkpoint", "mediaPlayer and Path NOT NULL");
                Uri recordingUri = Uri.fromFile(new File(AudioSavePathInDevice));
                storageReference.putFile(recordingUri).addOnSuccessListener(taskSnapshot -> {
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        uploadPostImgAudioAndData(uri);
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Failed to download audio Url", Toast.LENGTH_SHORT).show();
                    });

                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to Upload Audio", Toast.LENGTH_SHORT).show();
                });
            } else {
                Log.d("Checkpoint", "mediaPlayer and Path ARE NULL");
                uploadPostImgAndData();
            }
        });

        return binding.getRoot();
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

        PickImageDialog pickImageDialog = PickImageDialog.build(setup).show((FragmentActivity) getContext());

        PickImageDialog.build(setup)
                .setOnPickResult(r -> {
                    if (r.getError() == null) {
                        uri = r.getUri();
                        Log.d("Uri", String.valueOf(uri));
                        //Glide.with(getContext()).load(r.getBitmap()).into(binding.postImage);
                        binding.postImage.setImageBitmap(r.getBitmap());
                        binding.postImage.setVisibility(View.VISIBLE);
                        binding.removeImg.setVisibility(View.VISIBLE);
                        setButtonEnabled();
                        pickImageDialog.dismiss();

                    }
                })
                .setOnPickCancel(() -> {
                    pickImageDialog.dismiss();
                    Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                }).show(((FragmentActivity) getContext()).getSupportFragmentManager());

    }

    //    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (resultCode == RESULT_OK) {
//            if (requestCode == 10) {
//                if (data.getData() != null) {
//                    uri = data.getData();
//                    Log.d("Uri", String.valueOf(uri));
//                    binding.postImage.setImageURI(uri);
//                    binding.postImage.setVisibility(View.VISIBLE);
//                    binding.removeImg.setVisibility(View.VISIBLE);
//                    setButtonEnabled();
//                }
//            }
//        }
//    }

    private ArrayList<String> getFireBaseNotificationId() {
        mClient = new OkHttpClient();
        ArrayList<String> myArrayList = new ArrayList<>();
        firebaseDatabase.getReference().child("system").child("notification").addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot != null) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        String key = dataSnapshot.getKey();
                        String description = dataSnapshot.child("notification_id").getValue(String.class);
                        Log.d("KeyDesc", key + " " + description);
                        myArrayList.add(description);

                    }
                    JSONArray regArray = new JSONArray(myArrayList);

                    //Log.d("UserName", binding.userNameAddPost.getText().toString());
                    //Log.d("Checkpoint", myArrayList + "");
                    sendMessage(regArray, "New Post", binding.userNameAddPost.getText().toString() + " has added a new Post, click to see details", "icon", "message");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        return myArrayList;

    }

    private void removeRecording() {
        binding.audioContainer.setVisibility(View.GONE);
        binding.removeRecording.setVisibility(View.GONE);
        mediaPlayer = null;
        AudioSavePathInDevice = null;

        if (binding.postTitle.getText().toString().isEmpty() && binding.postDescription.getText().toString().isEmpty() && uri == null) {
            setButtonDisabled();
        }
    }

    private void removeImgFromPost() {
        uri = null;
        binding.postImage.setImageURI(null);
        binding.postImage.setImageResource(0);
        binding.removeImg.setVisibility(View.GONE);
        if (binding.postTitle.getText().toString().isEmpty() && binding.postDescription.getText().toString().isEmpty() && mediaPlayer == null) {
            setButtonDisabled();
        }

    }

    private void uploadPostImgAudioAndData(Uri audioUri) {

        final StorageReference storageReference = firebaseStorage.getReference().child("posts")
                .child(auth.getCurrentUser().getUid())
                .child(timeStamp + "");

        if (this.uri != null) {
            storageReference.putFile(this.uri).addOnSuccessListener(taskSnapshot -> {
                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    Post post = new Post();
                    post.setPostImage(uri.toString());
                    post.setPostRecording(audioUri.toString());
                    post.setRecTime(String.valueOf(timeStamp));
                    post.setPostedBy(auth.getCurrentUser().getUid());
                    post.setCreatedAt(new Date().toString());
                    post.setSolved(false);
                    post.setPostTitle(binding.postTitle.getText().toString());
                    post.setPostDescription(binding.postDescription.getText().toString());
                    post.setPostedAt(new Date().getTime());

                    firebaseDatabase.getReference().child("posts").child(String.valueOf(post.getPostedAt()))
                            .setValue(post).addOnSuccessListener(unused -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Posted Successfully", Toast.LENGTH_SHORT).show();
                        sendNotification();
                        hideKeyboard(getActivity());
                        incrementTotalPostsCount();
                    }).addOnFailureListener(e -> progressDialog.dismiss());
                });
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Failed to Post", Toast.LENGTH_SHORT).show();
            });
        } else {
            Post post = new Post();
            post.setPostRecording(audioUri.toString());
            post.setRecTime(String.valueOf(timeStamp));
            post.setPostedBy(auth.getCurrentUser().getUid());
            post.setCreatedAt(new Date().toString());
            post.setSolved(false);
            post.setPostTitle(binding.postTitle.getText().toString());
            post.setPostDescription(binding.postDescription.getText().toString());
            post.setPostedAt(new Date().getTime());

            firebaseDatabase.getReference().child("posts").child(String.valueOf(post.getPostedAt()))
                    .setValue(post).addOnSuccessListener(unused -> {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Posted Successfully", Toast.LENGTH_SHORT).show();
                sendNotification();
                hideKeyboard(getActivity());
                incrementTotalPostsCount();
            }).addOnFailureListener(e -> progressDialog.dismiss());
        }
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

    private void incrementTotalPostsCount() {
        FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(auth.getCurrentUser().getUid())
                .child("totalPosts")
                .setValue(ServerValue.increment(1)).addOnSuccessListener(unused -> {
            Toast.makeText(getContext(), "Post count updated", Toast.LENGTH_SHORT).show();
            ((BottomNavigationView) getActivity().findViewById(R.id.bottom_navigation_bar)).setSelectedItemId(R.id.item_home);
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to update post count", Toast.LENGTH_SHORT).show();
            ((BottomNavigationView) getActivity().findViewById(R.id.bottom_navigation_bar)).setSelectedItemId(R.id.item_home);
        });
    }

    private void sendNotification() {
        getFireBaseNotificationId();
    }

    private void uploadPostImgAndData() {

        final StorageReference storageReference = firebaseStorage.getReference().child("posts")
                .child(auth.getCurrentUser().getUid())
                .child(timeStamp + "");

        if (uri != null) {
            storageReference.putFile(uri).addOnSuccessListener(taskSnapshot -> {
                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    Post post = new Post();
                    post.setPostImage(uri.toString());
                    post.setRecTime(String.valueOf(timeStamp));
                    post.setPostedBy(auth.getCurrentUser().getUid());
                    post.setCreatedAt(new Date().toString());
                    post.setSolved(false);
                    post.setPostTitle(binding.postTitle.getText().toString());
                    post.setPostDescription(binding.postDescription.getText().toString());
                    post.setPostedAt(new Date().getTime());

                    firebaseDatabase.getReference().child("posts").child(String.valueOf(post.getPostedAt()))
                            .setValue(post).addOnSuccessListener(unused -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Posted Successfully", Toast.LENGTH_SHORT).show();
                        sendNotification();
                        hideKeyboard(getActivity());
                        incrementTotalPostsCount();
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
            post.setSolved(false);
            post.setPostTitle(binding.postTitle.getText().toString());
            post.setPostDescription(binding.postDescription.getText().toString());
            post.setPostedAt(new Date().getTime());

            firebaseDatabase.getReference().child("posts").child(String.valueOf(post.getPostedAt()))
                    .setValue(post).addOnSuccessListener(unused -> {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Posted Successfully", Toast.LENGTH_SHORT).show();
                sendNotification();
                hideKeyboard(getActivity());
                incrementTotalPostsCount();
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
                Toast.makeText(getContext(), "Recording Completed", Toast.LENGTH_SHORT).show();
                setButtonEnabled();
            } else {
                Toast.makeText(getContext(), "Error! Record Again..", Toast.LENGTH_SHORT).show();
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
                    getActivity().getExternalCacheDir().getAbsolutePath() + "/" +
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

    public void sendMessage(final JSONArray recipients, final String title, final String body, final String icon, final String message) {

        new AsyncTask<String, String, String>() {
            @SuppressLint("StaticFieldLeak")
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
        Log.d("Checkpointt", "here");
        RequestBody body = RequestBody.create(JSON, bodyString);
        Request request = new Request.Builder()
                .url(FCM_SEND)
                .post(body)
                .addHeader("Authorization", "key=AAAAaGx1iT8:APA91bEa9jz6wQVGu1lble4o2DRrVhm5b0omMS1T5F5it6s9KOl2TDoXYPOQxz3I1g6P37-APfKbfGnFYvZ1u0RaYexbgGsZ_ipFoFDIx3kbpBBW1GPJCgcDQMNriXjvMAC-fuf363Ek")
                .build();
        Response response = mClient.newCall(request).execute();
        return response.body().string();
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

    //    @Override
//    public void onPickResult(PickResult r) {
//        if (r.getError() == null) {
//            uri = r.getUri();
//            Log.d("Uri", String.valueOf(uri));
//            binding.postImage.setImageURI(uri);
//            binding.postImage.setVisibility(View.VISIBLE);
//            binding.removeImg.setVisibility(View.VISIBLE);
//            setButtonEnabled();
//        }
//    }
}