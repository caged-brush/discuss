package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Remembers which local account is logged in. */
public final class Session {
    private static final String PREFS = "session";
    private static final String KEY_UID = "uid";

    private Session() { }

    public static String currentUid(Context context) {
        return prefs(context).getString(KEY_UID, null);
    }

    public static void logIn(Context context, String uid) {
        prefs(context).edit().putString(KEY_UID, uid).apply();
    }

    public static void logOut(Context context) {
        prefs(context).edit().remove(KEY_UID).apply();
    }

    /**
     * Returns the logged-in uid, or sends the user back to the welcome page and returns null.
     * Call at the top of onCreate in every screen that needs an account.
     */
    public static String requireUser(Activity activity) {
        String uid = currentUid(activity);
        if (uid != null && DBHandler.getInstance(activity).getUser(uid) != null) {
            return uid;
        }
        logOut(activity);
        Intent intent = new Intent(activity, WelcomePage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return null;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
