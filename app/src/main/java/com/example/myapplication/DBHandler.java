package com.example.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHandler  extends SQLiteOpenHelper {
    private static final String DB_NAME = "discuss";
    private static final int DB_VERSION = 1;

    public DBHandler(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public void onCOnfigure(SQLiteDatabase db){
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
                "PRIMARY KEY (blocker_uid, blocker_uid), " +
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
       throw new IllegalStateException(
               "Missing database migration from " + oldVersion + " to " + newVersion
       );
    }
}
