package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class WelcomePage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip the welcome page when someone is already logged in
        String uid = Session.currentUid(this);
        if (uid != null && DBHandler.getInstance(this).getUser(uid) != null) {
            startActivity(new Intent(this, ArticleBrowse.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.welcome_page);

        Button login = findViewById(R.id.WelcomePage_Login);
        Button signUp = findViewById(R.id.WelcomePage_SignUp);
        TextView forgot = findViewById(R.id.WelcomePage_forgotCreds);

        login.setOnClickListener(v -> startActivity(AuthActivity.intent(this, false)));
        signUp.setOnClickListener(v -> startActivity(AuthActivity.intent(this, true)));
        forgot.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.forgot_title)
                .setMessage(R.string.forgot_message)
                .setPositiveButton(android.R.string.ok, null)
                .show());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
