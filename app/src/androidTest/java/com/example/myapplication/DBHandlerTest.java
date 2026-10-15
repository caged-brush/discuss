package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DBHandlerTest {
    private DBHandler db;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // A null name gives a fresh in-memory database for every test
        db = new DBHandler(context, null);
        insertUser("alice", "Alice", "Smith");
        insertUser("bob", "Bob", "");
        insertUser("carol", "", "");
    }

    @After
    public void tearDown() {
        db.close();
    }

    private void insertUser(String uid, String first, String last) {
        ContentValues values = new ContentValues();
        values.put("uid", uid);
        values.put("username", uid);
        values.put("first_name", first);
        values.put("last_name", last);
        values.put("created_at", System.currentTimeMillis());
        db.getWritableDatabase().insertOrThrow("users", null, values);
    }

    private String firstArticleId() {
        return db.getArticles(null).get(0).getId();
    }

    @Test
    public void seedArticlesAreLoaded() {
        assertEquals(10, db.getArticles(null).size());
        assertTrue(db.getCategories().contains("Economics"));
        for (Article article : db.getArticles("Science")) {
            assertEquals("Science", article.getCategory());
        }
    }

    @Test
    public void usernameLookupIgnoresCase() {
        UserProfile alice = db.findUserByUsername("ALICE");
        assertNotNull(alice);
        assertEquals("Alice Smith", alice.getDisplayName());
        assertEquals("carol", db.getUser("carol").getDisplayName());
        assertNull(db.findUserByUsername("nobody"));
    }

    @Test
    public void contactsCanBeAddedOnceAndRemoved() {
        assertTrue(db.addContact("alice", "bob"));
        assertFalse(db.addContact("alice", "bob"));
        assertFalse(db.addContact("alice", "ghost"));
        assertEquals(1, db.getContacts("alice").size());
        assertTrue(db.isContact("alice", "bob"));
        assertFalse(db.isContact("bob", "alice"));

        db.removeContact("alice", "bob");
        assertTrue(db.getContacts("alice").isEmpty());
    }

    @Test
    public void blockingRemovesContactAndHidesMessages() {
        db.addContact("alice", "bob");
        String group = db.createGroup("Chat", "alice", Collections.singletonList("bob"), null);
        db.sendMessage(group, "bob", "hi", null);

        db.blockUser("alice", "bob");
        assertTrue(db.isBlocked("alice", "bob"));
        assertFalse(db.isContact("alice", "bob"));
        assertTrue(db.getMessages(group, "alice").isEmpty());
        assertEquals(1, db.getMessages(group, "bob").size());

        db.unblockUser("alice", "bob");
        assertEquals(1, db.getMessages(group, "alice").size());
    }

    @Test
    public void userRatingsAverageAndReplace() {
        db.rateUser("alice", "carol", 5);
        db.rateUser("bob", "carol", 2);
        assertEquals(3.5, db.getAverageRating("carol"), 0.001);
        assertEquals(2, db.getRatingCount("carol"));

        db.rateUser("bob", "carol", 4);
        assertEquals(4.5, db.getAverageRating("carol"), 0.001);
        assertEquals(4, db.getUserRating("bob", "carol"));
        assertEquals(0, db.getUserRating("carol", "bob"));
    }

    @Test
    public void articleRatingsShowInListings() {
        String id = firstArticleId();
        db.rateArticle("alice", id, 5);
        db.rateArticle("bob", id, 3);
        Article article = db.getArticle(id);
        assertEquals(4.0, article.getAverageRating(), 0.001);
        assertEquals(2, article.getRatingCount());
        assertEquals(3, db.getArticleRating("bob", id));
        assertEquals(id, db.getRecentlyRatedArticles("alice", 5).get(0).getId());
    }

    @Test
    public void savedArticlesArePerUser() {
        String id = firstArticleId();
        db.saveArticle("alice", id);
        db.saveArticle("bob", id);
        assertTrue(db.isArticleSaved("alice", id));
        assertEquals(1, db.getSavedArticles("bob").size());

        db.unsaveArticle("alice", id);
        assertFalse(db.isArticleSaved("alice", id));
        assertTrue(db.isArticleSaved("bob", id));
    }

    @Test
    public void groupWithArticleStartsWithSharedMessage() {
        String id = firstArticleId();
        String group = db.createGroup("Readers", "alice", Collections.singletonList("bob"), id);

        assertEquals(2, db.getGroupMembers(group).size());
        assertTrue(db.isGroupMember(group, "bob"));
        assertFalse(db.isGroupMember(group, "carol"));
        List<ChatMessage> messages = db.getMessages(group, "bob");
        assertEquals(1, messages.size());
        assertEquals(id, messages.get(0).getArticleId());
        assertNotNull(messages.get(0).getArticleTitle());

        db.addMemberToGroup(group, "carol");
        db.sendMessage(group, "carol", "Joined!", null);
        ChatGroup listed = db.getGroupsForUser("carol").get(0);
        assertEquals("Readers", listed.getName());
        assertEquals(3, listed.getMemberCount());
        assertEquals("Joined!", listed.getLastMessage());

        db.leaveGroup(group, "carol");
        assertTrue(db.getGroupsForUser("carol").isEmpty());
    }
}
