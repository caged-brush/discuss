package com.example.myapplication;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.Objects;
import java.util.function.Consumer;

final class MessageAdapter extends ListAdapter<MessageAdapter.Message, MessageAdapter.Holder> {
    static final class Message {
        final long id;
        final String senderUid;
        final String username;
        final String body;
        final String articleUrl;
        final long createdAt;

        Message(long id, String senderUid, String username, String body, String articleUrl, long createdAt) {
            this.id = id;
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
    MessageAdapter(String currentUid, Consumer<Message> open, Consumer<Message> openProfile) {
        super(new DiffUtil.ItemCallback<Message>() {
            @Override
            public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
                return oldItem.id == newItem.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
                return oldItem.createdAt == newItem.createdAt
                        && oldItem.senderUid.equals(newItem.senderUid)
                        && oldItem.username.equals(newItem.username)
                        && oldItem.body.equals(newItem.body)
                        && Objects.equals(oldItem.articleUrl, newItem.articleUrl);
            }
        });
        this.currentUid = currentUid;
        this.open = open;
        this.openProfile = openProfile;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Message message = getItem(position);
        boolean own = currentUid.equals(message.senderUid);
        Context context = holder.row.getContext();
        holder.row.setGravity((own ? Gravity.END : Gravity.START) | Gravity.TOP);
        holder.avatar.setVisibility(own ? View.GONE : View.VISIBLE);
        holder.initial.setText(ContactAdapter.initial(message.username));
        holder.bubble.setCardBackgroundColor(MaterialColors.getColor(holder.bubble,
                own ? com.google.android.material.R.attr.colorPrimaryContainer
                        : com.google.android.material.R.attr.colorSurfaceContainer));
        holder.body.setTextColor(MaterialColors.getColor(holder.body,
                own ? com.google.android.material.R.attr.colorOnPrimaryContainer
                        : com.google.android.material.R.attr.colorOnSurface));
        String time = DateUtils.formatDateTime(context, message.createdAt,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH);
        String sender = own ? context.getString(R.string.message_self)
                : context.getString(R.string.username_handle, message.username);
        String body = message.articleUrl == null ? message.body
                : context.getString(R.string.message_shared_article, message.body);
        holder.body.setText(context.getString(R.string.message_display, sender, time, body));
        holder.avatar.setContentDescription(context.getString(R.string.message_sender_profile, sender));
        holder.avatar.setOnClickListener(view -> openProfile.accept(message));
        holder.bubble.setOnClickListener(view -> open.accept(message));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final LinearLayout row;
        final TextView body;
        final View avatar;
        final TextView initial;
        final MaterialCardView bubble;

        Holder(View view) {
            super(view);
            row = (LinearLayout) view;
            body = view.findViewById(R.id.tvMessage);
            avatar = view.findViewById(R.id.messageAvatar);
            initial = view.findViewById(R.id.messageInitial);
            bubble = view.findViewById(R.id.messageBubble);
        }
    }
}
