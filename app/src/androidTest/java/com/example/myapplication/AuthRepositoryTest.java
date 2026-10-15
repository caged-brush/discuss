package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AuthRepositoryTest {
    private static final String PASSWORD = "a long password";

    private DBHandler db;
    private AuthRepository auth;

    @Before
    public void setUp() {
        db = new DBHandler(InstrumentationRegistry.getInstrumentation().getTargetContext(), null);
        auth = new AuthRepository(db);
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void signUpThenLogIn() throws Exception {
        String uid = auth.signUp("reader_1", "Ann", "Lee", "Ann@Example.com",
                PASSWORD.toCharArray());
        assertNotNull(uid);
        assertEquals("Ann Lee", db.getUser(uid).getDisplayName());

        // Email is matched case-insensitively
        assertEquals(uid, auth.logIn("ann@example.com", PASSWORD.toCharArray()));
        assertNull(auth.logIn("ann@example.com", "wrong password!".toCharArray()));
        assertNull(auth.logIn("other@example.com", PASSWORD.toCharArray()));
    }

    @Test
    public void rejectsDuplicatesAndBadInput() throws Exception {
        auth.signUp("reader_1", "", "", "ann@example.com", PASSWORD.toCharArray());

        assertThrows(IllegalArgumentException.class, () ->
                auth.signUp("READER_1", "", "", "new@example.com", PASSWORD.toCharArray()));
        assertThrows(IllegalArgumentException.class, () ->
                auth.signUp("reader_2", "", "", "ann@example.com", PASSWORD.toCharArray()));
        assertThrows(IllegalArgumentException.class, () ->
                auth.signUp("no spaces", "", "", "x@example.com", PASSWORD.toCharArray()));
        assertThrows(IllegalArgumentException.class, () ->
                auth.signUp("reader_3", "", "", "not-an-email", PASSWORD.toCharArray()));
        assertThrows(IllegalArgumentException.class, () ->
                auth.signUp("reader_3", "", "", "y@example.com", "short".toCharArray()));
    }
}
