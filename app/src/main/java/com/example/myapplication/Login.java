
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

public class Login extends AppCompatActivity {

    private static final String TAG = "LOGIN";

    private FirebaseAuth auth;
    private EditText emailT;
    private EditText passwordT;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ScreenInsets.apply(findViewById(R.id.main));

        auth = FirebaseAuth.getInstance();

        emailT = findViewById(R.id.login_email);
        passwordT = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(v -> loginUser());

        TextView signupLink = findViewById(R.id.signup_text);

        signupLink.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, Signup.class));
        });
    }

    private void loginUser() {

        String email = emailT.getText().toString().trim();
        String password = passwordT.getText().toString();

        if (email.isEmpty()) {
            emailT.setError(getString(R.string.email_required));
            emailT.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailT.setError(getString(R.string.invalid_email));
            emailT.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordT.setError(getString(R.string.password_required));
            passwordT.requestFocus();
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText(getString(R.string.login_in_progress));

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    loginButton.setEnabled(true);
                    loginButton.setText(R.string.action_login);

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                Login.this,
                                getString(R.string.login_success),
                                Toast.LENGTH_SHORT
                        ).show();

                        updateUI();

                    } else {

                        Log.w(TAG, "Login failed", task.getException());

                        Toast.makeText(
                                Login.this,
                                getString(R.string.login_failed),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            updateUI();
        }
    }

    private void updateUI() {
        Intent intent = new Intent(
                Login.this,
                ArticleBrowse.class
        );

        startActivity(intent);
        finish();
    }
}
