package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

public class ArticleView extends SignedInActivity {
    static Intent intent(Context context, String title, String url, String author, String description) {
        return new Intent(context, ArticleView.class)
                .putExtra("title", title)
                .putExtra("url", url)
                .putExtra("author", author)
                .putExtra("desc", description);
    }

    static boolean isArticleUrl(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null && !uri.getHost().isEmpty();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        String url = getIntent().getStringExtra("url");
        if (!isArticleUrl(url)) {
            showMessage("Article unavailable.");
            finish();
            return;
        }
        setContentView(R.layout.activity_article_view);

        String title = getIntent().getStringExtra("title");
        String articleTitle = title == null || title.trim().isEmpty() ? url : title.trim();
        TextView heading = findViewById(R.id.article_view_title);
        heading.setText(articleTitle);

        WebView webView = findViewById(R.id.articleBody);
        webView.loadUrl(url);

        Button save = findViewById(R.id.saveToLib);
        save.setOnClickListener(view -> {
            db.saveArticle(articleTitle, getIntent().getStringExtra("author"),
                    getIntent().getStringExtra("desc"), url, currentUid);
            showMessage("Article saved to your library.");
        });

        findViewById(R.id.shareArticle).setOnClickListener(view -> {
            Intent share = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, url);
            startActivity(Intent.createChooser(share, null));
        });
    }
}
