package com.example.communityfeedapp.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.communityfeedapp.MainActivity;
import com.example.communityfeedapp.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    ActivityLoginBinding binding;
    FirebaseAuth auth;
    FirebaseUser currentUser;
    ProgressDialog progressDialog;
    static {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d("CheckingErr","onCreateLoginActivity");
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        progressDialog = new ProgressDialog(LoginActivity.this);

        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Logging In");
        progressDialog.setMessage("Please Wait...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        binding.loginBtn.setOnClickListener(v -> {
            progressDialog.show();
            auth.signInWithEmailAndPassword(binding.emailET.getText().toString(), binding.pwdET.getText().toString())
                    .addOnCompleteListener(task -> {
                        //Log.d("LoginActivity", "OnCompleteLogin");
                    })
                    .addOnSuccessListener(authResult -> {
                        //Log.d("LoginActivity", "OnSuccessLogin");
                        progressDialog.dismiss();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "Error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        binding.goToSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        Log.d("CheckingErr","onStartLoginActivity");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("CheckingErr","onResumeLoginActivity");
    }
}