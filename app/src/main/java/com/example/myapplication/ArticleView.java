package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

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
        Toast.makeText(this, url, Toast.LENGTH_SHORT).show();
        titleT.setText(title);
        bodyT.loadUrl(url);
        Button save = findViewById(R.id.saveToLib);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DBHandler db = new DBHandler(ArticleView.this);

                //db.saveArticle(title, author, );
            }
        });





        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}