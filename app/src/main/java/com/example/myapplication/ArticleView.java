package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.kwabenaberko.newsapilib.models.Article;

import java.util.ArrayList;

public class ArticleView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_article_view);

        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String url = intent.getStringExtra("url");
        String desc = intent.getStringExtra("desc");
        String author = intent.getStringExtra("author");

        TextView titleT= findViewById(R.id.article_view_title);
        WebView bodyT = findViewById(R.id.articleBody);
        WebSettings webSettings = bodyT.getSettings();
        //webSettings.setJavaScriptEnabled(true);

        bodyT.loadUrl(url);
        Button save = findViewById(R.id.saveToLib);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DBHandler db = new DBHandler(ArticleView.this);

                //db.saveArticle(title, author, );
            }
        });
        //give every article an ID
        ListView comments = findViewById(R.id.article_view_comments);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        //every article will have its own section for comments
        //get article id


        DatabaseReference ref = db.getReference("commentsTest");
        String uID = user.getUid();
        String comment = "this is a test comment";
        String commentID = ref.push().getKey();
        ref.child(commentID).setValue(new Message(uID, intent.getStringExtra("url")));





        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}