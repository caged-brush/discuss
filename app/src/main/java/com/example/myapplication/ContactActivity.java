package com.example.myapplication;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import android.widget.ImageButton;

public class ContactActivity extends AppCompatActivity {

    private RecyclerView rvContacts;
    private FloatingActionButton fabAddContact;
    private ImageButton btnTopAddContact;
    private ContactAdapter adapter;
    private List<ContactItem> contactList;
    private DBHandler dbHandler;
    
    // In a real app, this would be passed via Intent or fetched from a session manager
    private String currentUserUid = "user1"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        dbHandler = new DBHandler(this);
        rvContacts = findViewById(R.id.rvContacts);
        fabAddContact = findViewById(R.id.fabAddContact);
        btnTopAddContact = findViewById(R.id.btnTopAddContact);
        
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        
        loadContacts();

        fabAddContact.setOnClickListener(v -> showAddContactDialog());
        btnTopAddContact.setOnClickListener(v -> showAddContactDialog());
    }

    private void loadContacts() {
        contactList = new ArrayList<>();
        Cursor cursor = dbHandler.getContacts(currentUserUid);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String uid = cursor.getString(cursor.getColumnIndexOrThrow("uid"));
                String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow("first_name"));
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow("last_name"));
                String info = "@" + username;
                String name = firstName + " " + lastName;
                if (name.trim().isEmpty()) {
                    name = username;
                }
                contactList.add(new ContactItem(uid, name, info));
            }
            cursor.close();
        }

        adapter = new ContactAdapter(contactList, dbHandler, currentUserUid);
        rvContacts.setAdapter(adapter);
    }

    private void showAddContactDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter User UID");
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Contact")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String contactUid = input.getText().toString().trim();
                    if (!contactUid.isEmpty() && !contactUid.equals(currentUserUid)) {
                        dbHandler.addContact(currentUserUid, contactUid);
                        loadContacts(); // Refresh list
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}