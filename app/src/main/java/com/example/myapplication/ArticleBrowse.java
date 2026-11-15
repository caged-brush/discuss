package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class ArticleBrowse extends SignedInActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        setContentView(R.layout.activity_article_browse);

        RecyclerView recycler = findViewById(R.id.article_browse_recycler);
        ArticleBrowseRecyclerAdapter adapter = new ArticleBrowseRecyclerAdapter(new ArrayList<>(), this);
        recycler.setAdapter(adapter);

        TextView status = findViewById(R.id.articleStatus);
        News_Fetcher news = new ViewModelProvider(this).get(News_Fetcher.class);
        news.getArticles().observe(this, articles -> {
            adapter.updateData(articles);
            status.setText("No articles found.");
            status.setVisibility(articles.isEmpty() ? View.VISIBLE : View.GONE);
        });
        news.getError().observe(this, error -> {
            if (error != null) {
                status.setText("Unable to load articles. Try refreshing.");
                status.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.article_browse_refresh_button).setOnClickListener(view -> fetchArticles(news, status));
        if (savedInstanceState == null) fetchArticles(news, status);

        findViewById(R.id.openContacts).setOnClickListener(view ->
                startActivity(new Intent(this, ContactActivity.class)));
        findViewById(R.id.openGroups).setOnClickListener(view ->
                startActivity(new Intent(this, GroupListActivity.class)));
        findViewById(R.id.accountMenu).setOnClickListener(view ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Account")
                        .setItems(new String[]{"My profile", "Sign out"}, (dialog, item) -> {
                            if (item == 0) startActivity(new Intent(this, ProfileActivity.class));
                            else signOut();
                        })
                        .show());
    }

    private void fetchArticles(News_Fetcher news, TextView status) {
        status.setText("Loading articles…");
        status.setVisibility(View.VISIBLE);
        news.fetchArticles("economics + europe");
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, WelcomePage.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
}
