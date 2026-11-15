package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kwabenaberko.newsapilib.models.Article;

import java.util.ArrayList;

public class ArticleBrowseRecyclerAdapter extends RecyclerView.Adapter<ArticleBrowseRecyclerAdapter.ArticleHolder> {
    private final ArrayList<Article> articles;
    private final Context context;

    public ArticleBrowseRecyclerAdapter(ArrayList<Article> articles, Context context) {
        this.articles = articles;
        this.context = context;
    }

    @NonNull
    @Override
    public ArticleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ArticleHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.article_browse_recycler_view_model, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleHolder holder, int position) {
        Article article = articles.get(position);
        holder.title.setText(article.getTitle());
        holder.description.setText(article.getDescription());
        holder.itemView.setOnClickListener(view -> context.startActivity(ArticleView.intent(
                context, article.getTitle(), article.getUrl(), article.getAuthor(), article.getDescription())));
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    public void updateData(ArrayList<Article> updated) {
        articles.clear();
        for (Article article : updated) {
            if (article != null && ArticleView.isArticleUrl(article.getUrl())) articles.add(article);
        }
        notifyDataSetChanged();
    }

    static final class ArticleHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView description;

        ArticleHolder(View view) {
            super(view);
            title = view.findViewById(R.id.article_browse_recycler_article_title);
            description = view.findViewById(R.id.article_browse_recycler_thumbnail_text);
        }
    }
}
