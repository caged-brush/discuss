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

    @Test
    public void directChatIsReusedAndNamedAfterThePeer() {
        String chat = db.getOrCreateDirectGroup("alice", "bob");
        assertEquals(chat, db.getOrCreateDirectGroup("alice", "bob"));
        assertEquals(chat, db.getOrCreateDirectGroup("bob", "alice"));
        assertFalse(chat.equals(db.getOrCreateDirectGroup("alice", "carol")));

        ChatGroup forAlice = db.getGroup(chat, "alice");
        assertTrue(forAlice.isDirect());
        assertEquals("Bob", forAlice.getName());
        assertEquals("bob", forAlice.getPeerUid());
        ChatGroup forBob = db.getGroup(chat, "bob");
        assertEquals("Alice Smith", forBob.getName());
        assertEquals("alice", forBob.getPeerUid());
        assertEquals(2, db.getGroupsForUser("alice").size());
    }

    @Test
    public void recommendingAnArticleCreatesAMessageInTheDirectChat() {
        String id = firstArticleId();
        String chat = db.getOrCreateDirectGroup("alice", "bob");
        db.sendMessage(chat, "alice", "", id);

        List<ChatMessage> messages = db.getMessages(chat, "bob");
        assertEquals(1, messages.size());
        assertEquals(id, messages.get(0).getArticleId());
        assertNull(messages.get(0).getMediaType());
    }

    @Test
    public void photoAndVoiceMessagesAreStoredAndPreviewed() {
        String chat = db.getOrCreateDirectGroup("alice", "bob");
        db.sendMediaMessage(chat, "alice", ChatMessage.MEDIA_IMAGE, "img_1.jpg", 0);
        List<ChatMessage> messages = db.getMessages(chat, "bob");
        assertEquals(ChatMessage.MEDIA_IMAGE, messages.get(0).getMediaType());
        assertEquals("img_1.jpg", messages.get(0).getMediaPath());
        assertTrue(db.getGroupsForUser("bob").get(0).getLastMessage().contains("Photo"));

        db.sendMediaMessage(chat, "bob", ChatMessage.MEDIA_AUDIO, "voice_1.m4a", 4200);
        messages = db.getMessages(chat, "alice");
        assertEquals(2, messages.size());
        assertEquals(ChatMessage.MEDIA_AUDIO, messages.get(1).getMediaType());
        assertEquals(4200, messages.get(1).getMediaDurationMs());
        assertTrue(db.getGroupsForUser("alice").get(0).getLastMessage().contains("Voice message"));
    }

    @Test
    public void videosCanBeUploadedListedHiddenAndDeleted() {
        String id = db.addVideo("alice", "My first video", "About me", "vid_1.mp4", "thumb_1.jpg", 12_000);
        db.addVideo("bob", "Bob's video", "", "vid_2.mp4", "", 5_000);

        assertEquals(2, db.getVideos("carol").size());
        Video video = db.getVideo(id);
        assertEquals("My first video", video.getTitle());
        assertEquals("Alice Smith", video.getUploader().getDisplayName());
        assertEquals(12_000, video.getDurationMs());
        assertEquals(1, db.getVideosByUser("alice").size());

        // Blocked uploaders disappear from the feed but not from their own profile
        db.blockUser("carol", "alice");
        assertEquals(1, db.getVideos("carol").size());
        assertEquals(1, db.getVideosByUser("alice").size());

        // Only the uploader may delete
        assertNull(db.deleteVideo(id, "bob"));
        assertEquals(2, db.getVideos("bob").size());
        Video removed = db.deleteVideo(id, "alice");
        assertEquals("vid_1.mp4", removed.getFileName());
        assertNull(db.getVideo(id));
    }
}
