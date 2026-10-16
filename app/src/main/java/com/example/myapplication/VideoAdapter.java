package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Video cards used by the video feed and by profiles. */
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    public interface OnVideoClickListener {
        void onVideoClick(Video video);
    }

    private final List<Video> videos = new ArrayList<>();
    private final OnVideoClickListener listener;

    public VideoAdapter(OnVideoClickListener listener) {
        this.listener = listener;
    }

    public void setVideos(List<Video> newVideos) {
        videos.clear();
        videos.addAll(newVideos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VideoViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videos.get(position);
        Context context = holder.itemView.getContext();

        if (video.getThumbName().isEmpty()) {
            holder.thumb.setTag(R.id.image_loader_tag, null);
            holder.thumb.setImageDrawable(null);
        } else {
            ImageLoader.load(holder.thumb, MediaFiles.videoFile(context, video.getThumbName()),
                    Ui.dp(context, 360));
        }
        holder.duration.setVisibility(video.getDurationMs() > 0 ? View.VISIBLE : View.GONE);
        holder.duration.setText(MediaFiles.formatDuration(video.getDurationMs()));
        holder.title.setText(video.getTitle());
        Ui.bindAvatar(holder.avatar, video.getUploader().getUid(),
                video.getUploader().getDisplayName());
        holder.uploader.setText(video.getUploader().getDisplayName());
        holder.time.setText(Ui.timeAgo(context, video.getCreatedAt()));
        holder.itemView.setOnClickListener(v -> listener.onVideoClick(video));
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView duration, title, avatar, uploader, time;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.ivVideoThumb);
            duration = itemView.findViewById(R.id.tvVideoDuration);
            title = itemView.findViewById(R.id.tvVideoTitle);
            avatar = itemView.findViewById(R.id.tvVideoAvatar);
            uploader = itemView.findViewById(R.id.tvVideoUploader);
            time = itemView.findViewById(R.id.tvVideoTime);
        }
    }
}
