package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;

public class ContactActivity extends AppCompatActivity implements ContactAdapter.Listener {

    private RecyclerView rvContacts;
    private TextView tvEmpty;
    private List<UserProfile> contactList;
    private DBHandler dbHandler;
    private String currentUserUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserUid = Session.requireUser(this);
        if (currentUserUid == null) {
            return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contact);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHandler = DBHandler.getInstance(this);
        rvContacts = findViewById(R.id.rvContacts);
        tvEmpty = findViewById(R.id.tvContactsEmpty);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnContactsBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddContact).setOnClickListener(v -> showAddContactDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dbHandler != null) {
            // The profile screen can add, remove or block contacts
            loadContacts();
        }
    }

    private void loadContacts() {
        contactList = dbHandler.getContacts(currentUserUid);
        rvContacts.setAdapter(new ContactAdapter(contactList, this));
        updateEmptyState();
    }

    private void updateEmptyState() {
        tvEmpty.setVisibility(contactList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onContactClick(UserProfile contact) {
        startActivity(ProfileActivity.intent(this, contact.getUid()));
    }

    @Override
    public void onContactRemoved(UserProfile contact) {
        dbHandler.removeContact(currentUserUid, contact.getUid());
        updateEmptyState();
    }

    private void showAddContactDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_contact, null);
        TextInputEditText input = view.findViewById(R.id.etContactUsername);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.contacts_add_title)
                .setView(view)
                .setPositiveButton(R.string.contacts_add, (dialog, which) ->
                        addContactByUsername(String.valueOf(input.getText()).trim()))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void addContactByUsername(String username) {
        if (username.startsWith("@")) {
            username = username.substring(1);
        }
        if (username.isEmpty()) {
            return;
        }
        UserProfile user = dbHandler.findUserByUsername(username);
        String error = null;
        if (user == null) {
            error = getString(R.string.contacts_not_found, username);
        } else if (user.getUid().equals(currentUserUid)) {
            error = getString(R.string.contacts_self);
        } else if (dbHandler.isBlocked(currentUserUid, user.getUid())) {
            error = getString(R.string.contacts_blocked);
        } else if (!dbHandler.addContact(currentUserUid, user.getUid())) {
            error = getString(R.string.contacts_exists);
        }
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, getString(R.string.contacts_added, user.getDisplayName()),
                Toast.LENGTH_SHORT).show();
        loadContacts();
    }
}
