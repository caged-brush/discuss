package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pick a title, contacts and optionally one saved article, then open the new group. */
public class CreateGroupChat extends AppCompatActivity {
    private static final String EXTRA_ARTICLE_ID = "article_id";
    private static final String STATE_MEMBERS = "members";
    private static final String STATE_ARTICLE = "article";

    private final Set<String> selectedMembers = new LinkedHashSet<>();
    private final Map<String, MaterialCardView> articleCards = new HashMap<>();
    private String selectedArticleId;
    private String uid;
    private DBHandler db;
    private TextView tvInviteCount;

    /** Pass an article id to pre-select it, or null. */
    public static Intent intent(Context context, String articleId) {
        return new Intent(context, CreateGroupChat.class).putExtra(EXTRA_ARTICLE_ID, articleId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uid = Session.requireUser(this);
        if (uid == null) {
            return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_group_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        db = DBHandler.getInstance(this);
        tvInviteCount = findViewById(R.id.tvInviteCount);

        if (savedInstanceState != null) {
            ArrayList<String> members = savedInstanceState.getStringArrayList(STATE_MEMBERS);
            if (members != null) {
                selectedMembers.addAll(members);
            }
            selectedArticleId = savedInstanceState.getString(STATE_ARTICLE);
        } else {
            selectedArticleId = getIntent().getStringExtra(EXTRA_ARTICLE_ID);
        }

        buildContactRows();
        buildArticleRows();
        updateInviteCount();

        EditText title = findViewById(R.id.edtGroupTitle);
        if (savedInstanceState == null && selectedArticleId != null) {
            Article article = db.getArticle(selectedArticleId);
            if (article != null) {
                title.setText(article.getTitle());
            }
        }
        findViewById(R.id.btnCreateGroupBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCreateGroup).setOnClickListener(v -> createGroup(title));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_MEMBERS, new ArrayList<>(selectedMembers));
        outState.putString(STATE_ARTICLE, selectedArticleId);
    }

    private void buildContactRows() {
        LinearLayout container = findViewById(R.id.contactsContainer);
        List<UserProfile> contacts = db.getContacts(uid);
        if (contacts.isEmpty()) {
            container.addView(emptyLabel(R.string.create_group_no_contacts));
            return;
        }
        for (UserProfile contact : contacts) {
            MaterialCardView card = inflateRow(container);
            Ui.bindAvatar(card.findViewById(R.id.pickAvatar), contact.getUid(),
                    contact.getDisplayName());
            ((TextView) card.findViewById(R.id.pickTitle)).setText(contact.getDisplayName());
            ((TextView) card.findViewById(R.id.pickSubtitle))
                    .setText(getString(R.string.profile_username, contact.getUsername()));
            card.setChecked(selectedMembers.contains(contact.getUid()));
            card.setOnClickListener(v -> {
                boolean nowSelected = !card.isChecked();
                card.setChecked(nowSelected);
                if (nowSelected) {
                    selectedMembers.add(contact.getUid());
                } else {
                    selectedMembers.remove(contact.getUid());
                }
                updateInviteCount();
            });
            container.addView(card);
        }
    }

    private void buildArticleRows() {
        LinearLayout container = findViewById(R.id.articlesContainer);
        List<Article> saved = db.getSavedArticles(uid);
        if (saved.isEmpty()) {
            container.addView(emptyLabel(R.string.create_group_no_saved));
            return;
        }
        for (Article article : saved) {
            MaterialCardView card = inflateRow(container);
            TextView tile = card.findViewById(R.id.pickAvatar);
            tile.setBackgroundResource(R.drawable.bg_tile);
            Ui.bindCategoryTile(tile, article.getCategory());
            ((TextView) card.findViewById(R.id.pickTitle)).setText(article.getTitle());
            ((TextView) card.findViewById(R.id.pickSubtitle)).setText(article.getCategory());
            articleCards.put(article.getId(), card);
            card.setChecked(article.getId().equals(selectedArticleId));
            // Single choice: tapping the chosen article again clears it
            card.setOnClickListener(v -> {
                String previous = selectedArticleId;
                selectedArticleId = article.getId().equals(previous) ? null : article.getId();
                if (previous != null && articleCards.containsKey(previous)) {
                    articleCards.get(previous).setChecked(false);
                }
                card.setChecked(selectedArticleId != null);
            });
            container.addView(card);
        }
    }

    private MaterialCardView inflateRow(LinearLayout parent) {
        return (MaterialCardView) LayoutInflater.from(this)
                .inflate(R.layout.item_pick_row, parent, false);
    }

    private void updateInviteCount() {
        int count = selectedMembers.size();
        tvInviteCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        tvInviteCount.setText(getString(R.string.create_group_selected, count));
    }

    private TextView emptyLabel(int textRes) {
        TextView label = new TextView(this);
        label.setText(textRes);
        label.setGravity(Gravity.CENTER);
        label.setPadding(Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16));
        label.setTextColor(getColor(R.color.md_outline));
        return label;
    }

    private void createGroup(EditText titleField) {
        String title = titleField.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.create_group_need_title, Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedMembers.isEmpty()) {
            Toast.makeText(this, R.string.create_group_need_member, Toast.LENGTH_SHORT).show();
            return;
        }
        String groupId = db.createGroup(title, uid, new ArrayList<>(selectedMembers),
                selectedArticleId);
        startActivity(GroupChatActivity.intent(this, groupId));
        finish();
    }
}
