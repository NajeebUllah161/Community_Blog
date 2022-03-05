package com.example.communityfeedapp.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.communityfeedapp.MainActivity;
import com.example.communityfeedapp.databinding.ActivitySignupBinding;
import com.example.communityfeedapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    ActivitySignupBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase firebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        binding.signUpBtn.setOnClickListener(v -> createUser());

        binding.goToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void createUser() {
        auth.createUserWithEmailAndPassword(binding.emailET.getText().toString(), binding.pwdET.getText().toString())
                .addOnCompleteListener(task -> {
                    //Log.d("SignupActivity", "OnCompleteSignUp");
                }).addOnSuccessListener(authResult -> {
            User user = new User(
                    binding.nameET.getText().toString(),
                    binding.professionET.getText().toString(),
                    binding.emailET.getText().toString(),
                    binding.pwdET.getText().toString()
            );
            String userId = authResult.getUser().getUid();
            firebaseDatabase.getReference().child("Users").child(userId).setValue(user);
            //Log.d("SignupActivity", "OnSuccessSignup");
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            //Log.d("SignupActivity", "Error occurred: " + e.getMessage());
        });
    }
}