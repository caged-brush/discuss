package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomePage extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.welcome_page);

        findViewById(R.id.WelcomePage_Login).setOnClickListener(view ->
                startActivity(new Intent(this, Login.class)));
        findViewById(R.id.WelcomePage_SignUp).setOnClickListener(view ->
                startActivity(new Intent(this, Signup.class)));

        ScreenInsets.apply(findViewById(R.id.main));
    }
}
