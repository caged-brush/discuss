package com.example.myapplication;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class ProfileActivity extends SignedInActivity {
    static final String EXTRA_UID = "profile_uid";
    private String targetUid;
    private String username;
    private Button rateButton;
    private Button blockButton;
    private LinearLayout articlesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        targetUid = getIntent().getStringExtra(EXTRA_UID);
        if (targetUid == null) targetUid = currentUid;
        setContentView(R.layout.activity_profile);

        try (Cursor cursor = db.getUser(targetUid)) {
            if (!cursor.moveToFirst()) {
                showMessage(getString(R.string.profile_unavailable));
                finish();
                return;
            }
            ContactAdapter.Contact user = new ContactAdapter.Contact(cursor);
            TextView name = findViewById(R.id.tvUserName);
            name.setText(user.name);
            username = user.username;
            TextView handle = findViewById(R.id.profileUsername);
            handle.setText(getString(R.string.username_handle, username));
        }

        rateButton = findViewById(R.id.rateUser);
        blockButton = findViewById(R.id.blockUser);
        articlesView = findViewById(R.id.recentArticles);
        findViewById(R.id.btnBack).setOnClickListener(view -> finish());
        rateButton.setOnClickListener(view -> showRatingDialog());
        blockButton.setOnClickListener(view -> confirmBlockChange());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) refresh();
    }

    private void refresh() {
        boolean peer = !currentUid.equals(targetUid);
        boolean blockedByMe = peer && db.isBlockedBy(currentUid, targetUid);
        boolean blockActive = peer && db.hasBlockBetween(currentUid, targetUid);
        double rating = db.getAverageRating(targetUid);
        RatingBar ratingBar = findViewById(R.id.ratingBarUser);
        ratingBar.setRating((float) rating);
        TextView average = findViewById(R.id.averageRating);
        average.setText(rating == 0 ? getString(R.string.no_ratings)
                : getString(R.string.average_rating, rating));

        rateButton.setVisibility(peer && !blockActive ? View.VISIBLE : View.GONE);
        blockButton.setVisibility(peer ? View.VISIBLE : View.GONE);
        blockButton.setText(blockedByMe ? getString(R.string.unblock_user) : getString(R.string.block_user));
        articlesView.removeAllViews();
        if (blockActive) {
            addArticleStatus(getString(R.string.articles_blocked));
            return;
        }
        List<SavedArticle> articles = SavedArticle.load(db, targetUid);
        if (articles.isEmpty()) addArticleStatus(getString(R.string.no_saved_articles));
        for (SavedArticle article : articles) {
            Button row = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            row.setText(article.title);
            row.setOnClickListener(view -> article.open(this));
            articlesView.addView(row);
        }
    }

    private void addArticleStatus(String message) {
        TextView status = new TextView(this);
        status.setText(message);
        articlesView.addView(status);
    }

    private void showRatingDialog() {
        String[] values = new String[5];
        for (int i = 0; i < values.length; i++) {
            int stars = i + 1;
            values[i] = getResources().getQuantityString(R.plurals.rating_stars, stars, stars);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.rate_user_prompt, username))
                .setSingleChoiceItems(values, db.getUserRating(currentUid, targetUid) - 1, (dialog, item) -> {
                    try {
                        db.rateUser(currentUid, targetUid, item + 1);
                        refresh();
                    } catch (IllegalArgumentException exception) {
                        showMessage(exception.getMessage());
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
    }

    private void confirmBlockChange() {
        boolean blocked = db.isBlockedBy(currentUid, targetUid);
        String action = blocked ? getString(R.string.action_unblock) : getString(R.string.action_block);
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(blocked ? R.string.unblock_prompt : R.string.block_prompt, username))
                .setMessage(blocked ? getString(R.string.unblock_explanation)
                        : getString(R.string.block_explanation))
                .setPositiveButton(action, (dialog, item) -> {
                    if (blocked) db.unblockUser(currentUid, targetUid);
                    else db.blockUser(currentUid, targetUid);
                    refresh();
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
    }
}
