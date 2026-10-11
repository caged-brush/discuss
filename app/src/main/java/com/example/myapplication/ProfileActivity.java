package com.example.myapplication;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RatingBar ratingBarUser;
    private TextView tvUserName;
    private DBHandler dbHandler;
    
    // Default UID, normally passed via Intent
    private String targetUid = "user1"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile);

        dbHandler = new DBHandler(this);
        
        btnBack = findViewById(R.id.btnBack);
        ratingBarUser = findViewById(R.id.ratingBarUser);
        tvUserName = findViewById(R.id.tvUserName);

        btnBack.setOnClickListener(v -> finish());
        
        loadProfileData();
    }

    private void loadProfileData() {
        Cursor cursor = dbHandler.getUser(targetUid);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow("first_name"));
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow("last_name"));
                String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
                
                String name = firstName + " " + lastName;
                if (name.trim().isEmpty()) {
                    name = username;
                }
                tvUserName.setText(name);
            }
            cursor.close();
        }

        double avgRating = dbHandler.getAverageRating(targetUid);
        ratingBarUser.setRating((float) avgRating);
    }
}