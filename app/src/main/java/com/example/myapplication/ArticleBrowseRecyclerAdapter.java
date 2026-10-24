package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kwabenaberko.newsapilib.models.Article;

import java.util.ArrayList;

public class ArticleBrowseRecyclerAdapter extends RecyclerView.Adapter<ArticleBrowseRecyclerAdapter.article_browse_recycler_view_model> {
    ArrayList<Article> data;
    Context context;

    public ArticleBrowseRecyclerAdapter(ArrayList<Article> data, Context context){
        this.data = data;
        this.context = context;
    }

    @NonNull
    @Override
    public article_browse_recycler_view_model onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.article_browse_recycler_view_model, parent, false);
        return new article_browse_recycler_view_model(view);
    }

    @Override
    public void onBindViewHolder(@NonNull article_browse_recycler_view_model holder, @SuppressLint("RecyclerView") int position) {
        holder.title.setText(data.get(position).getTitle());
        holder.thumbnail.setText(data.get(position).getDescription());
        holder.thumbnail.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(context, ArticleView.class);
                intent.putExtra("title", data.get(position).getTitle());
                intent.putExtra("url", data.get(position).getUrl());
                intent.putExtra("source", data.get(position).getSource().toString());
                intent.putExtra("desc", data.get(position).getDescription());
                intent.putExtra("author", data.get(position).getAuthor());
                context.startActivity(intent);
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void updateData(ArrayList<Article> articles){
        data.clear();
        data.addAll(articles);
        notifyDataSetChanged();
    }

    public class article_browse_recycler_view_model extends RecyclerView.ViewHolder{
        TextView title, thumbnail;
        public article_browse_recycler_view_model(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.article_browse_recycler_article_title);
            thumbnail = itemView.findViewById(R.id.article_browse_recycler_thumbnail_text);
        }
    }
}
