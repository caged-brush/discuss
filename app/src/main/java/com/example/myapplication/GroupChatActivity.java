package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import androidx.core.graphics.ColorUtils;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class GroupChatActivity extends AppCompatActivity {
    private static final String EXTRA_GROUP_ID = "group_id";

    private DBHandler db;
    private String uid;
    private String groupId;
    private ChatGroup group;
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private TextView tvSubtitle;

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

        ((TextView) findViewById(R.id.tvChatTitle)).setText(group.getName());
        Ui.bindAvatar(findViewById(R.id.tvChatAvatar), group.getId(), group.getName());
        tvSubtitle = findViewById(R.id.tvChatSubtitle);
        findViewById(R.id.btnChatBack).setOnClickListener(v -> finish());
        findViewById(R.id.chatHeader).setOnClickListener(v -> showGroupOptions());
        findViewById(R.id.btnChatMore).setOnClickListener(v -> showGroupOptions());

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
        int members = db.getGroupMembers(groupId).size();
        tvSubtitle.setText(getResources().getQuantityString(R.plurals.groups_members,
                members, members));
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
            View bubble = holder.bubble;

            // Own messages sit on the right in the brand colour, without avatar or name
            int background = MaterialColors.getColor(bubble, mine
                    ? androidx.appcompat.R.attr.colorPrimary
                    : com.google.android.material.R.attr.colorSurfaceContainerHigh);
            int foreground = MaterialColors.getColor(bubble, mine
                    ? com.google.android.material.R.attr.colorOnPrimary
                    : com.google.android.material.R.attr.colorOnSurface);
            int accent = mine ? foreground : MaterialColors.getColor(bubble,
                    androidx.appcompat.R.attr.colorPrimary);
            holder.row.setGravity(mine ? Gravity.END : Gravity.START);
            holder.bubble.setCardBackgroundColor(background);

            float big = Ui.dp(GroupChatActivity.this, 18);
            float small = Ui.dp(GroupChatActivity.this, 5);
            ShapeAppearanceModel.Builder shape = holder.bubble.getShapeAppearanceModel()
                    .toBuilder().setAllCornerSizes(big);
            if (mine) {
                shape.setBottomRightCornerSize(small);
            } else {
                shape.setBottomLeftCornerSize(small);
            }
            holder.bubble.setShapeAppearanceModel(shape.build());

            holder.avatar.setVisibility(mine ? View.GONE : View.VISIBLE);
            holder.sender.setVisibility(mine ? View.GONE : View.VISIBLE);
            if (!mine) {
                Ui.bindAvatar(holder.avatar, message.getSender().getUid(),
                        message.getSender().getDisplayName());
                holder.avatar.setOnClickListener(v -> startActivity(ProfileActivity.intent(
                        GroupChatActivity.this, message.getSender().getUid())));
                holder.sender.setText(message.getSender().getDisplayName());
                holder.sender.setTextColor(accent);
            }

            boolean hasText = !message.getBody().isEmpty();
            holder.body.setVisibility(hasText ? View.VISIBLE : View.GONE);
            holder.body.setText(message.getBody());
            holder.body.setTextColor(foreground);

            boolean hasArticle = message.getArticleId() != null;
            holder.articleBlock.setVisibility(hasArticle ? View.VISIBLE : View.GONE);
            if (hasArticle) {
                holder.articleTitle.setText(message.getArticleTitle());
                holder.articleTitle.setTextColor(foreground);
                holder.articleCta.setTextColor(accent);
                holder.articleTile.setBackgroundTintList(ColorStateList.valueOf(
                        ColorUtils.setAlphaComponent(foreground, 0x26)));
                holder.articleBlock.setOnClickListener(v -> startActivity(
                        ArticleView.intent(GroupChatActivity.this, message.getArticleId())));
            } else {
                holder.articleBlock.setOnClickListener(null);
            }

            holder.time.setText(Ui.timeOfDay(GroupChatActivity.this, message.getSentAt()));
            holder.time.setTextColor(ColorUtils.setAlphaComponent(foreground, 0xB3));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            final LinearLayout row, articleBlock;
            final MaterialCardView bubble;
            final TextView avatar, sender, body, time, articleTile, articleTitle, articleCta;

            MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                row = itemView.findViewById(R.id.messageRow);
                avatar = itemView.findViewById(R.id.messageAvatar);
                bubble = itemView.findViewById(R.id.messageBubble);
                sender = itemView.findViewById(R.id.tvSender);
                body = itemView.findViewById(R.id.tvMessage);
                time = itemView.findViewById(R.id.tvMessageTime);
                articleBlock = itemView.findViewById(R.id.articleBlock);
                articleTile = itemView.findViewById(R.id.tvArticleTile);
                articleTitle = itemView.findViewById(R.id.tvSharedArticleTitle);
                articleCta = itemView.findViewById(R.id.tvSharedArticleCta);
            }
        }
    }
}
