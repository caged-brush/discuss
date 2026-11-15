package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class DBHandler extends SQLiteOpenHelper {
    private static final String DB_NAME = "discuss";
    private static final int DB_VERSION = 2;
    private static final String BLOCK_EXISTS =
            "EXISTS (SELECT 1 FROM block WHERE (blocker_uid=? AND blocked_uid=%1$s) "
                    + "OR (blocked_uid=? AND blocker_uid=%1$s))";

    public DBHandler(Context context) {
        this(context, DB_NAME);
    }

    DBHandler(Context context, String name) {
        super(context, name, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
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

        db.execSQL(
                "CREATE TABLE chat_groups (" +
                "group_id TEXT PRIMARY KEY NOT NULL, " +
                "group_name TEXT NOT NULL, " +
                "creator_uid TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "FOREIGN KEY (creator_uid) REFERENCES users(uid) ON DELETE CASCADE" +
                ")"
        );

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
        createMessagesTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            createMessagesTable(db);
        }
    }

    private void createMessagesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE messages ("
                + "message_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "group_id TEXT NOT NULL REFERENCES chat_groups(group_id) ON DELETE CASCADE, "
                + "sender_uid TEXT NOT NULL REFERENCES users(uid) ON DELETE CASCADE, "
                + "body TEXT NOT NULL CHECK (length(trim(body)) > 0), "
                + "article_url TEXT, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX messages_by_group ON messages(group_id, message_id)");
    }

    public void ensureUser(String uid, String displayName) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            try (Cursor cursor = getUser(uid)) {
                if (cursor.moveToFirst()) {
                    db.setTransactionSuccessful();
                    return;
                }
            }
            String username = displayName == null ? "" : displayName.trim();
            if (username.isEmpty()) {
                username = uid;
            }
            ContentValues profile = new ContentValues();
            profile.put("uid", uid);
            profile.put("created_at", System.currentTimeMillis());
            profile.put("username", username);
            int suffix = 1;
            while (db.insertWithOnConflict("users", null, profile, SQLiteDatabase.CONFLICT_IGNORE) == -1) {
                profile.put("username", username + "#" + suffix++);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void addContact(String ownerUid, String contactUid) {
        ContentValues values = new ContentValues();
        values.put("owner_uid", ownerUid);
        values.put("contact_uid", contactUid);
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("contacts", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void removeContact(String ownerUid, String contactUid) {
        getWritableDatabase().delete("contacts", "owner_uid=? AND contact_uid=?", new String[]{ownerUid, contactUid});
    }

    public void blockUser(String blockerUid, String blockedUid) {
        ContentValues values = new ContentValues();
        values.put("blocker_uid", blockerUid);
        values.put("blocked_uid", blockedUid);
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("block", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void unblockUser(String blockerUid, String blockedUid) {
        getWritableDatabase().delete("block", "blocker_uid=? AND blocked_uid=?",
                new String[]{blockerUid, blockedUid});
    }

    public boolean isBlockedBy(String blockerUid, String blockedUid) {
        try (Cursor cursor = getReadableDatabase().query("block", new String[]{"1"},
                "blocker_uid=? AND blocked_uid=?", new String[]{blockerUid, blockedUid},
                null, null, null)) {
            return cursor.moveToFirst();
        }
    }

    public boolean hasBlockBetween(String firstUid, String secondUid) {
        return isBlockedBy(firstUid, secondUid) || isBlockedBy(secondUid, firstUid);
    }

    public Cursor getBlockedUsers(String uid) {
        return getReadableDatabase().rawQuery("SELECT u.* FROM block b JOIN users u ON u.uid=b.blocked_uid "
                + "WHERE b.blocker_uid=? ORDER BY u.username", new String[]{uid});
    }

    public void saveArticle(String title, String author, String contentSnippet, String sourceUrl, String savedByUid) {
        String articleId = UUID.nameUUIDFromBytes((savedByUid + "\n" + sourceUrl)
                .getBytes(StandardCharsets.UTF_8)).toString();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("author", author == null ? "" : author);
        values.put("content_snippet", contentSnippet == null ? "" : contentSnippet);
        values.put("source_url", sourceUrl);
        values.put("saved_by_uid", savedByUid);
        values.put("saved_at", System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (db.update("articles", values, "saved_by_uid=? AND source_url=?",
                    new String[]{savedByUid, sourceUrl}) == 0) {
                values.put("article_id", articleId);
                db.insertOrThrow("articles", null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Cursor getSavedArticles(String uid) {
        return getReadableDatabase().query("articles", null, "saved_by_uid=?", new String[]{uid},
                null, null, "saved_at DESC, article_id");
    }

    public void rateUser(String raterUid, String targetUid, int value) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (hasBlockBetween(raterUid, targetUid)) {
                throw new IllegalArgumentException("Rating is unavailable for this user.");
            }
            ContentValues values = new ContentValues();
            values.put("rater_uid", raterUid);
            values.put("target_uid", targetUid);
            values.put("value", value);
            values.put("updated_at", System.currentTimeMillis());
            db.replaceOrThrow("user_ratings", null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public int getUserRating(String raterUid, String targetUid) {
        try (Cursor cursor = getReadableDatabase().query("user_ratings", new String[]{"value"},
                "rater_uid=? AND target_uid=?", new String[]{raterUid, targetUid}, null, null, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public double getAverageRating(String targetUid) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT AVG(value) FROM user_ratings WHERE target_uid=?", new String[]{targetUid})) {
            return cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getDouble(0) : 0;
        }
    }

    public void createGroup(String groupId, String groupName, String creatorUid) {
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("group_name", groupName);
        values.put("creator_uid", creatorUid);
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("chat_groups", null, values);
    }

    public void addMemberToGroup(String groupId, String memberUid) {
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("member_uid", memberUid);
        values.put("joined_at", System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("group_members", null, values);
    }

    public String createGroupWithMembers(String groupName, String creatorUid,
                                         List<String> contacts, List<String> articleIds) {
        if (groupName.trim().isEmpty() || contacts.isEmpty()) {
            throw new IllegalArgumentException("Enter a group name and select a contact.");
        }
        SQLiteDatabase db = getWritableDatabase();
        String groupId = UUID.randomUUID().toString();
        db.beginTransaction();
        try {
            createGroup(groupId, groupName.trim(), creatorUid);
            addMemberToGroup(groupId, creatorUid);
            for (String contactUid : contacts) {
                if (!hasContact(creatorUid, contactUid) || hasBlockBetween(creatorUid, contactUid)) {
                    throw new IllegalArgumentException("A selected contact is no longer available.");
                }
                addMemberToGroup(groupId, contactUid);
            }
            for (String articleId : articleIds) {
                try (Cursor cursor = db.query("articles", new String[]{"title", "source_url"},
                        "article_id=? AND saved_by_uid=?", new String[]{articleId, creatorUid},
                        null, null, null)) {
                    if (!cursor.moveToFirst() || !ArticleView.isArticleUrl(cursor.getString(1))) {
                        throw new IllegalArgumentException("A selected article is no longer available.");
                    }
                    insertMessage(db, groupId, creatorUid, cursor.getString(0), cursor.getString(1));
                }
            }
            db.setTransactionSuccessful();
            return groupId;
        } finally {
            db.endTransaction();
        }
    }

    public Cursor getGroups(String uid) {
        return getReadableDatabase().rawQuery("SELECT g.group_id, g.group_name FROM chat_groups g "
                        + "LEFT JOIN group_members m ON m.group_id=g.group_id AND m.member_uid=? "
                        + "WHERE m.member_uid IS NOT NULL OR g.creator_uid=? "
                        + "ORDER BY g.created_at DESC, g.group_id",
                new String[]{uid, uid});
    }

    public String getGroupName(String groupId, String uid) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT g.group_name FROM chat_groups g LEFT JOIN group_members m "
                        + "ON m.group_id=g.group_id AND m.member_uid=? "
                        + "WHERE g.group_id=? AND (m.member_uid IS NOT NULL OR g.creator_uid=?)",
                new String[]{uid, groupId, uid})) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    public void sendMessage(String groupId, String senderUid, String body) {
        if (body.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a message.");
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (getGroupName(groupId, senderUid) == null) {
                throw new IllegalArgumentException("This group is unavailable.");
            }
            insertMessage(db, groupId, senderUid, body.trim(), null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertMessage(SQLiteDatabase db, String groupId, String uid, String body, String articleUrl) {
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("sender_uid", uid);
        values.put("body", body);
        values.put("article_url", articleUrl);
        values.put("created_at", System.currentTimeMillis());
        db.insertOrThrow("messages", null, values);
    }

    public Cursor getMessages(String groupId, String uid) {
        return getReadableDatabase().rawQuery(
                "SELECT m.sender_uid, u.username, m.body, m.article_url, m.created_at "
                        + "FROM messages m JOIN users u ON u.uid=m.sender_uid "
                        + "JOIN chat_groups g ON g.group_id=m.group_id "
                        + "LEFT JOIN group_members member ON member.group_id=g.group_id AND member.member_uid=? "
                        + "WHERE m.group_id=? AND (member.member_uid IS NOT NULL OR g.creator_uid=?) "
                        + "AND NOT " + String.format(BLOCK_EXISTS, "m.sender_uid")
                        + " ORDER BY m.message_id",
                new String[]{uid, groupId, uid, uid, uid});
    }

    public boolean hasContact(String ownerUid, String contactUid) {
        try (Cursor cursor = getReadableDatabase().query("contacts", new String[]{"1"},
                "owner_uid=? AND contact_uid=?", new String[]{ownerUid, contactUid}, null, null, null)) {
            return cursor.moveToFirst();
        }
    }

    public Cursor getContacts(String ownerUid) {
        return getReadableDatabase().rawQuery("SELECT u.* FROM contacts c "
                + "JOIN users u ON u.uid=c.contact_uid WHERE c.owner_uid=? ORDER BY u.username",
                new String[]{ownerUid});
    }

    public Cursor getUser(String uid) {
        return getReadableDatabase().query("users", null, "uid=?", new String[]{uid}, null, null, null);
    }

    public String findUserUid(String query) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT uid FROM users WHERE uid=? OR username=? ORDER BY (uid=?) DESC LIMIT 1",
                new String[]{query, query, query})) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }
}
