package com.example.myapplication;

import android.content.ActivityNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Pick or record a video, give it a title and publish it to the video feed. */
public class UploadVideoActivity extends AppCompatActivity {
    private static final int MAX_VIDEO_MB = 300;
    private static final String STATE_PICKED = "picked";
    private static final String STATE_RECORDED = "recorded";
    private static final String STATE_PENDING = "pending";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private DBHandler db;
    private String uid;
    private TextInputEditText etTitle, etDescription;
    private ImageView ivPreview;
    private TextView tvEmpty, tvDuration;
    private Button btnPublish;
    private View progress;

    // Exactly one of these identifies the chosen video
    private Uri pickedUri;
    private File recordedFile;
    // File the camera app is currently writing to
    private File pendingRecording;
    private boolean busy;
    private boolean published;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickVideo = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), this::onVideoPicked);
    private final ActivityResultLauncher<Uri> recordVideo = registerForActivityResult(
            new ActivityResultContracts.CaptureVideo(), this::onVideoRecorded);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uid = Session.requireUser(this);
        if (uid == null) {
            return;
        }
        db = DBHandler.getInstance(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upload_video);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        etTitle = findViewById(R.id.etVideoTitle);
        etDescription = findViewById(R.id.etVideoDescription);
        ivPreview = findViewById(R.id.ivUploadPreview);
        tvEmpty = findViewById(R.id.tvUploadEmpty);
        tvDuration = findViewById(R.id.tvUploadDuration);
        btnPublish = findViewById(R.id.btnPublishVideo);
        progress = findViewById(R.id.progressUpload);

        findViewById(R.id.btnUploadBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnChooseVideo).setOnClickListener(v -> pickVideo.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly.INSTANCE)
                        .build()));
        findViewById(R.id.btnRecordVideo).setOnClickListener(v -> startRecording());
        btnPublish.setOnClickListener(v -> publish());
        etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                updatePublishButton();
            }
        });

        if (savedInstanceState != null) {
            String picked = savedInstanceState.getString(STATE_PICKED);
            String recorded = savedInstanceState.getString(STATE_RECORDED);
            String pending = savedInstanceState.getString(STATE_PENDING);
            pickedUri = picked == null ? null : Uri.parse(picked);
            recordedFile = recorded == null ? null : new File(recorded);
            pendingRecording = pending == null ? null : new File(pending);
            if (pickedUri != null || recordedFile != null) {
                loadPreview();
            }
        }
        updatePublishButton();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pickedUri != null) {
            outState.putString(STATE_PICKED, pickedUri.toString());
        }
        if (recordedFile != null) {
            outState.putString(STATE_RECORDED, recordedFile.getAbsolutePath());
        }
        if (pendingRecording != null) {
            outState.putString(STATE_PENDING, pendingRecording.getAbsolutePath());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.shutdown();
        // A recording that was never published would otherwise stay on disk forever
        if (isFinishing() && !published && recordedFile != null) {
            //noinspection ResultOfMethodCallIgnored
            recordedFile.delete();
        }
    }

    // ---------------------------------------------------------------- choosing

    private void startRecording() {
        File file = MediaFiles.newFile(MediaFiles.videoDir(this), "rec_", ".mp4");
        try {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
        } catch (IOException e) {
            toast(getString(R.string.video_unreadable));
            return;
        }
        pendingRecording = file;
        try {
            recordVideo.launch(MediaFiles.uriFor(this, file));
        } catch (ActivityNotFoundException e) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            pendingRecording = null;
            toast(getString(R.string.chat_camera_unavailable));
        }
    }

    private void onVideoRecorded(boolean saved) {
        File file = pendingRecording;
        pendingRecording = null;
        if (file == null) {
            return;
        }
        if (!saved || file.length() == 0) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            return;
        }
        discardRecording();
        recordedFile = file;
        pickedUri = null;
        loadPreview();
    }

    private void onVideoPicked(Uri uri) {
        if (uri == null) {
            return;
        }
        long size = sizeOf(uri);
        if (size > MAX_VIDEO_MB * 1024L * 1024L) {
            toast(getString(R.string.video_too_large, MAX_VIDEO_MB));
            return;
        }
        discardRecording();
        pickedUri = uri;
        recordedFile = null;
        loadPreview();
    }

    private void discardRecording() {
        if (recordedFile != null) {
            //noinspection ResultOfMethodCallIgnored
            recordedFile.delete();
            recordedFile = null;
        }
    }

    private long sizeOf(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getLong(0);
            }
        } catch (RuntimeException ignored) {
            // Size is only used for a friendly warning, so unknown is fine
        }
        return -1;
    }

    /** Shows a frame and the length of the chosen video, reading it off the UI thread. */
    private void loadPreview() {
        Uri source = pickedUri != null ? pickedUri : Uri.fromFile(recordedFile);
        worker.execute(() -> {
            Bitmap frame = MediaFiles.frameOf(this, source);
            long duration = MediaFiles.durationOf(this, source);
            handler.post(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (frame == null && duration == 0) {
                    pickedUri = null;
                    recordedFile = null;
                    toast(getString(R.string.video_unreadable));
                    showPreview(null, 0);
                } else {
                    showPreview(frame, duration);
                }
            });
        });
    }

    private void showPreview(Bitmap frame, long durationMs) {
        boolean has = pickedUri != null || recordedFile != null;
        ivPreview.setVisibility(has ? View.VISIBLE : View.GONE);
        tvEmpty.setVisibility(has ? View.GONE : View.VISIBLE);
        tvDuration.setVisibility(has && durationMs > 0 ? View.VISIBLE : View.GONE);
        ivPreview.setImageBitmap(frame);
        tvDuration.setText(MediaFiles.formatDuration(durationMs));
        updatePublishButton();
    }

    private void updatePublishButton() {
        boolean hasVideo = pickedUri != null || recordedFile != null;
        boolean hasTitle = etTitle.getText() != null && !etTitle.getText().toString().trim().isEmpty();
        btnPublish.setEnabled(hasVideo && hasTitle && !busy);
    }

    // --------------------------------------------------------------- publishing

    private void publish() {
        String title = String.valueOf(etTitle.getText()).trim();
        String description = String.valueOf(etDescription.getText()).trim();
        Uri source = pickedUri;
        File recorded = recordedFile;
        if (title.isEmpty() || (source == null && recorded == null)) {
            return;
        }
        setBusy(true);
        worker.execute(() -> {
            File target = recorded;
            try {
                if (target == null) {
                    target = MediaFiles.newFile(MediaFiles.videoDir(this), "vid_", ".mp4");
                    InputStream in = getContentResolver().openInputStream(source);
                    if (in == null) {
                        throw new FileNotFoundException(source.toString());
                    }
                    MediaFiles.copy(in, target);
                }
                long duration = MediaFiles.durationOf(target);
                if (duration <= 0) {
                    throw new IOException("Not a playable video");
                }
                String thumb = MediaFiles.saveVideoThumbnail(this, target);
                db.addVideo(uid, title, description, target.getName(), thumb, duration);
                published = true;
                handler.post(() -> {
                    toast(getString(R.string.video_published));
                    finish();
                });
            } catch (IOException | RuntimeException e) {
                if (recorded == null && target != null) {
                    //noinspection ResultOfMethodCallIgnored
                    target.delete();
                }
                handler.post(() -> {
                    setBusy(false);
                    toast(getString(R.string.video_publish_failed));
                });
            }
        });
    }

    private void setBusy(boolean value) {
        busy = value;
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnChooseVideo).setEnabled(!value);
        findViewById(R.id.btnRecordVideo).setEnabled(!value);
        updatePublishButton();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
