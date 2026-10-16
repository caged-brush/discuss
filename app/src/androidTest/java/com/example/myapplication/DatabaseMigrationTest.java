package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/** Opens a database that was created by the previous app version (schema 2) and upgrades it. */
@RunWith(AndroidJUnit4.class)
public class DatabaseMigrationTest {
    private static final String DB_NAME = "migration_test.db";

    private Context context;

    @Before
    public void createVersion2Database() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(DB_NAME);
        File file = context.getDatabasePath(DB_NAME);
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();

        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null);
        db.execSQL("CREATE TABLE users(uid TEXT PRIMARY KEY NOT NULL, username TEXT NOT NULL, " +
                "first_name TEXT NOT NULL DEFAULT '', last_name TEXT NOT NULL DEFAULT '', " +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE articles(article_id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL)");
        db.execSQL("CREATE TABLE chat_groups(group_id TEXT PRIMARY KEY NOT NULL, " +
                "group_name TEXT NOT NULL, creator_uid TEXT NOT NULL, article_id TEXT, " +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE group_members(group_id TEXT NOT NULL, member_uid TEXT NOT NULL, " +
                "joined_at INTEGER NOT NULL, PRIMARY KEY (group_id, member_uid))");
        db.execSQL("CREATE TABLE messages(message_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "group_id TEXT NOT NULL, sender_uid TEXT NOT NULL, body TEXT NOT NULL DEFAULT '', " +
                "article_id TEXT, sent_at INTEGER NOT NULL)");
        db.execSQL("INSERT INTO users VALUES('u1', 'alice', 'Alice', '', 1)");
        db.execSQL("INSERT INTO chat_groups VALUES('g1', 'Old group', 'u1', NULL, 1)");
        db.execSQL("INSERT INTO group_members VALUES('g1', 'u1', 1)");
        db.execSQL("INSERT INTO messages(group_id, sender_uid, body, sent_at) " +
                "VALUES('g1', 'u1', 'written before the upgrade', 1)");
        db.setVersion(2);
        db.close();
    }

    @After
    public void deleteDatabase() {
        context.deleteDatabase(DB_NAME);
    }

    @Test
    public void upgradeKeepsExistingDataAndAddsTheNewColumnsAndTables() {
        DBHandler handler = new DBHandler(context, DB_NAME);
        SQLiteDatabase db = handler.getWritableDatabase();

        assertEquals(3, db.getVersion());
        assertTrue(hasColumn(db, "chat_groups", "is_direct"));
        assertTrue(hasColumn(db, "messages", "media_type"));
        assertTrue(hasColumn(db, "messages", "media_path"));
        assertTrue(hasColumn(db, "messages", "media_duration"));
        assertTrue(hasColumn(db, "videos", "file_name"));

        // Old rows survive, and behave as ordinary group text messages
        ChatGroup group = handler.getGroup("g1", "u1");
        assertFalse(group.isDirect());
        assertEquals("Old group", group.getName());
        try (Cursor c = db.rawQuery("SELECT body, media_type, media_duration FROM messages", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("written before the upgrade", c.getString(0));
            assertTrue(c.isNull(1));
            assertEquals(0, c.getInt(2));
        }

        // The new features work on the upgraded database
        String video = handler.addVideo("u1", "After upgrade", "", "v.mp4", "", 1000);
        assertEquals("After upgrade", handler.getVideo(video).getTitle());
        handler.close();
    }

    private static boolean hasColumn(SQLiteDatabase db, String table, String column) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (c.moveToNext()) {
                if (column.equals(c.getString(c.getColumnIndexOrThrow("name")))) {
                    return true;
                }
            }
        }
        return false;
    }
}
