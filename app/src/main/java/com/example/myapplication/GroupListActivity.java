package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Lists the group chats the user belongs to. */
public class GroupListActivity extends AppCompatActivity {
    private String uid;
    private DBHandler db;
    private RecyclerView rvGroups;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uid = Session.requireUser(this);
        if (uid == null) {
            return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        db = DBHandler.getInstance(this);

        rvGroups = findViewById(R.id.rvGroups);
        tvEmpty = findViewById(R.id.tvGroupsEmpty);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnGroupsBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabCreateGroup).setOnClickListener(v ->
                startActivity(CreateGroupChat.intent(this, null)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) {
            List<ChatGroup> groups = db.getGroupsForUser(uid);
            rvGroups.setAdapter(new GroupAdapter(groups));
            tvEmpty.setVisibility(groups.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {
        private final List<ChatGroup> groups;

        GroupAdapter(List<ChatGroup> groups) {
            this.groups = groups;
        }

        @NonNull
        @Override
        public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new GroupViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_group, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
            ChatGroup group = groups.get(position);
            Ui.bindAvatar(holder.avatar,
                    group.isDirect() && group.getPeerUid() != null ? group.getPeerUid() : group.getId(),
                    group.getName());
            holder.name.setText(group.getName());
            holder.time.setText(Ui.shortTime(GroupListActivity.this, group.getLastActiveAt()));
            holder.lastMessage.setText(group.getLastMessage() == null
                    ? getString(R.string.groups_no_messages) : group.getLastMessage());
            holder.members.setText(group.isDirect()
                    ? getString(R.string.chat_direct)
                    : getResources().getQuantityString(R.plurals.groups_members,
                            group.getMemberCount(), group.getMemberCount()));
            holder.itemView.setOnClickListener(v -> startActivity(
                    GroupChatActivity.intent(GroupListActivity.this, group.getId())));
        }

        @Override
        public int getItemCount() {
            return groups.size();
        }

        class GroupViewHolder extends RecyclerView.ViewHolder {
            final TextView avatar, name, lastMessage, members, time;

            GroupViewHolder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.tvGroupAvatar);
                time = itemView.findViewById(R.id.tvGroupTime);
                name = itemView.findViewById(R.id.tvGroupName);
                lastMessage = itemView.findViewById(R.id.tvGroupLastMessage);
                members = itemView.findViewById(R.id.tvGroupMembers);
            }
        }
    }
}
