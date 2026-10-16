package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Everyone's videos, newest first, with a button to upload your own. */
public class VideoFeedActivity extends AppCompatActivity {
    private String uid;
    private DBHandler db;
    private VideoAdapter adapter;
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uid = Session.requireUser(this);
        if (uid == null) {
            return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_video_feed);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        db = DBHandler.getInstance(this);

        RecyclerView rvVideos = findViewById(R.id.rvVideos);
        emptyView = findViewById(R.id.tvVideosEmpty);
        adapter = new VideoAdapter(video ->
                startActivity(VideoPlayerActivity.intent(this, video.getId())));
        rvVideos.setAdapter(adapter);

        findViewById(R.id.btnVideosBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabUploadVideo).setOnClickListener(v ->
                startActivity(new Intent(this, UploadVideoActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            // A video may have been uploaded or deleted on another screen
            List<Video> videos = db.getVideos(uid);
            adapter.setVideos(videos);
            emptyView.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}
