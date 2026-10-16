package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
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

        // The top of this screen is always dark, so keep the status bar icons light
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT));
        setContentView(R.layout.welcome_page);

        findViewById(R.id.WelcomePage_Login).setOnClickListener(v ->
                startActivity(AuthActivity.intent(this, false)));
        findViewById(R.id.WelcomePage_SignUp).setOnClickListener(v ->
                startActivity(AuthActivity.intent(this, true)));
        findViewById(R.id.WelcomePage_forgotCreds).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.forgot_title)
                        .setMessage(R.string.forgot_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show());

        View hero = findViewById(R.id.welcomeHero);
        View sheet = findViewById(R.id.welcomeSheet);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // The brand area clears the status bar; the sheet reaches under the navigation bar
            hero.setPadding(Ui.dp(this, 32) + bars.left, bars.top, Ui.dp(this, 32) + bars.right, 0);
            sheet.setPadding(Ui.dp(this, 28) + bars.left, Ui.dp(this, 32),
                    Ui.dp(this, 28) + bars.right, Ui.dp(this, 20) + bars.bottom);
            return insets;
        });
    }
}
