package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class ContactActivity extends SignedInActivity {
    private RecyclerView contactsView;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        setContentView(R.layout.activity_contact);
        contactsView = findViewById(R.id.rvContacts);
        emptyView = findViewById(R.id.contactsEmpty);
        findViewById(R.id.btnTopAddContact).setOnClickListener(view -> showAddContactDialog());
        findViewById(R.id.blockedUsers).setOnClickListener(view -> showBlockedUsers());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) loadContacts();
    }

    private void loadContacts() {
        List<ContactAdapter.Contact> contacts = new ArrayList<>();
        try (Cursor cursor = db.getContacts(currentUid)) {
            while (cursor.moveToNext()) {
                contacts.add(new ContactAdapter.Contact(cursor));
            }
        }
        contactsView.setAdapter(new ContactAdapter(contacts,
                contact -> openProfile(contact.uid), this::confirmRemoval));
        emptyView.setVisibility(contacts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openProfile(String uid) {
        startActivity(new Intent(this, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_UID, uid));
    }

    private void confirmRemoval(ContactAdapter.Contact contact) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.remove_contact_prompt))
                .setMessage(contact.name)
                .setPositiveButton(getString(R.string.action_remove), (dialog, item) -> {
                    db.removeContact(currentUid, contact.uid);
                    loadContacts();
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
    }

    private void showAddContactDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(getString(R.string.contact_search_hint));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.add_contact_description))
                .setView(input)
                .setPositiveButton(getString(R.string.action_add), null)
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String query = input.getText().toString().trim();
            if (query.startsWith("@")) query = query.substring(1);
            String uid = db.findUserUid(query);
            if (uid == null) {
                input.setError(getString(R.string.unknown_local_user));
            } else if (uid.equals(currentUid)) {
                input.setError(getString(R.string.cannot_add_self));
            } else if (db.hasBlockBetween(currentUid, uid)) {
                input.setError(getString(R.string.contact_blocked));
            } else if (db.hasContact(currentUid, uid)) {
                input.setError(getString(R.string.duplicate_contact));
            } else {
                db.addContact(currentUid, uid);
                loadContacts();
                dialog.dismiss();
            }
        });
    }

    private void showBlockedUsers() {
        List<ContactAdapter.Contact> blocked = new ArrayList<>();
        try (Cursor cursor = db.getBlockedUsers(currentUid)) {
            while (cursor.moveToNext()) {
                blocked.add(new ContactAdapter.Contact(cursor));
            }
        }
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.blocked_users))
                .setPositiveButton(getString(R.string.action_done), null);
        if (blocked.isEmpty()) {
            dialog.setMessage(getString(R.string.empty_block_list));
        } else {
            String[] usernames = blocked.stream().map(contact -> getString(R.string.username_handle, contact.username)).toArray(String[]::new);
            dialog.setItems(usernames, (window, position) -> openProfile(blocked.get(position).uid));
        }
        dialog.show();
    }
}
