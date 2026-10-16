package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.text.TextUtils;

import java.util.List;

public class ArticleView extends AppCompatActivity {
    private static final String EXTRA_ARTICLE_ID = "article_id";

    private DBHandler db;
    private String uid;
    private String articleId;
    private TextView tvByline, tvYourRating;
    private RatingBar ratingBar;
    private MaterialButton btnSave;

    public static Intent intent(Context context, String articleId) {
        return new Intent(context, ArticleView.class).putExtra(EXTRA_ARTICLE_ID, articleId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uid = Session.requireUser(this);
        if (uid == null) {
            return;
        }
        db = DBHandler.getInstance(this);
        articleId = getIntent().getStringExtra(EXTRA_ARTICLE_ID);
        Article article = articleId == null ? null : db.getArticle(articleId);
        if (article == null) {
            Toast.makeText(this, R.string.article_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_article_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvCategory = findViewById(R.id.tvArticleCategory);
        tvCategory.setText(article.getCategory());
        tvCategory.setTextColor(Ui.categoryTextColor(this, article.getCategory()));
        Ui.bindCategoryTile(findViewById(R.id.tvArticleTile), article.getCategory());
        ((TextView) findViewById(R.id.tvArticleTitle)).setText(article.getTitle());
        ((TextView) findViewById(R.id.tvArticleBody)).setText(article.getBody());
        tvByline = findViewById(R.id.tvArticleByline);
        tvYourRating = findViewById(R.id.tvYourRating);
        ratingBar = findViewById(R.id.ratingBarArticle);
        btnSave = findViewById(R.id.btnSaveArticle);

        findViewById(R.id.btnArticleBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnShareArticle).setOnClickListener(v -> showShareDialog(article));
        btnSave.setOnClickListener(v -> toggleSaved());

        int myRating = db.getArticleRating(uid, articleId);
        ratingBar.setRating(myRating);
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (!fromUser) {
                return;
            }
            // A rating of 0 is not allowed, so treat a tap on the left edge as 1 star
            int stars = Math.max(1, Math.round(rating));
            if (stars != Math.round(rating)) {
                bar.setRating(stars);
            }
            db.rateArticle(uid, articleId, stars);
            refreshRating();
        });

        refreshRating();
        refreshSaved();
    }

    private void refreshRating() {
        Article article = db.getArticle(articleId);
        tvByline.setText(TextUtils.concat(article.getAuthor(), "  ·  ", Ui.rating(this, article)));
        int mine = db.getArticleRating(uid, articleId);
        tvYourRating.setVisibility(mine == 0 ? View.GONE : View.VISIBLE);
        tvYourRating.setText(mine == 0 ? "" : getString(R.string.article_your_rating, mine));
    }

    private void toggleSaved() {
        if (db.isArticleSaved(uid, articleId)) {
            db.unsaveArticle(uid, articleId);
        } else {
            db.saveArticle(uid, articleId);
            Toast.makeText(this, R.string.article_saved_toast, Toast.LENGTH_SHORT).show();
        }
        refreshSaved();
    }

    private void refreshSaved() {
        boolean saved = db.isArticleSaved(uid, articleId);
        btnSave.setText(saved ? R.string.article_unsave : R.string.article_save);
        btnSave.setIconResource(saved ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_border);
    }

    /** Lets the user post the article into one of their groups, or start a new group. */
    private void showShareDialog(Article article) {
        List<ChatGroup> groups = db.getGroupsForUser(uid);
        String[] labels = new String[groups.size() + 1];
        labels[0] = getString(R.string.article_share_new_group);
        for (int i = 0; i < groups.size(); i++) {
            labels[i + 1] = groups.get(i).getName();
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.article_share_title)
                .setItems(labels, (dialog, which) -> {
                    if (which == 0) {
                        // Sharing requires the article to be in the library of the creator
                        db.saveArticle(uid, articleId);
                        refreshSaved();
                        startActivity(CreateGroupChat.intent(this, articleId));
                        return;
                    }
                    ChatGroup group = groups.get(which - 1);
                    db.sendMessage(group.getId(), uid, "", article.getId());
                    Toast.makeText(this, getString(R.string.article_shared_toast, group.getName()),
                            Toast.LENGTH_SHORT).show();
                    startActivity(GroupChatActivity.intent(this, group.getId()));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
