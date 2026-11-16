
package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        ScreenInsets.apply(findViewById(R.id.main));

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
            usernameT.setError(getString(R.string.username_required));
            usernameT.requestFocus();
            return;
        }

        if (email.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailT.setError(getString(R.string.invalid_email));
            emailT.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordT.setError(
                    getString(R.string.short_password)
            );
            passwordT.requestFocus();
            return;
        }

        signupButton.setEnabled(false);
        signupButton.setText(getString(R.string.signup_in_progress));

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        signupButton.setEnabled(true);
                        signupButton.setText(getString(R.string.action_create_account));

                        Log.w(TAG, "Signup failed",
                                task.getException());

                        Toast.makeText(
                                Signup.this,
                                getString(R.string.signup_failed,
                                        (task.getException() != null
                                                ? task.getException().getMessage()
                                                : getString(R.string.unknown_error))),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        signupButton.setEnabled(true);
                        signupButton.setText(getString(R.string.action_create_account));
                        Toast.makeText(this,
                                getString(R.string.signup_user_unavailable),
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
                                            getString(R.string.signup_display_name_failed),
                                            Toast.LENGTH_LONG
                                    ).show();
                                } else {
                                    Toast.makeText(
                                            Signup.this,
                                            getString(R.string.signup_success),
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
