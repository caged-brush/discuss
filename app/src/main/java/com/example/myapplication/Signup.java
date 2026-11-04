
package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class Signup extends AppCompatActivity {

    private static final String TAG = "SIGNUP";

    private FirebaseAuth auth;
    private EditText usernameT, emailT, passwordT;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        auth = FirebaseAuth.getInstance();

        usernameT = findViewById(R.id.signUp_username);
        emailT = findViewById(R.id.signup_email);
        passwordT = findViewById(R.id.signup_password);
        signupButton = findViewById(R.id.signup_button);

        TextView loginLink =
                findViewById(R.id.signup_login_link);

        signupButton.setOnClickListener(v -> signupUser());

        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(Signup.this, Login.class));
            finish();
        });
    }

    private void signupUser() {
        String username =
                usernameT.getText().toString().trim();
        String email =
                emailT.getText().toString().trim();
        String password =
                passwordT.getText().toString();

        if (username.isEmpty()) {
            usernameT.setError("Username is required");
            usernameT.requestFocus();
            return;
        }

        if (email.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailT.setError("Enter a valid email address");
            emailT.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordT.setError(
                    "Password must be at least 6 characters"
            );
            passwordT.requestFocus();
            return;
        }

        signupButton.setEnabled(false);
        signupButton.setText("Creating account...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        signupButton.setEnabled(true);
                        signupButton.setText("Create Account");

                        Log.w(TAG, "Signup failed",
                                task.getException());

                        Toast.makeText(
                                Signup.this,
                                "Signup failed: " +
                                        (task.getException() != null
                                                ? task.getException().getMessage()
                                                : "Unknown error"),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        signupButton.setEnabled(true);
                        signupButton.setText("Create Account");
                        Toast.makeText(this,
                                "Account created, but user unavailable",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    UserProfileChangeRequest profile =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                    user.updateProfile(profile)
                            .addOnCompleteListener(profileTask -> {
                                if (!profileTask.isSuccessful()) {
                                    Log.w(TAG,
                                            "Could not save display name",
                                            profileTask.getException());
                                    Toast.makeText(
                                            Signup.this,
                                            "Account created, but username could not be saved",
                                            Toast.LENGTH_LONG
                                    ).show();
                                } else {
                                    Toast.makeText(
                                            Signup.this,
                                            "Account created successfully!",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                                openArticleBrowse();
                            });
                });
    }

    private void openArticleBrowse() {
        Intent intent =
                new Intent(Signup.this, ArticleBrowse.class);
        startActivity(intent);
        finish();
    }
}
