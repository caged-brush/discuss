package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Article cards used by the browse list and the profile's recent-articles list. */
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
        Ui.bindCategoryTile(holder.tile, article.getCategory());
        holder.category.setText(article.getCategory());
        holder.category.setTextColor(Ui.categoryTextColor(holder.itemView.getContext(),
                article.getCategory()));
        holder.title.setText(article.getTitle());
        // Paragraph breaks would waste the few preview lines
        holder.thumbnail.setText(article.getBody().replace("\n", " "));
        holder.rating.setText(Ui.rating(holder.itemView.getContext(), article));
        holder.author.setText(article.getAuthor());
        holder.itemView.setOnClickListener(v -> listener.onArticleClick(article));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class article_browse_recycler_view_model extends RecyclerView.ViewHolder {
        final TextView tile, category, title, thumbnail, rating, author;

        public article_browse_recycler_view_model(@NonNull View itemView) {
            super(itemView);
            tile = itemView.findViewById(R.id.article_browse_recycler_tile);
            category = itemView.findViewById(R.id.article_browse_recycler_category);
            title = itemView.findViewById(R.id.article_browse_recycler_article_title);
            thumbnail = itemView.findViewById(R.id.article_browse_recycler_thumbnail_text);
            rating = itemView.findViewById(R.id.article_browse_recycler_rating);
            author = itemView.findViewById(R.id.article_browse_recycler_author);
        }
    }
}
