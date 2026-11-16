package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

abstract class SignedInActivity extends AppCompatActivity {
    protected DBHandler db;
    protected String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }
        currentUid = user.getUid();
        db = new DBHandler(this);
        db.ensureUser(currentUid, user.getDisplayName());
        EdgeToEdge.enable(this);
    }

    @Override
    public void setContentView(int layout) {
        super.setContentView(layout);
        ScreenInsets.apply(findViewById(R.id.main));
    }

    protected void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }
}
