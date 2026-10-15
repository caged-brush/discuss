package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class PasswordHasherTest {
    // Low iteration count keeps the test fast; production uses PasswordHasher.ITERATIONS
    private static final int ITERATIONS = 1_000;

    @Test
    public void matchesCorrectPassword() throws Exception {
        byte[] salt = PasswordHasher.newSalt();
        byte[] hash = PasswordHasher.hash("correct horse".toCharArray(), salt, ITERATIONS);
        assertTrue(PasswordHasher.matches("correct horse".toCharArray(), salt, ITERATIONS, hash));
    }

    @Test
    public void rejectsWrongPassword() throws Exception {
        byte[] salt = PasswordHasher.newSalt();
        byte[] hash = PasswordHasher.hash("correct horse".toCharArray(), salt, ITERATIONS);
        assertFalse(PasswordHasher.matches("wrong horse".toCharArray(), salt, ITERATIONS, hash));
    }

    @Test
    public void saltsAreRandomAndHashesDiffer() throws Exception {
        byte[] salt1 = PasswordHasher.newSalt();
        byte[] salt2 = PasswordHasher.newSalt();
        assertEquals(16, salt1.length);
        assertFalse(Arrays.equals(salt1, salt2));
        assertNotEquals(
                Arrays.toString(PasswordHasher.hash("same".toCharArray(), salt1, ITERATIONS)),
                Arrays.toString(PasswordHasher.hash("same".toCharArray(), salt2, ITERATIONS)));
    }
}
