package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Shows a chat photo full screen. */
public class ImageViewerActivity extends AppCompatActivity {
    private static final String EXTRA_FILE = "file";

    public static Intent intent(Context context, String chatImageFileName) {
        return new Intent(context, ImageViewerActivity.class).putExtra(EXTRA_FILE, chatImageFileName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Black background, so keep the system bar icons light
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_image_viewer);

        View close = findViewById(R.id.btnCloseViewer);
        close.setOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) close.getLayoutParams();
            params.topMargin = bars.top + Ui.dp(this, 12);
            close.setLayoutParams(params);
            return insets;
        });

        String name = getIntent().getStringExtra(EXTRA_FILE);
        if (name == null) {
            finish();
            return;
        }
        ImageLoader.load(findViewById(R.id.ivFullImage), MediaFiles.chatFile(this, name), 2048);
    }
}
