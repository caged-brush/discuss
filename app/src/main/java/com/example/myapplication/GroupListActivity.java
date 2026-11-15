package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class GroupListActivity extends SignedInActivity {
    private ListView groupsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        setContentView(R.layout.activity_group_list);
        groupsView = findViewById(R.id.groupsList);
        groupsView.setEmptyView(findViewById(R.id.groupsEmpty));
        findViewById(R.id.newGroup).setOnClickListener(view ->
                startActivity(new Intent(this, CreateGroupChat.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFinishing()) return;
        List<String> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        try (Cursor cursor = db.getGroups(currentUid)) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getString(0));
                names.add(cursor.getString(1));
            }
        }
        groupsView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
        groupsView.setOnItemClickListener((parent, view, position, id) ->
                startActivity(new Intent(this, GroupChatActivity.class)
                        .putExtra(GroupChatActivity.EXTRA_GROUP_ID, ids.get(position))));
    }
}
