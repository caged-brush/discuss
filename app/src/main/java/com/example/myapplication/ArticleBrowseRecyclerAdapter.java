package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.kwabenaberko.newsapilib.models.Article;

import java.util.Objects;

final class ArticleBrowseRecyclerAdapter extends ListAdapter<Article, ArticleBrowseRecyclerAdapter.ArticleHolder> {
    ArticleBrowseRecyclerAdapter() {
        super(new DiffUtil.ItemCallback<Article>() {
            @Override
            public boolean areItemsTheSame(@NonNull Article oldItem, @NonNull Article newItem) {
                return oldItem.getUrl().equals(newItem.getUrl());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Article oldItem, @NonNull Article newItem) {
                return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                        && Objects.equals(oldItem.getDescription(), newItem.getDescription())
                        && Objects.equals(oldItem.getAuthor(), newItem.getAuthor());
            }
        });
    }

    @NonNull
    @Override
    public ArticleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ArticleHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.article_browse_recycler_view_model, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleHolder holder, int position) {
        Article article = getItem(position);
        String title = article.getTitle();
        String description = article.getDescription();
        holder.title.setText(title == null || title.trim().isEmpty() ? article.getUrl() : title);
        holder.description.setText(description);
        holder.description.setVisibility(description == null || description.trim().isEmpty()
                ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(view -> view.getContext().startActivity(ArticleView.intent(
                view.getContext(), title, article.getUrl(), article.getAuthor(), description)));
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
