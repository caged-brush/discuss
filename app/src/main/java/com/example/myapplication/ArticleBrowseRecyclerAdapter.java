package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Article cards used by the browse grid and the profile's recent-articles list. */
public class ArticleBrowseRecyclerAdapter extends RecyclerView.Adapter<ArticleBrowseRecyclerAdapter.article_browse_recycler_view_model> {

    public interface OnArticleClickListener {
        void onArticleClick(Article article);
    }

    private final List<Article> data = new ArrayList<>();
    private final OnArticleClickListener listener;

    public ArticleBrowseRecyclerAdapter(OnArticleClickListener listener) {
        this.listener = listener;
    }

    public void setArticles(List<Article> articles) {
        data.clear();
        data.addAll(articles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public article_browse_recycler_view_model onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.article_browse_recycler_view_model, parent, false);
        return new article_browse_recycler_view_model(view);
    }

    @Override
    public void onBindViewHolder(@NonNull article_browse_recycler_view_model holder, int position) {
        Article article = data.get(position);
        holder.category.setText(article.getCategory());
        holder.title.setText(article.getTitle());
        holder.thumbnail.setText(article.getBody());
        holder.rating.setText(formatRating(holder.itemView, article));
        holder.itemView.setOnClickListener(v -> listener.onArticleClick(article));
    }

    static String formatRating(View view, Article article) {
        if (article.getRatingCount() == 0) {
            return view.getContext().getString(R.string.rating_none);
        }
        return view.getContext().getString(R.string.rating_summary,
                article.getAverageRating(), article.getRatingCount());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class article_browse_recycler_view_model extends RecyclerView.ViewHolder {
        TextView category, title, thumbnail, rating;

        public article_browse_recycler_view_model(@NonNull View itemView) {
            super(itemView);
            category = itemView.findViewById(R.id.article_browse_recycler_category);
            title = itemView.findViewById(R.id.article_browse_recycler_article_title);
            thumbnail = itemView.findViewById(R.id.article_browse_recycler_thumbnail_text);
            rating = itemView.findViewById(R.id.article_browse_recycler_rating);
        }
    }
}
