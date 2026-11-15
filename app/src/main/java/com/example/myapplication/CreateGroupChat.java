package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreateGroupChat extends SignedInActivity {
    private final Map<String, CheckBox> contacts = new LinkedHashMap<>();
    private final Map<String, CheckBox> articles = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        setContentView(R.layout.activity_create_group_chat);

        LinearLayout contactsView = findViewById(R.id.contactsContainer);
        try (Cursor cursor = db.getContacts(currentUid)) {
            while (cursor.moveToNext()) {
                ContactAdapter.Contact contact = new ContactAdapter.Contact(cursor);
                if (!db.hasBlockBetween(currentUid, contact.uid)) {
                    addChoice(contactsView, contacts, contact.uid, "@" + contact.username);
                }
            }
        }
        if (contacts.isEmpty()) addStatus(contactsView, "Add an available contact first.");

        LinearLayout articlesView = findViewById(R.id.articlesContainer);
        for (SavedArticle article : SavedArticle.load(db, currentUid)) {
            addChoice(articlesView, articles, article.id, article.title);
        }
        if (articles.isEmpty()) addStatus(articlesView, "Save an article to your library to share it.");

        if (savedInstanceState != null) {
            restoreChoices(contacts, savedInstanceState.getStringArrayList("contacts"));
            restoreChoices(articles, savedInstanceState.getStringArrayList("articles"));
        }

        findViewById(R.id.btnCreateGroup).setEnabled(!contacts.isEmpty());
        findViewById(R.id.btnCreateGroup).setOnClickListener(view -> createGroup());
    }

    private void addChoice(LinearLayout parent, Map<String, CheckBox> choices, String id, String label) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setMinHeight((int) (48 * getResources().getDisplayMetrics().density));
        choices.put(id, box);
        parent.addView(box);
    }

    private void addStatus(LinearLayout parent, String message) {
        TextView status = new TextView(this);
        status.setText(message);
        parent.addView(status);
    }

    private static ArrayList<String> selected(Map<String, CheckBox> choices) {
        ArrayList<String> selected = new ArrayList<>();
        choices.forEach((id, box) -> {
            if (box.isChecked()) selected.add(id);
        });
        return selected;
    }

    private static void restoreChoices(Map<String, CheckBox> choices, List<String> selected) {
        if (selected == null) return;
        choices.forEach((id, box) -> box.setChecked(selected.contains(id)));
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putStringArrayList("contacts", selected(contacts));
        state.putStringArrayList("articles", selected(articles));
        super.onSaveInstanceState(state);
    }

    private void createGroup() {
        EditText title = findViewById(R.id.edtGroupTitle);
        String name = title.getText().toString().trim();
        if (name.isEmpty()) {
            title.setError("Enter a group name.");
            return;
        }
        try {
            String groupId = db.createGroupWithMembers(name, currentUid, selected(contacts), selected(articles));
            startActivity(new Intent(this, GroupChatActivity.class).putExtra(GroupChatActivity.EXTRA_GROUP_ID, groupId));
            finish();
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLiteException exception) {
            showMessage("Unable to create the group. Try again.");
        }
    }
}
