package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
    private static final int COLOR_UNSELECTED = Color.parseColor("#666666");
    private static final int COLOR_SELECTED = Color.parseColor("#FF0000");

    private final Set<String> selectedMembers = new LinkedHashSet<>();
    private final Map<String, AppCompatButton> articleButtons = new HashMap<>();
    private String selectedArticleId;
    private String uid;
    private DBHandler db;

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

        if (savedInstanceState != null) {
            ArrayList<String> members = savedInstanceState.getStringArrayList(STATE_MEMBERS);
            if (members != null) {
                selectedMembers.addAll(members);
            }
            selectedArticleId = savedInstanceState.getString(STATE_ARTICLE);
        } else {
            selectedArticleId = getIntent().getStringExtra(EXTRA_ARTICLE_ID);
        }

        buildContactButtons();
        buildArticleButtons();

        EditText title = findViewById(R.id.edtGroupTitle);
        if (savedInstanceState == null && selectedArticleId != null) {
            Article article = db.getArticle(selectedArticleId);
            if (article != null) {
                title.setText(article.getTitle());
            }
        }
        findViewById(R.id.btnCreateGroup).setOnClickListener(v -> createGroup(title));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_MEMBERS, new ArrayList<>(selectedMembers));
        outState.putString(STATE_ARTICLE, selectedArticleId);
    }

    private void buildContactButtons() {
        LinearLayout container = findViewById(R.id.contactsContainer);
        List<UserProfile> contacts = db.getContacts(uid);
        if (contacts.isEmpty()) {
            container.addView(emptyLabel(R.string.create_group_no_contacts));
            return;
        }
        for (UserProfile contact : contacts) {
            AppCompatButton button = toggleButton(contact.getDisplayName()
                    + "  (@" + contact.getUsername() + ")");
            setSelected(button, selectedMembers.contains(contact.getUid()));
            button.setOnClickListener(v -> {
                boolean nowSelected = !selectedMembers.remove(contact.getUid());
                if (nowSelected) {
                    selectedMembers.add(contact.getUid());
                }
                setSelected(button, nowSelected);
            });
            container.addView(button);
        }
    }

    private void buildArticleButtons() {
        LinearLayout container = findViewById(R.id.articlesContainer);
        List<Article> saved = db.getSavedArticles(uid);
        if (saved.isEmpty()) {
            container.addView(emptyLabel(R.string.create_group_no_saved));
            return;
        }
        for (Article article : saved) {
            AppCompatButton button = toggleButton(article.getTitle());
            articleButtons.put(article.getId(), button);
            setSelected(button, article.getId().equals(selectedArticleId));
            // Single choice: tapping the chosen article again clears it
            button.setOnClickListener(v -> {
                String previous = selectedArticleId;
                selectedArticleId = article.getId().equals(previous) ? null : article.getId();
                if (previous != null && articleButtons.containsKey(previous)) {
                    setSelected(articleButtons.get(previous), false);
                }
                setSelected(button, selectedArticleId != null);
            });
            container.addView(button);
        }
    }

    private AppCompatButton toggleButton(String label) {
        AppCompatButton button = new AppCompatButton(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        button.setLayoutParams(params);
        button.setText(label);
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setTextColor(Color.WHITE);
        return button;
    }

    private void setSelected(AppCompatButton button, boolean selected) {
        button.setBackgroundTintList(ColorStateList.valueOf(
                selected ? COLOR_SELECTED : COLOR_UNSELECTED));
        button.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    private TextView emptyLabel(int textRes) {
        TextView label = new TextView(this);
        label.setText(textRes);
        label.setGravity(Gravity.CENTER);
        label.setPadding(dp(12), dp(16), dp(12), dp(16));
        label.setTextColor(Color.DKGRAY);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
