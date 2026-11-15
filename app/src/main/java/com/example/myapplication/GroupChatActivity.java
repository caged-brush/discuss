package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class GroupChatActivity extends SignedInActivity {
    static final String EXTRA_GROUP_ID = "group_id";
    private String groupId;
    private RecyclerView messagesView;
    private MessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        String name = groupId == null ? null : db.getGroupName(groupId, currentUid);
        if (name == null) {
            showMessage("This group is unavailable.");
            finish();
            return;
        }
        setContentView(R.layout.activity_group_chat);
        TextView title = findViewById(R.id.tvChatTitle);
        title.setText(name);

        messagesView = findViewById(R.id.rvChatMessages);
        adapter = new MessageAdapter(currentUid, this::openMessage, this::openProfile);
        messagesView.setAdapter(adapter);
        EditText composer = findViewById(R.id.etMessage);
        findViewById(R.id.btnSendMessage).setOnClickListener(view -> {
            try {
                db.sendMessage(groupId, currentUid, composer.getText().toString());
                composer.setText("");
                loadMessages();
            } catch (IllegalArgumentException exception) {
                composer.setError(exception.getMessage());
            } catch (SQLiteException exception) {
                showMessage("Unable to send the message. Try again.");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) loadMessages();
    }

    private void loadMessages() {
        List<MessageAdapter.Message> messages = new ArrayList<>();
        try (Cursor cursor = db.getMessages(groupId, currentUid)) {
            while (cursor.moveToNext()) {
                messages.add(new MessageAdapter.Message(cursor.getString(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3), cursor.getLong(4)));
            }
        }
        adapter.replaceMessages(messages);
        findViewById(R.id.chatEmpty).setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        if (!messages.isEmpty()) messagesView.scrollToPosition(messages.size() - 1);
    }

    private void openMessage(MessageAdapter.Message message) {
        if (message.articleUrl != null) {
            startActivity(ArticleView.intent(this, message.body, message.articleUrl, null, null));
        } else {
            openProfile(message);
        }
    }

    private void openProfile(MessageAdapter.Message message) {
        startActivity(new Intent(this, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_UID, message.senderUid));
    }
}
