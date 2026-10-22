package com.example.myapplication;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Patterns;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.UUID;

/** Local accounts only. Run signup/login on a background executor, never the UI thread. */
public final class AuthRepository {
    private final DBHandler helper;

    public AuthRepository(DBHandler helper) {
        this.helper = helper;
    }

    /** Inserts the profile and credentials together. A failed insert rolls back both. */
    public String signUp(String username, String firstName, String lastName,
                         String email, char[] password) throws GeneralSecurityException {
        username = username == null ? "" : username.trim();
        if (!username.matches("[A-Za-z0-9_]{3,30}")) {
            throw new IllegalArgumentException("Username: use 3-30 letters, numbers or underscores.");
        }
        email = normalizedEmail(email);
        if (password == null || password.length < 12 || password.length > 128) {
            throw new IllegalArgumentException("Use a password containing 12-128 characters.");
        }
        byte[] salt = PasswordHasher.newSalt();
        byte[] hash = PasswordHasher.hash(password, salt, PasswordHasher.ITERATIONS);
        String uid = UUID.randomUUID().toString();
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues profile = new ContentValues();
            profile.put("uid", uid);
            profile.put("username", username);
            profile.put("first_name", firstName == null ? "" : firstName.trim());
            profile.put("last_name", lastName == null ? "" : lastName.trim());
            profile.put("created_at", System.currentTimeMillis());
            db.insertOrThrow("users", null, profile);

            ContentValues credentials = new ContentValues();
            credentials.put("uid", uid);
            credentials.put("email", email);
            credentials.put("password_hash", hash);
            credentials.put("password_salt", salt);
            credentials.put("password_iterations", PasswordHasher.ITERATIONS);
            db.insertOrThrow("credentials", null, credentials);
            db.setTransactionSuccessful();
            return uid;
        } catch (Exception e) {
            Log.d("AUTH_REPO", "ERRORRRR", e);
            return null;
        } finally {
            db.endTransaction();
            java.util.Arrays.fill(hash, (byte) 0);
        }
    }

    /** Returns a stable UID on success, null for an incorrect email/password. */
    public String logIn(String email, char[] password) throws GeneralSecurityException {
        email = normalizedEmail(email);
        if (password == null || password.length == 0 || password.length > 128) {
            throw new IllegalArgumentException("Enter your password (maximum 128 characters).");
        }
        try (Cursor c = helper.getReadableDatabase().query("credentials",
                new String[]{"uid", "password_hash", "password_salt", "password_iterations"},
                "email = ?", new String[]{email}, null, null, null)) {
            if (!c.moveToFirst()) {
                return null;
            }
            return PasswordHasher.matches(password, c.getBlob(2), c.getInt(3), c.getBlob(1))
                    ? c.getString(0) : null;
        }
    }

    private static String normalizedEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !Patterns.EMAIL_ADDRESS.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        return normalized;
    }
}
