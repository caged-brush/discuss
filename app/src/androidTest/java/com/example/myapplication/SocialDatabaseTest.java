package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
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

import java.util.Collections;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class SocialDatabaseTest {
    private static final String ALICE = "alice-auth-uid";
    private static final String BOB = "bob-auth-uid";
    private static final String CAROL = "carol-auth-uid";
    private static final String URL = "https://example.com/news";

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final String databaseName = "social-test-" + UUID.randomUUID();
    private DBHandler db;

    @Before
    public void setUp() {
        db = new DBHandler(context, databaseName);
        db.ensureUser(ALICE, "Alice");
        db.ensureUser(BOB, "Bob");
        db.ensureUser(CAROL, "Bob");
    }

    @After
    public void tearDown() {
        db.close();
        context.deleteDatabase(databaseName);
    }

    @Test
    public void contactsAreScopedAndRatingsUpdate() {
        assertEquals(BOB, db.findUserUid("bOB"));
        assertEquals(CAROL, db.findUserUid(CAROL));
        assertNull(db.findUserUid("missing"));
        db.ensureUser(BOB, "Changed name");
        assertEquals(BOB, db.findUserUid("Bob"));
        db.addContact(ALICE, BOB);
        db.addContact(ALICE, BOB);
        assertEquals(1, count(db.getContacts(ALICE)));
        assertEquals(0, count(db.getContacts(BOB)));

        db.rateUser(ALICE, BOB, 3);
        db.rateUser(CAROL, BOB, 5);
        assertEquals(4.0, db.getAverageRating(BOB), 0);
        db.rateUser(ALICE, BOB, 1);
        assertEquals(1, db.getUserRating(ALICE, BOB));
        assertEquals(3.0, db.getAverageRating(BOB), 0);
        db.removeContact(ALICE, BOB);
        assertEquals(0, count(db.getContacts(ALICE)));
    }

    @Test
    public void articleSharingAndMessagesPersistAndRespectMembershipAndBlocks() {
        db.addContact(ALICE, BOB);
        db.saveArticle("Original title", null, null, URL, ALICE);
        db.saveArticle("Updated title", null, null, URL, ALICE);
        db.saveArticle("Bob's copy", null, null, URL, BOB);
        assertEquals(1, count(db.getSavedArticles(ALICE)));
        String aliceArticle = SavedArticle.load(db, ALICE).get(0).id;
        String bobArticle = SavedArticle.load(db, BOB).get(0).id;
        assertNotEquals(aliceArticle, bobArticle);

        String group = db.createGroupWithMembers("News group", ALICE,
                Collections.singletonList(BOB), Collections.singletonList(aliceArticle));
        assertEquals("News group", db.getGroupName(group, BOB));
        assertEquals(1, count(db.getGroups(ALICE)));
        assertEquals(1, count(db.getGroups(BOB)));
        assertEquals(0, count(db.getGroups(CAROL)));
        assertEquals(0, count(db.getMessages(group, CAROL)));
        assertThrows(IllegalArgumentException.class, () -> db.sendMessage(group, CAROL, "Not a member"));
        assertThrows(IllegalArgumentException.class, () -> db.sendMessage(group, ALICE, " \n "));
        db.sendMessage(group, BOB, "Reply");

        db.close();
        db = new DBHandler(context, databaseName);
        try (Cursor history = db.getMessages(group, ALICE)) {
            assertTrue(history.moveToFirst());
            assertEquals(ALICE, history.getString(0));
            assertEquals("Updated title", history.getString(2));
            assertEquals(URL, history.getString(3));
            assertTrue(history.moveToNext());
            assertEquals("Reply", history.getString(2));
            assertFalse(history.moveToNext());
        }

        db.blockUser(ALICE, BOB);
        assertEquals(1, count(db.getBlockedUsers(ALICE)));
        assertEquals(1, count(db.getMessages(group, ALICE)));
        assertEquals(1, count(db.getMessages(group, BOB)));
        assertThrows(IllegalArgumentException.class, () -> db.rateUser(BOB, ALICE, 5));
        assertThrows(IllegalArgumentException.class, () -> db.createGroupWithMembers("Blocked", ALICE,
                Collections.singletonList(BOB), Collections.emptyList()));
        assertEquals(1, count(db.getGroups(ALICE)));

        db.unblockUser(ALICE, BOB);
        assertEquals(0, count(db.getBlockedUsers(ALICE)));
        assertEquals(2, count(db.getMessages(group, ALICE)));
        assertThrows(IllegalArgumentException.class, () -> db.createGroupWithMembers("Wrong library", ALICE,
                Collections.singletonList(BOB), Collections.singletonList(bobArticle)));
        assertEquals(1, count(db.getGroups(BOB)));
    }

    @Test
    public void upgradingV1PreservesUsersAndGroupsAndEnablesMessages() {
        db.addContact(ALICE, BOB);
        db.rateUser(ALICE, BOB, 4);
        db.createGroup("legacy-group", "Legacy group", ALICE);
        SQLiteDatabase sqlite = db.getWritableDatabase();
        sqlite.execSQL("DROP TABLE messages");
        sqlite.setVersion(1);
        db.close();

        db = new DBHandler(context, databaseName);
        assertEquals(2, db.getReadableDatabase().getVersion());
        assertEquals(BOB, db.findUserUid("Bob"));
        assertEquals(1, count(db.getContacts(ALICE)));
        assertEquals(4.0, db.getAverageRating(BOB), 0);
        assertEquals(1, count(db.getGroups(ALICE)));
        db.sendMessage("legacy-group", ALICE, "After upgrade");
        assertEquals(1, count(db.getMessages("legacy-group", ALICE)));
    }

    private static int count(Cursor cursor) {
        try (Cursor rows = cursor) {
            return rows.getCount();
        }
    }
}
