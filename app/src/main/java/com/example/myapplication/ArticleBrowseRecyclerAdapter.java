package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ArticleBrowseRecyclerAdapter extends RecyclerView.Adapter<ArticleBrowseRecyclerAdapter.article_browse_recycler_view_model> {
    ArrayList<ArticleBrowseRecyclerDataModel> data;
    Context context;

    public ArticleBrowseRecyclerAdapter(ArrayList<ArticleBrowseRecyclerDataModel> data, Context context){
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
    public void onBindViewHolder(@NonNull article_browse_recycler_view_model holder, int position) {
        holder.title.setText(data.get(position).getTitle());
        holder.thumbnail.setText(data.get(position).getThumbnail());
    }

    @Override
    public int getItemCount() {
        return data.size();
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
