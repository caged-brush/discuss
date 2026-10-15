package com.example.myapplication;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Single SQLite database for the whole app. Use {@link #getInstance(Context)} so every screen
 * shares one connection; methods never close the database because open cursors would break.
 */
public class DBHandler extends SQLiteOpenHelper {
    private static final String DB_NAME = "discuss";
    private static final int DB_VERSION = 2;

    private static DBHandler instance;
    private final Context appContext;

    public static synchronized DBHandler getInstance(Context context) {
        if (instance == null) {
            instance = new DBHandler(context.getApplicationContext(), DB_NAME);
        }
        return instance;
    }

    /** Pass a null name for an in-memory database (used by tests). */
    DBHandler(Context context, String name) {
        super(context, name, null, DB_VERSION);
        this.appContext = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
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

        // Login data kept apart from the public profile
        db.execSQL(
                "CREATE TABLE credentials (" +
                        "uid TEXT PRIMARY KEY NOT NULL, " +
                        "email TEXT NOT NULL UNIQUE, " +
                        "password_hash BLOB NOT NULL, " +
                        "password_salt BLOB NOT NULL, " +
                        "password_iterations INTEGER NOT NULL, " +
                        "FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE" +
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
                "FOREIGN KEY (contact_uid) REFERENCES users(uid) " +
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

        // Article catalog, shared by every user
        db.execSQL(
                "CREATE TABLE articles (" +
                        "article_id TEXT PRIMARY KEY NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "author TEXT NOT NULL DEFAULT '', " +
                        "category TEXT NOT NULL DEFAULT '', " +
                        "body TEXT NOT NULL DEFAULT '', " +
                        "published_at INTEGER NOT NULL" +
                        ")"
        );

        // A user's personal library
        db.execSQL(
                "CREATE TABLE saved_articles (" +
                        "uid TEXT NOT NULL, " +
                        "article_id TEXT NOT NULL, " +
                        "saved_at INTEGER NOT NULL, " +
                        "PRIMARY KEY (uid, article_id), " +
                        "FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE, " +
                        "FOREIGN KEY (article_id) REFERENCES articles(article_id) ON DELETE CASCADE" +
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

        // A group may be started around an article (article_id is optional)
        db.execSQL(
                "CREATE TABLE chat_groups (" +
                        "group_id TEXT PRIMARY KEY NOT NULL, " +
                        "group_name TEXT NOT NULL, " +
                        "creator_uid TEXT NOT NULL, " +
                        "article_id TEXT, " +
                        "created_at INTEGER NOT NULL, " +
                        "FOREIGN KEY (creator_uid) REFERENCES users(uid) ON DELETE CASCADE, " +
                        "FOREIGN KEY (article_id) REFERENCES articles(article_id) ON DELETE SET NULL" +
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

        // A message is text, a shared article, or both
        db.execSQL(
                "CREATE TABLE messages (" +
                        "message_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "group_id TEXT NOT NULL, " +
                        "sender_uid TEXT NOT NULL, " +
                        "body TEXT NOT NULL DEFAULT '', " +
                        "article_id TEXT, " +
                        "sent_at INTEGER NOT NULL, " +
                        "FOREIGN KEY (group_id) REFERENCES chat_groups(group_id) ON DELETE CASCADE, " +
                        "FOREIGN KEY (sender_uid) REFERENCES users(uid) ON DELETE CASCADE, " +
                        "FOREIGN KEY (article_id) REFERENCES articles(article_id) ON DELETE SET NULL" +
                        ")"
        );
        db.execSQL("CREATE INDEX messages_by_group ON messages(group_id, sent_at)");

        seedArticles(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 was never released, so its data is discarded instead of migrated.
        if (oldVersion < 2) {
            String[] tables = {"messages", "group_members", "chat_groups", "article_ratings",
                    "saved_articles", "articles", "user_ratings", "block", "contacts",
                    "credentials", "users"};
            db.setForeignKeyConstraintsEnabled(false);
            for (String table : tables) {
                db.execSQL("DROP TABLE IF EXISTS " + table);
            }
            db.setForeignKeyConstraintsEnabled(true);
            onCreate(db);
            return;
        }
        throw new IllegalStateException(
                "Missing database migration from " + oldVersion + " to " + newVersion
        );
    }

    private void seedArticles(SQLiteDatabase db) {
        try (InputStream in = appContext.getResources().openRawResource(R.raw.seed_articles)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            JSONArray articles = new JSONArray(out.toString(StandardCharsets.UTF_8.name()));
            long now = System.currentTimeMillis();
            for (int i = 0; i < articles.length(); i++) {
                JSONObject a = articles.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put("article_id", a.getString("id"));
                values.put("title", a.getString("title"));
                values.put("author", a.optString("author"));
                values.put("category", a.optString("category"));
                values.put("body", a.getString("body"));
                // Keep the file order as newest-first
                values.put("published_at", now - i * 3_600_000L);
                db.insertOrThrow("articles", null, values);
            }
        } catch (IOException | JSONException e) {
            throw new IllegalStateException("Could not load seed articles", e);
        }
    }

    // ---------------------------------------------------------------- users

    public UserProfile getUser(String uid) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT uid, username, first_name, last_name FROM users WHERE uid = ?",
                new String[]{uid})) {
            return c.moveToFirst() ? readUser(c) : null;
        }
    }

    public UserProfile findUserByUsername(String username) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT uid, username, first_name, last_name FROM users WHERE username = ?",
                new String[]{username.trim()})) {
            return c.moveToFirst() ? readUser(c) : null;
        }
    }

    private static UserProfile readUser(Cursor c) {
        return new UserProfile(c.getString(0), c.getString(1), c.getString(2), c.getString(3));
    }

    private List<UserProfile> readUsers(String sql, String[] args) {
        List<UserProfile> users = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                users.add(readUser(c));
            }
        }
        return users;
    }

    // ---------------------------------------------------------------- contacts

    /** Returns false if the contact already exists or the insert was rejected. */
    public boolean addContact(String ownerUid, String contactUid) {
        ContentValues values = new ContentValues();
        values.put("owner_uid", ownerUid);
        values.put("contact_uid", contactUid);
        values.put("created_at", System.currentTimeMillis());
        try {
            return getWritableDatabase().insertWithOnConflict(
                    "contacts", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1;
        } catch (SQLiteConstraintException e) {
            // CONFLICT_IGNORE does not cover foreign keys, e.g. an unknown contact uid
            return false;
        }
    }

    public void removeContact(String ownerUid, String contactUid) {
        getWritableDatabase().delete("contacts", "owner_uid=? AND contact_uid=?",
                new String[]{ownerUid, contactUid});
    }

    public boolean isContact(String ownerUid, String contactUid) {
        return exists("SELECT 1 FROM contacts WHERE owner_uid=? AND contact_uid=?",
                ownerUid, contactUid);
    }

    public List<UserProfile> getContacts(String ownerUid) {
        return readUsers(
                "SELECT u.uid, u.username, u.first_name, u.last_name " +
                        "FROM contacts c " +
                        "INNER JOIN users u ON c.contact_uid = u.uid " +
                        "WHERE c.owner_uid = ? ORDER BY c.created_at DESC",
                new String[]{ownerUid});
    }

    // ---------------------------------------------------------------- blocking

    /** Blocking also removes the user from your contacts. */
    public void blockUser(String blockerUid, String blockedUid) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("blocker_uid", blockerUid);
            values.put("blocked_uid", blockedUid);
            values.put("created_at", System.currentTimeMillis());
            db.insertWithOnConflict("block", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            removeContact(blockerUid, blockedUid);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void unblockUser(String blockerUid, String blockedUid) {
        getWritableDatabase().delete("block", "blocker_uid=? AND blocked_uid=?",
                new String[]{blockerUid, blockedUid});
    }

    public boolean isBlocked(String blockerUid, String blockedUid) {
        return exists("SELECT 1 FROM block WHERE blocker_uid=? AND blocked_uid=?",
                blockerUid, blockedUid);
    }

    // ---------------------------------------------------------------- user ratings

    public void rateUser(String raterUid, String targetUid, int value) {
        ContentValues values = new ContentValues();
        values.put("rater_uid", raterUid);
        values.put("target_uid", targetUid);
        values.put("value", value);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(
                "user_ratings", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Returns 0 if the rater has not rated the target yet. */
    public int getUserRating(String raterUid, String targetUid) {
        return queryInt("SELECT value FROM user_ratings WHERE rater_uid=? AND target_uid=?",
                raterUid, targetUid);
    }

    public double getAverageRating(String targetUid) {
        return queryDouble("SELECT AVG(value) FROM user_ratings WHERE target_uid=?", targetUid);
    }

    public int getRatingCount(String targetUid) {
        return queryInt("SELECT COUNT(*) FROM user_ratings WHERE target_uid=?", targetUid);
    }

    // ---------------------------------------------------------------- articles

    private static final String ARTICLE_COLUMNS =
            "a.article_id, a.title, a.author, a.category, a.body, " +
                    "IFNULL(r.avg_rating, 0), IFNULL(r.rating_count, 0) ";
    private static final String ARTICLE_FROM =
            "FROM articles a LEFT JOIN (" +
                    "SELECT article_id, AVG(rating) AS avg_rating, COUNT(*) AS rating_count " +
                    "FROM article_ratings GROUP BY article_id) r " +
                    "ON r.article_id = a.article_id ";

    private List<Article> readArticles(String sql, String[] args) {
        List<Article> articles = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                articles.add(new Article(c.getString(0), c.getString(1), c.getString(2),
                        c.getString(3), c.getString(4), c.getDouble(5), c.getInt(6)));
            }
        }
        return articles;
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT category FROM articles WHERE category <> '' ORDER BY category",
                null)) {
            while (c.moveToNext()) {
                categories.add(c.getString(0));
            }
        }
        return categories;
    }

    /** Pass a null category for every article. */
    public List<Article> getArticles(String category) {
        if (category == null) {
            return readArticles("SELECT " + ARTICLE_COLUMNS + ARTICLE_FROM +
                    "ORDER BY a.published_at DESC", null);
        }
        return readArticles("SELECT " + ARTICLE_COLUMNS + ARTICLE_FROM +
                "WHERE a.category = ? ORDER BY a.published_at DESC", new String[]{category});
    }

    public Article getArticle(String articleId) {
        List<Article> result = readArticles("SELECT " + ARTICLE_COLUMNS + ARTICLE_FROM +
                "WHERE a.article_id = ?", new String[]{articleId});
        return result.isEmpty() ? null : result.get(0);
    }

    public List<Article> getSavedArticles(String uid) {
        return readArticles("SELECT " + ARTICLE_COLUMNS + ARTICLE_FROM +
                "INNER JOIN saved_articles s ON s.article_id = a.article_id " +
                "WHERE s.uid = ? ORDER BY s.saved_at DESC", new String[]{uid});
    }

    /** Articles the user rated most recently, shown on their profile. */
    public List<Article> getRecentlyRatedArticles(String uid, int limit) {
        return readArticles("SELECT " + ARTICLE_COLUMNS + ARTICLE_FROM +
                "INNER JOIN article_ratings mine ON mine.article_id = a.article_id " +
                "WHERE mine.user_uid = ? ORDER BY mine.rated_at DESC LIMIT " + limit,
                new String[]{uid});
    }

    public void saveArticle(String uid, String articleId) {
        ContentValues values = new ContentValues();
        values.put("uid", uid);
        values.put("article_id", articleId);
        values.put("saved_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(
                "saved_articles", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void unsaveArticle(String uid, String articleId) {
        getWritableDatabase().delete("saved_articles", "uid=? AND article_id=?",
                new String[]{uid, articleId});
    }

    public boolean isArticleSaved(String uid, String articleId) {
        return exists("SELECT 1 FROM saved_articles WHERE uid=? AND article_id=?", uid, articleId);
    }

    public void rateArticle(String uid, String articleId, int rating) {
        ContentValues values = new ContentValues();
        values.put("article_id", articleId);
        values.put("user_uid", uid);
        values.put("rating", rating);
        values.put("rated_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(
                "article_ratings", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Returns 0 if the user has not rated the article yet. */
    public int getArticleRating(String uid, String articleId) {
        return queryInt("SELECT rating FROM article_ratings WHERE user_uid=? AND article_id=?",
                uid, articleId);
    }

    // ---------------------------------------------------------------- groups

    /**
     * Creates the group with the creator and the given members. If an article is attached it is
     * posted as the first message. Returns the new group id.
     */
    public String createGroup(String groupName, String creatorUid, List<String> memberUids,
                              String articleId) {
        String groupId = UUID.randomUUID().toString();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("group_id", groupId);
            values.put("group_name", groupName);
            values.put("creator_uid", creatorUid);
            values.put("article_id", articleId);
            values.put("created_at", System.currentTimeMillis());
            db.insertOrThrow("chat_groups", null, values);

            addMemberToGroup(groupId, creatorUid);
            for (String uid : memberUids) {
                addMemberToGroup(groupId, uid);
            }
            if (articleId != null) {
                sendMessage(groupId, creatorUid, "", articleId);
            }
            db.setTransactionSuccessful();
            return groupId;
        } finally {
            db.endTransaction();
        }
    }

    public void addMemberToGroup(String groupId, String memberUid) {
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("member_uid", memberUid);
        values.put("joined_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(
                "group_members", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void leaveGroup(String groupId, String memberUid) {
        getWritableDatabase().delete("group_members", "group_id=? AND member_uid=?",
                new String[]{groupId, memberUid});
    }

    public boolean isGroupMember(String groupId, String uid) {
        return exists("SELECT 1 FROM group_members WHERE group_id=? AND member_uid=?",
                groupId, uid);
    }

    public ChatGroup getGroup(String groupId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT g.group_id, g.group_name, g.article_id, " +
                        "(SELECT COUNT(*) FROM group_members m WHERE m.group_id = g.group_id) " +
                        "FROM chat_groups g WHERE g.group_id = ?",
                new String[]{groupId})) {
            if (!c.moveToFirst()) {
                return null;
            }
            return new ChatGroup(c.getString(0), c.getString(1), c.getString(2), c.getInt(3),
                    null, 0);
        }
    }

    /** Groups the user belongs to, most recently active first. */
    public List<ChatGroup> getGroupsForUser(String uid) {
        List<ChatGroup> groups = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT g.group_id, g.group_name, g.article_id, " +
                        "(SELECT COUNT(*) FROM group_members m2 WHERE m2.group_id = g.group_id), " +
                        "(SELECT CASE WHEN msg.body <> '' THEN msg.body ELSE '📄 ' || a.title END " +
                        " FROM messages msg LEFT JOIN articles a ON a.article_id = msg.article_id " +
                        " WHERE msg.group_id = g.group_id ORDER BY msg.message_id DESC LIMIT 1), " +
                        "IFNULL((SELECT MAX(sent_at) FROM messages msg WHERE msg.group_id = g.group_id), " +
                        " g.created_at) AS last_active " +
                        "FROM chat_groups g " +
                        "INNER JOIN group_members m ON m.group_id = g.group_id " +
                        "WHERE m.member_uid = ? ORDER BY last_active DESC",
                new String[]{uid})) {
            while (c.moveToNext()) {
                groups.add(new ChatGroup(c.getString(0), c.getString(1), c.getString(2),
                        c.getInt(3), c.getString(4), c.getLong(5)));
            }
        }
        return groups;
    }

    public List<UserProfile> getGroupMembers(String groupId) {
        return readUsers(
                "SELECT u.uid, u.username, u.first_name, u.last_name " +
                        "FROM group_members m INNER JOIN users u ON u.uid = m.member_uid " +
                        "WHERE m.group_id = ? ORDER BY m.joined_at",
                new String[]{groupId});
    }

    // ---------------------------------------------------------------- messages

    /** Pass a null articleId for a plain text message. */
    public void sendMessage(String groupId, String senderUid, String body, String articleId) {
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("sender_uid", senderUid);
        values.put("body", body == null ? "" : body);
        values.put("article_id", articleId);
        values.put("sent_at", System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("messages", null, values);
    }

    /** Messages in send order, hiding anything from users the viewer has blocked. */
    public List<ChatMessage> getMessages(String groupId, String viewerUid) {
        List<ChatMessage> messages = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT msg.message_id, msg.sender_uid, u.username, u.first_name, u.last_name, " +
                        "msg.body, msg.article_id, a.title, msg.sent_at " +
                        "FROM messages msg " +
                        "INNER JOIN users u ON u.uid = msg.sender_uid " +
                        "LEFT JOIN articles a ON a.article_id = msg.article_id " +
                        "WHERE msg.group_id = ? AND msg.sender_uid NOT IN " +
                        "(SELECT blocked_uid FROM block WHERE blocker_uid = ?) " +
                        "ORDER BY msg.message_id",
                new String[]{groupId, viewerUid})) {
            while (c.moveToNext()) {
                UserProfile sender = new UserProfile(c.getString(1), c.getString(2),
                        c.getString(3), c.getString(4));
                messages.add(new ChatMessage(c.getLong(0), sender, c.getString(5),
                        c.getString(6), c.getString(7), c.getLong(8)));
            }
        }
        return messages;
    }

    // ---------------------------------------------------------------- helpers

    private boolean exists(String sql, String... args) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst();
        }
    }

    private int queryInt(String sql, String... args) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst() && !c.isNull(0) ? c.getInt(0) : 0;
        }
    }

    private double queryDouble(String sql, String... args) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst() && !c.isNull(0) ? c.getDouble(0) : 0;
        }
    }
}
