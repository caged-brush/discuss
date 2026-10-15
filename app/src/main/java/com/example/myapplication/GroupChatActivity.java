package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class GroupChatActivity extends AppCompatActivity {
    private static final String EXTRA_GROUP_ID = "group_id";
    private static final int COLOR_OWN_BUBBLE = Color.parseColor("#A9C7E0");
    private static final int COLOR_OTHER_BUBBLE = Color.parseColor("#C9D3DC");

    private DBHandler db;
    private String uid;
    private String groupId;
    private ChatGroup group;
    private RecyclerView rvMessages;
    private MessageAdapter adapter;

    public static Intent intent(Context context, String groupId) {
        return new Intent(context, GroupChatActivity.class).putExtra(EXTRA_GROUP_ID, groupId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uid = Session.requireUser(this);
        if (uid == null) {
            return;
        }
        db = DBHandler.getInstance(this);
        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        group = groupId == null ? null : db.getGroup(groupId);
        if (group == null || !db.isGroupMember(groupId, uid)) {
            Toast.makeText(this, R.string.chat_not_member, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // Include the keyboard so the composer stays visible while typing
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        TextView title = findViewById(R.id.tvChatTitle);
        title.setText(group.getName());
        title.setOnClickListener(v -> showGroupOptions());

        rvMessages = findViewById(R.id.rvChatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        adapter = new MessageAdapter();
        rvMessages.setAdapter(adapter);

        EditText etMessage = findViewById(R.id.etMessage);
        findViewById(R.id.btnSendMessage).setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (text.isEmpty()) {
                return;
            }
            db.sendMessage(groupId, uid, text, null);
            etMessage.setText("");
            loadMessages();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            loadMessages();
        }
    }

    private void loadMessages() {
        adapter.setMessages(db.getMessages(groupId, uid));
        if (adapter.getItemCount() > 0) {
            rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void showGroupOptions() {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        labels.add(getString(R.string.chat_members_title)
                + " (" + db.getGroupMembers(groupId).size() + ")");
        actions.add(this::showMembers);
        labels.add(getString(R.string.chat_add_member));
        actions.add(this::showAddMember);
        if (group.getArticleId() != null) {
            labels.add(getString(R.string.chat_open_article));
            actions.add(() -> startActivity(ArticleView.intent(this, group.getArticleId())));
        }
        labels.add(getString(R.string.chat_leave));
        actions.add(() -> {
            db.leaveGroup(groupId, uid);
            finish();
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle(group.getName())
                .setItems(labels.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .show();
    }

    private void showMembers() {
        List<UserProfile> members = db.getGroupMembers(groupId);
        String[] names = new String[members.size()];
        for (int i = 0; i < members.size(); i++) {
            names[i] = members.get(i).getDisplayName() + "  @" + members.get(i).getUsername();
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.chat_members_title)
                .setItems(names, (d, which) ->
                        startActivity(ProfileActivity.intent(this, members.get(which).getUid())))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showAddMember() {
        List<UserProfile> candidates = new ArrayList<>();
        for (UserProfile contact : db.getContacts(uid)) {
            if (!db.isGroupMember(groupId, contact.getUid())) {
                candidates.add(contact);
            }
        }
        if (candidates.isEmpty()) {
            Toast.makeText(this, R.string.chat_no_contacts_to_add, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            names[i] = candidates.get(i).getDisplayName();
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.chat_add_member)
                .setItems(names, (d, which) -> {
                    db.addMemberToGroup(groupId, candidates.get(which).getUid());
                    Toast.makeText(this, getString(R.string.contacts_added, names[which]),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
        private final List<ChatMessage> messages = new ArrayList<>();

        void setMessages(List<ChatMessage> newMessages) {
            messages.clear();
            messages.addAll(newMessages);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MessageViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            boolean mine = message.getSender().getUid().equals(uid);

            // Own messages sit on the right without avatar or name
            holder.row.setGravity(mine ? Gravity.END : Gravity.START);
            holder.avatar.setVisibility(mine ? View.GONE : View.VISIBLE);
            holder.sender.setVisibility(mine ? View.GONE : View.VISIBLE);
            holder.sender.setText(message.getSender().getDisplayName());
            holder.bubble.setCardBackgroundColor(mine ? COLOR_OWN_BUBBLE : COLOR_OTHER_BUBBLE);
            holder.avatar.setOnClickListener(v -> startActivity(ProfileActivity.intent(
                    GroupChatActivity.this, message.getSender().getUid())));

            StringBuilder text = new StringBuilder(message.getBody());
            if (message.getArticleId() != null) {
                if (text.length() > 0) {
                    text.append("\n\n");
                }
                text.append(getString(R.string.chat_shared_article, message.getArticleTitle()));
                holder.bubble.setOnClickListener(v -> startActivity(
                        ArticleView.intent(GroupChatActivity.this, message.getArticleId())));
            } else {
                holder.bubble.setOnClickListener(null);
                holder.bubble.setClickable(false);
            }
            holder.body.setText(text);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            final LinearLayout row;
            final MaterialCardView avatar, bubble;
            final TextView sender, body;

            MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                row = itemView.findViewById(R.id.messageRow);
                avatar = itemView.findViewById(R.id.messageAvatar);
                bubble = itemView.findViewById(R.id.messageBubble);
                sender = itemView.findViewById(R.id.tvSender);
                body = itemView.findViewById(R.id.tvMessage);
            }
        }
    }
}
