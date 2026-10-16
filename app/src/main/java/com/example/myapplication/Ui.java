package com.example.myapplication;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

/** Small presentation helpers shared by the screens. */
final class Ui {
    private static final int[] AVATAR_COLORS = {
            0xFF4F46E5, 0xFF0D9488, 0xFFB45309, 0xFFDB2777,
            0xFF2563EB, 0xFF7C3AED, 0xFF059669, 0xFFE11D48};

    private Ui() { }

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------------ avatars

    /** Stable colour for a user or group, so the same person always looks the same. */
    static int avatarColor(String key) {
        return AVATAR_COLORS[(key.hashCode() & 0x7fffffff) % AVATAR_COLORS.length];
    }

    /** Up to two initials from a name, e.g. "Alice Reader" gives "AR". */
    static String initials(String name) {
        StringBuilder out = new StringBuilder();
        for (String part : name.trim().split("[^\\p{L}\\p{N}]+")) {
            if (!part.isEmpty() && out.length() < 2) {
                out.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return out.length() == 0 ? "?" : out.toString();
    }

    /** The view must use the bg_avatar background. */
    static void bindAvatar(TextView view, String key, String name) {
        view.setText(initials(name));
        view.setBackgroundTintList(ColorStateList.valueOf(avatarColor(key)));
    }

    // --------------------------------------------------------------- categories

    static int categoryColor(String category) {
        switch (category == null ? "" : category) {
            case "Economics": return 0xFFB45309;
            case "Technology": return 0xFF4F46E5;
            case "Science": return 0xFF0D9488;
            case "Culture": return 0xFFDB2777;
            default: return 0xFF64748B;
        }
    }

    static String categoryEmoji(String category) {
        switch (category == null ? "" : category) {
            case "Economics": return "📈";
            case "Technology": return "💻";
            case "Science": return "🔬";
            case "Culture": return "🎭";
            default: return "📰";
        }
    }

    /** Category colour that stays readable as text in dark mode. */
    static int categoryTextColor(Context context, String category) {
        int color = categoryColor(category);
        boolean night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return night ? ColorUtils.blendARGB(color, Color.WHITE, 0.4f) : color;
    }

    /** The view must use the bg_tile background. */
    static void bindCategoryTile(TextView tile, String category) {
        tile.setText(categoryEmoji(category));
        tile.setBackgroundTintList(ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(categoryColor(category), 0x2E)));
    }

    // ------------------------------------------------------------------ text

    /** "★ 4.2 (5)" with a gold star, or a note when nobody has rated yet. */
    static CharSequence rating(Context context, Article article) {
        if (article.getRatingCount() == 0) {
            return context.getString(R.string.rating_none);
        }
        SpannableString text = new SpannableString(context.getString(R.string.rating_summary,
                article.getAverageRating(), article.getRatingCount()));
        text.setSpan(new ForegroundColorSpan(context.getColor(R.color.star_gold)), 0, 1, 0);
        return text;
    }

    /** Time of day for today, otherwise a short date. */
    static String shortTime(Context context, long millis) {
        int flags = DateUtils.isToday(millis)
                ? DateUtils.FORMAT_SHOW_TIME
                : DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
        return DateUtils.formatDateTime(context, millis, flags);
    }

    /** "Just now", "5 minutes ago", "Yesterday"... for a time in the past. */
    static CharSequence timeAgo(Context context, long millis) {
        long age = System.currentTimeMillis() - millis;
        if (age < DateUtils.MINUTE_IN_MILLIS) {
            return context.getString(R.string.video_just_now);
        }
        return DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS);
    }

    static String timeOfDay(Context context, long millis) {
        return DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME);
    }
}
