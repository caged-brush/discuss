package com.example.myapplication;

import static kotlinx.coroutines.future.FutureKt.await;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Collections;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kwabenaberko.newsapilib.NewsApiClient;
import com.kwabenaberko.newsapilib.models.Article;
import com.kwabenaberko.newsapilib.models.request.EverythingRequest;
import com.kwabenaberko.newsapilib.models.response.ArticleResponse;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class ArticleBrowse extends AppCompatActivity {
    int CBcollapsed = 20;
    int CBexpanded = 75;
    Boolean isCBexpanded = true;

    ArrayList<Article> articles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_article_browse);

        News_Fetcher newsFetcher = new ViewModelProvider(this).get(News_Fetcher.class);
        newsFetcher.fetchArticles("economics + europe");

        Button testBtn = findViewById(R.id.testButton);
        Button economics = findViewById(R.id.article_browse_economics);
        RecyclerView recycler = findViewById(R.id.article_browse_recycler);
        FrameLayout contactBar = findViewById(R.id.article_browse_contactBar);
        AppCompatImageButton contactButton = findViewById(R.id.article_browse_contact_button);


        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ArticleBrowse.this, ArticleView.class);
                startActivity(intent);
            }
        });
        ArticleBrowseRecyclerAdapter adapter = new ArticleBrowseRecyclerAdapter(articles, ArticleBrowse.this);
        newsFetcher.getArticles().observe(this, list -> {
            adapter.updateData(list);
        });
        recycler.setAdapter(adapter);
        LinearLayoutManager llm = new LinearLayoutManager(ArticleBrowse.this, LinearLayoutManager.VERTICAL, false);
        //GridLayoutManager glm = new GridLayoutManager(ArticleBrowse.this, 2);
        recycler.setLayoutManager(llm);

        Button refresh = findViewById(R.id.article_browse_refresh_button);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newsFetcher.fetchArticles("economics + europe");
                newsFetcher.getArticles().observe(ArticleBrowse.this, list -> {
                    Collections.shuffle(list);
                    adapter.updateData(list);
                });
            }
        });



        /*
        https://stackoverflow.com/questions/2963152/how-to-resize-a-custom-view-programmatically
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) someLayout.getLayoutParams();
            params.height = 130;
            someLayout.setLayoutParams(params);*/

        contactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCBexpanded = changeContactBar(isCBexpanded, contactBar);

            }
        });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public Boolean changeContactBar(Boolean isCBexpanded, FrameLayout contactBar){
        if(isCBexpanded){
            ViewGroup.LayoutParams params = contactBar.getLayoutParams();
            params.width = (int) getResources().getDimension(R.dimen.ArticleBrowseCollapsedCB);
            //params.height = 732;
            contactBar.setLayoutParams(params);

        }else{
            ViewGroup.LayoutParams params = contactBar.getLayoutParams();
            params.width = (int) getResources().getDimension(R.dimen.ArticleBrowseExpandedCB);
           // params.height = 732;
            contactBar.setLayoutParams(params);

        }

        return !isCBexpanded;
    }


}