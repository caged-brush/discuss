package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

final class SavedArticle {
    final String id;
    final String title;
    final String url;
    final String author;
    final String description;

    private SavedArticle(Cursor cursor) {
        id = cursor.getString(cursor.getColumnIndexOrThrow("article_id"));
        title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        url = cursor.getString(cursor.getColumnIndexOrThrow("source_url"));
        author = cursor.getString(cursor.getColumnIndexOrThrow("author"));
        description = cursor.getString(cursor.getColumnIndexOrThrow("content_snippet"));
    }

    static List<SavedArticle> load(DBHandler db, String uid) {
        List<SavedArticle> articles = new ArrayList<>();
        try (Cursor cursor = db.getSavedArticles(uid)) {
            while (cursor.moveToNext()) {
                SavedArticle article = new SavedArticle(cursor);
                if (ArticleView.isArticleUrl(article.url)) articles.add(article);
            }
        }
        return articles;
    }

    void open(Context context) {
        context.startActivity(ArticleView.intent(context, title, url, author, description));
    }
}
