package com.example.myapplication;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;
import java.util.List;
import java.util.function.Consumer;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {
    static final class Contact {
        final String uid;
        final String username;
        final String name;

        Contact(Cursor cursor) {
            uid = cursor.getString(cursor.getColumnIndexOrThrow("uid"));
            username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
            String fullName = (cursor.getString(cursor.getColumnIndexOrThrow("first_name")) + " "
                    + cursor.getString(cursor.getColumnIndexOrThrow("last_name"))).trim();
            name = fullName.isEmpty() ? username : fullName;
        }
    }

    private final List<Contact> contacts;
    private final Consumer<Contact> open;
    private final Consumer<Contact> remove;

    ContactAdapter(List<Contact> contacts, Consumer<Contact> open, Consumer<Contact> remove) {
        this.contacts = contacts;
        this.open = open;
        this.remove = remove;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ContactViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.name.setText(contact.name);
        holder.avatar.setText(initial(contact.name));
        holder.username.setText(holder.itemView.getContext().getString(R.string.username_handle, contact.username));
        holder.itemView.setOnClickListener(view -> open.accept(contact));
        holder.remove.setOnClickListener(view -> remove.accept(contact));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static String initial(String name) {
        if (name.isEmpty()) return "?";
        return name.substring(0, name.offsetByCodePoints(0, 1)).toUpperCase(Locale.getDefault());
    }

    static final class ContactViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView username;
        final TextView avatar;
        final ImageButton remove;

        ContactViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.tvContactName);
            username = view.findViewById(R.id.tvContactInfo);
            avatar = view.findViewById(R.id.ivContactAvatar);
            remove = view.findViewById(R.id.btnRemoveContact);
        }
    }
}
