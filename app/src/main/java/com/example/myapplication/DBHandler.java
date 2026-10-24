package com.example.myapplication;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHandler  extends SQLiteOpenHelper {
    private static final String DB_NAME = "discuss";
    private static final int DB_VERSION = 1;

    public DBHandler(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public void onConfigure(SQLiteDatabase db){
        super.onConfigure(db);

        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE users( " +
                        "uid TEXT PRIMARY KEY NOT NULL, " +
                        "username TEXT NOT NULL COLLATE NOCASE UNIQUE, " +
                        "first_name TEXT NOT NULL DEFAULT '', " +
                        "last_name TEXT NOT NULL DEFAULT '', " +
                        "created_at INTEGER NOT NULL" +
                        ")"
        );

        db.execSQL("CREATE TABLE contacts(" +
                "owner_uid TEXT NOT NULL, " +
                "contact_uid TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "PRIMARY KEY (owner_uid,contact_uid), " +
                "CHECK (owner_uid <>contact_uid), " +
                "FOREIGN KEY (owner_uid) REFERENCES users(uid) " +
                "ON DELETE CASCADE, " +
                "FOREIGN KEY (contact_uid) REFERENCES users(uid)" +
                "ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE block (" +
                "blocker_uid TEXT NOT NULL, " +
                "blocked_uid TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "PRIMARY KEY (blocker_uid, blocked_uid), " +
                "CHECK (blocker_uid <> blocked_uid), " +
                "FOREIGN KEY (blocker_uid) REFERENCES users(uid) " +
                "ON DELETE CASCADE, " +
                "FOREIGN KEY (blocked_uid) REFERENCES users(uid) " +
                "ON DELETE CASCADE" +
                ")"
        );

        db.execSQL(
                "CREATE TABLE user_ratings (" +
                        "rater_uid TEXT NOT NULL, " +
                        "target_uid TEXT NOT NULL, " +
                        "value INTEGER NOT NULL " +
                        "CHECK (typeof(value)= 'integer' AND value BETWEEN 1 AND 5), " +
                        "updated_at INTEGER NOT NULL, " +
                        "PRIMARY KEY (rater_uid, target_uid), " +
                        "CHECK (rater_uid <> target_uid), " +
                        "FOREIGN KEY (rater_uid) REFERENCES users(uid) " +
                        "ON DELETE CASCADE, " +
                        "FOREIGN KEY (target_uid) REFERENCES users(uid) " +
                        "ON DELETE CASCADE" +
                        ")"
        );

    // Articles table, to Save/remove article from library
        db.execSQL(
                "CREATE TABLE articles (" +
                "article_id TEXT PRIMARY KEY NOT NULL, " +
                "title TEXT NOT NULL, " +
                "author TEXT NOT NULL DEFAULT '', " +
                "content_snippet TEXT NOT NULL DEFAULT '', " +
                "source_url TEXT NOT NULL DEFAULT '', " +
                "saved_by_uid TEXT NOT NULL, " +
                "saved_at INTEGER NOT NULL, " +
                "FOREIGN KEY (saved_by_uid) REFERENCES users(uid) ON DELETE CASCADE" +
       ")"
    );

    //  Article rating table
        db.execSQL(
                "CREATE TABLE article_ratings (" +
                "article_id TEXT NOT NULL, " +
                "user_uid TEXT NOT NULL, " +
                "rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5), " +
                "rated_at INTEGER NOT NULL, " +
                "PRIMARY KEY (article_id, user_uid), " +
                "FOREIGN KEY (article_id) REFERENCES articles(article_id) ON DELETE CASCADE, " +
                "FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE" +
                ")"
                );

    // 7.  Chat Group table (Support Create chat group
        db.execSQL(
                "CREATE TABLE chat_groups (" +
                "group_id TEXT PRIMARY KEY NOT NULL, " +
                "group_name TEXT NOT NULL, " +
                "creator_uid TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "FOREIGN KEY (creator_uid) REFERENCES users(uid) ON DELETE CASCADE" +
                ")"
                );

    // 8. Group Members table (support Add people to chat group)
        db.execSQL(
                "CREATE TABLE group_members (" +
                "group_id TEXT NOT NULL, " +
                "member_uid TEXT NOT NULL, " +
                "joined_at INTEGER NOT NULL, " +
                "PRIMARY KEY (group_id, member_uid), " +
                "FOREIGN KEY (group_id) REFERENCES chat_groups(group_id) ON DELETE CASCADE, " +
                "FOREIGN KEY (member_uid) REFERENCES users(uid) ON DELETE CASCADE" +
                ")"
                );
}
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
       throw new IllegalStateException(
               "Missing database migration from " + oldVersion + " to " + newVersion
       );
    }

    public void addContact(String ownerUid, String contactUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("owner_uid", ownerUid);
        values.put("contact_uid", contactUid);
        values.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict("contacts", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public void removeContact(String ownerUid, String contactUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("contacts", "owner_uid=? AND contact_uid=?", new String[]{ownerUid, contactUid});
        db.close();
    }

    public void blockUser(String blockerUid, String blockedUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("blocker_uid", blockerUid);
        values.put("blocked_uid", blockedUid);
        values.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict("block", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    //articleID is now auto increment

    public void saveArticle(String title, String author, String contentSnippet, String sourceUrl, String savedByUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        //values.put("article_id", articleId);
        values.put("title", title);
        values.put("author", author);
        values.put("content_snippet", contentSnippet);
        values.put("source_url", sourceUrl);
        values.put("saved_by_uid", savedByUid);
        values.put("saved_at", System.currentTimeMillis());
        db.insertWithOnConflict("articles", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public Cursor getSavedArticles(String uid) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("articles", null, "saved_by_uid=?", new String[]{uid}, null, null, "saved_at DESC");
    }

    public void rateUser(String raterUid, String targetUid, int value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("rater_uid", raterUid);
        values.put("target_uid", targetUid);
        values.put("value", value);
        values.put("updated_at", System.currentTimeMillis());
        db.insertWithOnConflict("user_ratings", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public double getAverageRating(String targetUid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT AVG(value) FROM user_ratings WHERE target_uid=?", new String[]{targetUid});
        double avg = 0;
        if (cursor.moveToFirst()) {
            avg = cursor.getDouble(0);
        }
        cursor.close();
        return avg;
    }

    public void createGroup(String groupId, String groupName, String creatorUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("group_name", groupName);
        values.put("creator_uid", creatorUid);
        values.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict("chat_groups", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public void addMemberToGroup(String groupId, String memberUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("member_uid", memberUid);
        values.put("joined_at", System.currentTimeMillis());
        db.insertWithOnConflict("group_members", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public Cursor getContacts(String ownerUid) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT u.uid, u.username, u.first_name, u.last_name " +
                "FROM contacts c " +
                "INNER JOIN users u ON c.contact_uid = u.uid " +
                "WHERE c.owner_uid = ? ORDER BY c.created_at DESC", 
                new String[]{ownerUid}
        );
    }

    public Cursor getUser(String uid) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("users", null, "uid=?", new String[]{uid}, null, null, null);
    }
}
