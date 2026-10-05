package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class ArticleBrowse extends AppCompatActivity {
    int CBcollapsed = 20;
    int CBexpanded = 75;
    Boolean isCBexpanded = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_article_browse);


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



        ArrayList<ArticleBrowseRecyclerDataModel> data = new ArrayList<>();
        ArrayList<ArticleBrowseRecyclerDataModel> data2 = new ArrayList<>();

        for(int i = 0; i < 12; i++){
            data.add(new ArticleBrowseRecyclerDataModel("temp Title", getResources().getString(R.string.ArticleBrowseRecyclerLoremIpsum), i));
            data2.add(new ArticleBrowseRecyclerDataModel("temp2 Title", getResources().getString(R.string.ArticleBrowseRecyclerLoremIpsum), 12 * i + 1));
        }
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

        ArticleBrowseRecyclerAdapter adapter = new ArticleBrowseRecyclerAdapter(data, this);

        recycler.setAdapter(adapter);

        GridLayoutManager glm = new GridLayoutManager(this, 2);
        recycler.setLayoutManager(glm);


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