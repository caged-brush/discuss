
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

public class Login extends AppCompatActivity {

    private static final String TAG = "LOGIN";

    private FirebaseAuth auth;
    private EditText emailT;
    private EditText passwordT;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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
            emailT.setError("Email is required");
            emailT.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailT.setError("Enter a valid email address");
            emailT.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordT.setError("Password is required");
            passwordT.requestFocus();
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    loginButton.setEnabled(true);
                    loginButton.setText("Login");

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                Login.this,
                                "Login successful!",
                                Toast.LENGTH_SHORT
                        ).show();

                        updateUI();

                    } else {

                        Log.w(TAG, "Login failed", task.getException());

                        Toast.makeText(
                                Login.this,
                                "Login failed. Check your credentials.",
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
