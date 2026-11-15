package com.example.myapplication;

import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

final class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.Holder> {
    static final class Message {
        final String senderUid;
        final String username;
        final String body;
        final String articleUrl;
        final long createdAt;

        Message(String senderUid, String username, String body, String articleUrl, long createdAt) {
            this.senderUid = senderUid;
            this.username = username;
            this.body = body;
            this.articleUrl = articleUrl;
            this.createdAt = createdAt;
        }
    }

    private final String currentUid;
    private final Consumer<Message> open;
    private final Consumer<Message> openProfile;
    private List<Message> messages = Collections.emptyList();

    MessageAdapter(String currentUid, Consumer<Message> open, Consumer<Message> openProfile) {
        this.currentUid = currentUid;
        this.open = open;
        this.openProfile = openProfile;
    }

    void replaceMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Message message = messages.get(position);
        boolean own = currentUid.equals(message.senderUid);
        holder.row.setGravity((own ? Gravity.END : Gravity.START) | Gravity.TOP);
        holder.avatar.setVisibility(own ? View.GONE : View.VISIBLE);
        String time = DateUtils.formatDateTime(holder.row.getContext(), message.createdAt,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH);
        String sender = own ? "You" : "@" + message.username;
        String body = message.articleUrl == null ? message.body : "Shared article\n" + message.body;
        holder.body.setText(sender + " · " + time + "\n" + body);
        holder.avatar.setContentDescription("Open profile for " + sender);
        holder.avatar.setOnClickListener(view -> openProfile.accept(message));
        holder.bubble.setOnClickListener(view -> open.accept(message));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final LinearLayout row;
        final TextView body;
        final View avatar;
        final View bubble;

        Holder(View view) {
            super(view);
            row = (LinearLayout) view;
            body = view.findViewById(R.id.tvMessage);
            avatar = view.findViewById(R.id.messageAvatar);
            bubble = view.findViewById(R.id.messageBubble);
        }
    }
}
