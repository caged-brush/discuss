package com.example.myapplication;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.username.setText("@" + contact.username);
        holder.itemView.setOnClickListener(view -> open.accept(contact));
        holder.remove.setOnClickListener(view -> remove.accept(contact));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static final class ContactViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView username;
        final ImageButton remove;

        ContactViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.tvContactName);
            username = view.findViewById(R.id.tvContactInfo);
            remove = view.findViewById(R.id.btnRemoveContact);
        }
    }
}
