package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

/** Plays one video and shows who made it. The uploader can delete it here. */
public class VideoPlayerActivity extends AppCompatActivity {
    private static final String EXTRA_VIDEO_ID = "video_id";
    private static final String STATE_POSITION = "position";
    private static final String STATE_PLAYING = "playing";

    private android.widget.VideoView videoView;
    private int resumePosition;
    private boolean resumePlaying = true;

    public static Intent intent(Context context, String videoId) {
        return new Intent(context, VideoPlayerActivity.class).putExtra(EXTRA_VIDEO_ID, videoId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String uid = Session.requireUser(this);
        if (uid == null) {
            return;
        }
        DBHandler db = DBHandler.getInstance(this);
        String videoId = getIntent().getStringExtra(EXTRA_VIDEO_ID);
        Video video = videoId == null ? null : db.getVideo(videoId);
        File file = video == null ? null : MediaFiles.videoFile(this, video.getFileName());
        if (video == null || !file.exists()) {
            Toast.makeText(this, R.string.video_unavailable, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (savedInstanceState != null) {
            resumePosition = savedInstanceState.getInt(STATE_POSITION);
            resumePlaying = savedInstanceState.getBoolean(STATE_PLAYING, true);
        }

        // The video stage is black, so the status bar icons stay light
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT));
        setContentView(R.layout.activity_video_player);
        View stage = findViewById(R.id.videoStage);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            stage.setPadding(0, bars.top, 0, 0);
            return insets;
        });

        videoView = findViewById(R.id.videoView);
        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);
        videoView.setOnPreparedListener(player -> {
            if (resumePosition > 0) {
                videoView.seekTo(resumePosition);
            }
            if (resumePlaying) {
                videoView.start();
            }
        });
        videoView.setOnErrorListener((player, what, extra) -> {
            Toast.makeText(this, R.string.video_unavailable, Toast.LENGTH_SHORT).show();
            return true;
        });
        videoView.setVideoPath(file.getAbsolutePath());
        findViewById(R.id.btnVideoBack).setOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.tvPlayerTitle)).setText(video.getTitle());
        Ui.bindAvatar(findViewById(R.id.tvPlayerAvatar), video.getUploader().getUid(),
                video.getUploader().getDisplayName());
        ((TextView) findViewById(R.id.tvPlayerUploader)).setText(video.getUploader().getDisplayName());
        ((TextView) findViewById(R.id.tvPlayerTime)).setText(Ui.timeAgo(this, video.getCreatedAt()));
        findViewById(R.id.uploaderRow).setOnClickListener(v ->
                startActivity(ProfileActivity.intent(this, video.getUploader().getUid())));

        TextView description = findViewById(R.id.tvPlayerDescription);
        description.setText(video.getDescription());
        description.setVisibility(video.getDescription().isEmpty() ? View.GONE : View.VISIBLE);

        View delete = findViewById(R.id.btnDeleteVideo);
        boolean mine = video.getUploader().getUid().equals(uid);
        delete.setVisibility(mine ? View.VISIBLE : View.GONE);
        delete.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.video_delete)
                .setMessage(R.string.video_delete_confirm)
                .setPositiveButton(R.string.video_delete, (d, which) -> {
                    Video removed = db.deleteVideo(video.getId(), uid);
                    if (removed != null) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                        if (!removed.getThumbName().isEmpty()) {
                            //noinspection ResultOfMethodCallIgnored
                            MediaFiles.videoFile(this, removed.getThumbName()).delete();
                        }
                        Toast.makeText(this, R.string.video_deleted, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null) {
            resumePlaying = videoView.isPlaying();
            resumePosition = videoView.getCurrentPosition();
            videoView.pause();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (videoView != null) {
            outState.putInt(STATE_POSITION, videoView.getCurrentPosition());
            outState.putBoolean(STATE_PLAYING, resumePlaying);
        }
    }
}
