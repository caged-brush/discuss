package com.example.myapplication;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class PasswordHasher {
    static final int ITERATIONS = 600_000;
    private static final int HASH_BITS = 256;
    private PasswordHasher() { }

    static byte[] newSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    static byte[] hash(char[] password, byte[] salt, int iterations)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    static boolean matches(char[] password, byte[] salt, int iterations, byte[] expected)
            throws GeneralSecurityException {
        byte[] actual = hash(password, salt, iterations);
        try {
            return MessageDigest.isEqual(actual, expected);
        } finally {
            java.util.Arrays.fill(actual, (byte) 0);
        }
    }
}
