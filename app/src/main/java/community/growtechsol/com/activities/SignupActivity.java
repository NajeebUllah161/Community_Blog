package community.growtechsol.com.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import community.growtechsol.com.MainActivity;
import community.growtechsol.com.databinding.ActivitySignupBinding;
import community.growtechsol.com.models.User;

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