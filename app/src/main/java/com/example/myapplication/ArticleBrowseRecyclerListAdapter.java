package com.example.myapplication;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ArticleBrowseRecyclerListAdapter extends ListAdapter<ArticleBrowseRecyclerDataModel, ArticleBrowseRecyclerListAdapter.article_browse_recycler_list_view_model> {
    Context context;


    private static final DiffUtil.ItemCallback<ArticleBrowseRecyclerDataModel> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ArticleBrowseRecyclerDataModel>() {

                @Override
                public boolean areItemsTheSame(@NonNull ArticleBrowseRecyclerDataModel oldItem, @NonNull ArticleBrowseRecyclerDataModel newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ArticleBrowseRecyclerDataModel oldItem, @NonNull ArticleBrowseRecyclerDataModel newItem) {
                    return oldItem.getId() == newItem.getId();
                }
            };
    public ArticleBrowseRecyclerListAdapter(Context context){
        super(DIFF_CALLBACK);
        this.context = context;
    }



    @NonNull
    @Override
    public article_browse_recycler_list_view_model onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull article_browse_recycler_list_view_model holder, int position) {

    }

    public class article_browse_recycler_list_view_model extends RecyclerView.ViewHolder{
        TextView title, thumbnail;
        public article_browse_recycler_list_view_model(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.article_browse_recycler_article_title);
            thumbnail = itemView.findViewById(R.id.article_browse_recycler_thumbnail_text);
        }
    }
}
