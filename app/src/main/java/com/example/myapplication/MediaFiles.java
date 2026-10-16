package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

/**
 * Where photos, voice messages and videos live, and the helpers that prepare them. Everything is
 * stored in the app's private folder; the database keeps only file names.
 */
final class MediaFiles {
    private static final int MAX_IMAGE_EDGE = 1600;
    private static final int THUMBNAIL_WIDTH = 640;

    private MediaFiles() { }

    /** Opens a fresh stream each time, because decoding needs several passes. */
    interface StreamSource {
        InputStream open() throws IOException;
    }

    // ------------------------------------------------------------------ files

    static File chatDir(Context context) {
        return dir(context, "chat_media");
    }

    static File videoDir(Context context) {
        return dir(context, "videos");
    }

    private static File dir(Context context, String name) {
        File dir = new File(context.getFilesDir(), name);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return dir;
    }

    static File chatFile(Context context, String name) {
        return new File(chatDir(context), name);
    }

    static File videoFile(Context context, String name) {
        return new File(videoDir(context), name);
    }

    static File newFile(File dir, String prefix, String extension) {
        return new File(dir, prefix + UUID.randomUUID().toString().replace("-", "") + extension);
    }

    /** A content:// Uri the camera app is allowed to write to. */
    static Uri uriFor(Context context, File file) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

    static void copy(InputStream in, File target) throws IOException {
        try (InputStream input = in; OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    // ----------------------------------------------------------------- photos

    /**
     * Decodes a photo, turns it upright, shrinks it and saves it as a JPEG in the chat media
     * folder. Returns the new file name. Blocking: call from a background thread.
     */
    static String saveChatImage(Context context, StreamSource source) throws IOException {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream in = source.open()) {
            orientation = new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException ignored) {
            // No EXIF data (for example a PNG): assume the picture is already upright
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = source.open()) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Not an image");
        }
        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_IMAGE_EDGE) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream in = source.open()) {
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }
        if (bitmap == null) {
            throw new IOException("Could not decode image");
        }

        Matrix matrix = matrixFor(orientation);
        if (matrix != null) {
            Bitmap upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
            if (upright != bitmap) {
                bitmap.recycle();
            }
            bitmap = upright;
        }

        File out = newFile(chatDir(context), "img_", ".jpg");
        try (FileOutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        } finally {
            bitmap.recycle();
        }
        return out.getName();
    }

    private static Matrix matrixFor(int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.postRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.postRotate(270);
                matrix.postScale(-1, 1);
                break;
            default:
                return null;
        }
        return matrix;
    }

    // ----------------------------------------------------------------- videos

    /** Length of a video or audio file in milliseconds, or 0 if it cannot be read. */
    static long durationOf(File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            return parseDuration(retriever);
        } catch (RuntimeException e) {
            return 0;
        } finally {
            release(retriever);
        }
    }

    /** Same as {@link #durationOf(File)} for a Uri the user picked. */
    static long durationOf(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            return parseDuration(retriever);
        } catch (RuntimeException e) {
            return 0;
        } finally {
            release(retriever);
        }
    }

    private static void release(MediaMetadataRetriever retriever) {
        try {
            retriever.release();
        } catch (IOException ignored) {
            // Nothing useful can be done if releasing fails
        }
    }

    private static long parseDuration(MediaMetadataRetriever retriever) {
        String value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        try {
            return value == null ? 0 : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** One frame of a picked video, for the preview; null if it cannot be read. */
    static Bitmap frameOf(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            return scaledFrame(retriever);
        } catch (RuntimeException e) {
            return null;
        } finally {
            release(retriever);
        }
    }

    /** Saves a thumbnail next to the video. Returns its file name, or "" if none could be made. */
    static String saveVideoThumbnail(Context context, File video) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(video.getAbsolutePath());
            Bitmap frame = scaledFrame(retriever);
            if (frame == null) {
                return "";
            }
            File out = newFile(videoDir(context), "thumb_", ".jpg");
            try (FileOutputStream stream = new FileOutputStream(out)) {
                frame.compress(Bitmap.CompressFormat.JPEG, 85, stream);
            } finally {
                frame.recycle();
            }
            return out.getName();
        } catch (RuntimeException | IOException e) {
            return "";
        } finally {
            release(retriever);
        }
    }

    private static Bitmap scaledFrame(MediaMetadataRetriever retriever) {
        // A frame a little way in is usually better than a black first frame
        Bitmap frame = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        if (frame == null) {
            frame = retriever.getFrameAtTime(0);
        }
        if (frame == null || frame.getWidth() <= THUMBNAIL_WIDTH) {
            return frame;
        }
        int height = Math.round(frame.getHeight() * (THUMBNAIL_WIDTH / (float) frame.getWidth()));
        Bitmap scaled = Bitmap.createScaledBitmap(frame, THUMBNAIL_WIDTH, height, true);
        if (scaled != frame) {
            frame.recycle();
        }
        return scaled;
    }

    // ------------------------------------------------------------------- text

    /** "0:07", "12:30" or "1:02:03". */
    static String formatDuration(long millis) {
        long totalSeconds = Math.round(millis / 1000.0);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
}
