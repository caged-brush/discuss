package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Signup extends AppCompatActivity {
    private final String TAG = "SIGNUP";
    private FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        auth = FirebaseAuth.getInstance();

        EditText usernameT = findViewById(R.id.signUp_username);
        EditText emailT = findViewById(R.id.signup_email);
        EditText passwordT = findViewById(R.id.signup_password);
        TextView test = findViewById(R.id.textView2);

        Button signup = findViewById(R.id.signup_button);
        //emails must be properly formatted and paswords must be atleast 6 characters
        //for firebase to accept them and create a new user
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameT.getText().toString();
                String email = emailT.getText().toString();
                String password = passwordT.getText().toString();
                test.setText(email + " " + password);
                auth.createUserWithEmailAndPassword(email, password).
                        addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()){
                                    Log.d(TAG, "user sign up success");
                                    FirebaseUser user = auth.getCurrentUser();
                                    updateUI(Signup.this);

                                }else{
                                    Log.w(TAG, "user signup failure", task.getException());
                                    Toast.makeText(Signup.this, "authentication failure", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });




        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onStart(){
        super.onStart();
        FirebaseUser curUser = auth.getCurrentUser();
        if(curUser != null){
            //todo reload the page
            updateUI(Signup.this);
        }
    }

    public void updateUI(Context context){
        Intent intent = new Intent(context, ArticleBrowse.class);
        startActivity(intent);
    }
}