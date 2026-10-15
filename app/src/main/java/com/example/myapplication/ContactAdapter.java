package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    public interface Listener {
        void onContactClick(UserProfile contact);
        void onContactRemoved(UserProfile contact);
    }

    private final List<UserProfile> contactList;
    private final Listener listener;

    public ContactAdapter(List<UserProfile> contactList, Listener listener) {
        this.contactList = contactList;
        this.listener = listener;
    }

    // Create single View (load item_contact.xml)
    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    // Bind data to single View
    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        UserProfile currentItem = contactList.get(position);

        holder.tvName.setText(currentItem.getDisplayName());
        holder.tvInfo.setText(holder.itemView.getContext()
                .getString(R.string.profile_username, currentItem.getUsername()));
        holder.itemView.setOnClickListener(v -> listener.onContactClick(currentItem));

        // Confirm popup
        holder.btnRemove.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            new MaterialAlertDialogBuilder(v.getContext())
                    .setTitle(R.string.contacts_remove_title)
                    .setMessage(v.getContext().getString(R.string.contacts_remove_message,
                            currentItem.getDisplayName()))
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        contactList.remove(currentPos);
                        notifyItemRemoved(currentPos);
                        listener.onContactRemoved(currentItem);
                    })
                    .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvName;
        TextView tvInfo;
        ImageButton btnRemove;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivContactAvatar);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvInfo = itemView.findViewById(R.id.tvContactInfo);
            btnRemove = itemView.findViewById(R.id.btnRemoveContact);
        }
    }
}
