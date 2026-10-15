package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class ArticleBrowse extends AppCompatActivity {
    private static final String FILTER_ALL = "__all__";
    private static final String FILTER_SAVED = "__saved__";
    private static final String STATE_FILTER = "filter";
    private static final String STATE_BAR = "bar_expanded";

    private Boolean isCBexpanded = true;
    private String filter = FILTER_ALL;
    private String uid;
    private DBHandler db;
    private ArticleBrowseRecyclerAdapter adapter;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uid = Session.requireUser(this);
        if (uid == null) {
            return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_article_browse);
        db = DBHandler.getInstance(this);

        RecyclerView recycler = findViewById(R.id.article_browse_recycler);
        FrameLayout contactBar = findViewById(R.id.article_browse_contactBar);
        AppCompatImageButton contactButton = findViewById(R.id.article_browse_contact_button);
        emptyView = findViewById(R.id.article_browse_empty);

        if (savedInstanceState != null) {
            filter = savedInstanceState.getString(STATE_FILTER, FILTER_ALL);
            if (!savedInstanceState.getBoolean(STATE_BAR, true)) {
                isCBexpanded = changeContactBar(true, contactBar);
            }
        }

        contactButton.setOnClickListener(v -> isCBexpanded = changeContactBar(isCBexpanded, contactBar));
        findViewById(R.id.nav_contacts).setOnClickListener(v ->
                startActivity(new Intent(this, ContactActivity.class)));
        findViewById(R.id.nav_groups).setOnClickListener(v ->
                startActivity(new Intent(this, GroupListActivity.class)));
        findViewById(R.id.nav_profile).setOnClickListener(v ->
                startActivity(ProfileActivity.intent(this, uid)));

        adapter = new ArticleBrowseRecyclerAdapter(article ->
                startActivity(ArticleView.intent(this, article.getId())));
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));

        buildCategoryChips();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            // Ratings and saved articles may have changed on another screen
            loadArticles();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_FILTER, filter);
        outState.putBoolean(STATE_BAR, isCBexpanded);
    }

    private void buildCategoryChips() {
        ChipGroup group = findViewById(R.id.article_browse_categories);
        addChip(group, getString(R.string.category_all), FILTER_ALL);
        addChip(group, getString(R.string.category_saved), FILTER_SAVED);
        for (String category : db.getCategories()) {
            addChip(group, category, category);
        }
        group.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = g.findViewById(checkedIds.get(0));
            filter = (String) chip.getTag();
            loadArticles();
        });
    }

    private void addChip(ChipGroup group, String label, String tag) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setTag(tag);
        chip.setCheckable(true);
        chip.setId(View.generateViewId());
        group.addView(chip);
        if (tag.equals(filter)) {
            chip.setChecked(true);
        }
    }

    private void loadArticles() {
        List<Article> articles;
        if (FILTER_SAVED.equals(filter)) {
            articles = db.getSavedArticles(uid);
        } else {
            articles = db.getArticles(FILTER_ALL.equals(filter) ? null : filter);
        }
        adapter.setArticles(articles);
        boolean empty = articles.isEmpty();
        emptyView.setText(R.string.browse_empty_saved);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    public Boolean changeContactBar(Boolean isCBexpanded, FrameLayout contactBar) {
        ViewGroup.LayoutParams params = contactBar.getLayoutParams();
        params.width = (int) getResources().getDimension(isCBexpanded
                ? R.dimen.ArticleBrowseCollapsedCB : R.dimen.ArticleBrowseExpandedCB);
        contactBar.setLayoutParams(params);
        return !isCBexpanded;
    }
}
