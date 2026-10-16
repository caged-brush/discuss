package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Shows any user's profile. On your own profile the menu offers log out instead. */
public class ProfileActivity extends AppCompatActivity {
    private static final String EXTRA_UID = "uid";
    private static final int MENU_CONTACT = 1;
    private static final int MENU_BLOCK = 2;
    private static final int MENU_LOGOUT = 3;
    private static final int RECENT_ARTICLE_LIMIT = 5;

    private RatingBar ratingBarUser;
    private RatingBar ratingBarRateUser;
    private TextView tvRatingSummary;
    private TextView tvBlockedBanner;
    private DBHandler dbHandler;
    private String viewerUid;
    private String targetUid;
    private UserProfile target;

    public static Intent intent(Context context, String uid) {
        return new Intent(context, ProfileActivity.class).putExtra(EXTRA_UID, uid);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewerUid = Session.requireUser(this);
        if (viewerUid == null) {
            return;
        }
        dbHandler = DBHandler.getInstance(this);
        targetUid = getIntent().getStringExtra(EXTRA_UID);
        if (targetUid == null) {
            targetUid = viewerUid;
        }
        target = dbHandler.getUser(targetUid);
        if (target == null) {
            finish();
            return;
        }

        // The header is a dark gradient, so the status bar icons stay light
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT));
        setContentView(R.layout.fragment_profile);
        View hero = findViewById(R.id.profileHero);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // The gradient extends behind the status bar; the rest respects the other insets
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            hero.setPadding(0, bars.top, 0, hero.getPaddingBottom());
            return insets;
        });

        ratingBarUser = findViewById(R.id.ratingBarUser);
        ratingBarRateUser = findViewById(R.id.ratingBarRateUser);
        tvRatingSummary = findViewById(R.id.tvRatingSummary);
        tvBlockedBanner = findViewById(R.id.tvBlockedBanner);
        ImageButton btnTopAction = findViewById(R.id.btnTopAction);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnTopAction.setOnClickListener(this::showActionsMenu);

        ((TextView) findViewById(R.id.tvProfileAvatar)).setText(Ui.initials(target.getDisplayName()));
        ((TextView) findViewById(R.id.tvUserName)).setText(target.getDisplayName());
        ((TextView) findViewById(R.id.tvUserHandle))
                .setText(getString(R.string.profile_username, target.getUsername()));

        boolean isSelf = targetUid.equals(viewerUid);
        View rateSection = findViewById(R.id.layoutRateUser);
        rateSection.setVisibility(isSelf ? View.GONE : View.VISIBLE);
        if (!isSelf) {
            ((TextView) findViewById(R.id.tvRateUserPrompt))
                    .setText(getString(R.string.profile_rate_prompt, target.getDisplayName()));
            ratingBarRateUser.setRating(dbHandler.getUserRating(viewerUid, targetUid));
            ratingBarRateUser.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
                if (!fromUser) {
                    return;
                }
                int stars = Math.max(1, Math.round(rating));
                if (stars != Math.round(rating)) {
                    bar.setRating(stars);
                }
                dbHandler.rateUser(viewerUid, targetUid, stars);
                refreshRating();
            });
        }

        loadRecentArticles();
        refreshRating();
        refreshBlocked();
    }

    private void refreshRating() {
        int count = dbHandler.getRatingCount(targetUid);
        double avgRating = dbHandler.getAverageRating(targetUid);
        ratingBarUser.setRating((float) avgRating);
        tvRatingSummary.setText(count == 0
                ? getString(R.string.rating_none)
                : getResources().getQuantityString(R.plurals.profile_rating_summary,
                        count, avgRating, count));
    }

    private void refreshBlocked() {
        boolean blocked = !targetUid.equals(viewerUid) && dbHandler.isBlocked(viewerUid, targetUid);
        tvBlockedBanner.setVisibility(blocked ? View.VISIBLE : View.GONE);
    }

    private void loadRecentArticles() {
        RecyclerView rv = findViewById(R.id.rvRecentArticles);
        ArticleBrowseRecyclerAdapter adapter = new ArticleBrowseRecyclerAdapter(article ->
                startActivity(ArticleView.intent(this, article.getId())));
        rv.setAdapter(adapter);
        List<Article> recent = dbHandler.getRecentlyRatedArticles(targetUid, RECENT_ARTICLE_LIMIT);
        adapter.setArticles(recent);
        findViewById(R.id.tvNoRecentArticles)
                .setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showActionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        Menu menu = popup.getMenu();
        if (targetUid.equals(viewerUid)) {
            menu.add(Menu.NONE, MENU_LOGOUT, Menu.NONE, R.string.profile_logout);
        } else {
            boolean blocked = dbHandler.isBlocked(viewerUid, targetUid);
            if (!blocked) {
                menu.add(Menu.NONE, MENU_CONTACT, Menu.NONE,
                        dbHandler.isContact(viewerUid, targetUid)
                                ? R.string.profile_remove_contact : R.string.profile_add_contact);
            }
            menu.add(Menu.NONE, MENU_BLOCK, Menu.NONE,
                    blocked ? R.string.profile_unblock : R.string.profile_block);
        }
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case MENU_CONTACT:
                    if (dbHandler.isContact(viewerUid, targetUid)) {
                        dbHandler.removeContact(viewerUid, targetUid);
                    } else {
                        dbHandler.addContact(viewerUid, targetUid);
                        Toast.makeText(this, getString(R.string.contacts_added,
                                target.getDisplayName()), Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case MENU_BLOCK:
                    if (dbHandler.isBlocked(viewerUid, targetUid)) {
                        dbHandler.unblockUser(viewerUid, targetUid);
                    } else {
                        dbHandler.blockUser(viewerUid, targetUid);
                    }
                    refreshBlocked();
                    return true;
                case MENU_LOGOUT:
                    Session.logOut(this);
                    Intent intent = new Intent(this, WelcomePage.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }
}
