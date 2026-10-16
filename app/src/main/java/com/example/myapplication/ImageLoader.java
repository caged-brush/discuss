package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Loads downsized images off the UI thread and keeps recently used ones in memory. */
final class ImageLoader {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 8)) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    private ImageLoader() { }

    /** Shows the file scaled so its longer side is at most maxEdgePx; clears the view if missing. */
    static void load(ImageView view, File file, int maxEdgePx) {
        String key = file.getAbsolutePath() + "@" + maxEdgePx;
        // RecyclerView reuses views, so only the latest request may set the picture
        view.setTag(R.id.image_loader_tag, key);
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        view.setImageDrawable(null);
        POOL.execute(() -> {
            Bitmap bitmap = decode(file, maxEdgePx);
            if (bitmap == null) {
                return;
            }
            CACHE.put(key, bitmap);
            view.post(() -> {
                if (key.equals(view.getTag(R.id.image_loader_tag))) {
                    view.setImageBitmap(bitmap);
                }
            });
        });
    }

    private static Bitmap decode(File file, int maxEdgePx) {
        if (!file.exists()) {
            return null;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0) {
            return null;
        }
        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdgePx) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }
}
