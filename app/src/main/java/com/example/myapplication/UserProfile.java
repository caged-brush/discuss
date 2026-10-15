package com.example.myapplication;

/** Public profile of a user (no login data). */
public class UserProfile {
    private final String uid;
    private final String username;
    private final String firstName;
    private final String lastName;

    public UserProfile(String uid, String username, String firstName, String lastName) {
        this.uid = uid;
        this.username = username;
        this.firstName = firstName == null ? "" : firstName;
        this.lastName = lastName == null ? "" : lastName;
    }

    public String getUid() { return uid; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }

    /** Full name, falling back to the username when no name was given. */
    public String getDisplayName() {
        String name = (firstName + " " + lastName).trim();
        return name.isEmpty() ? username : name;
    }
}
