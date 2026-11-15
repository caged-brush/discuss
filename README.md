# Discuss

Discuss uses Firebase Authentication for sign-in and NewsAPI for article browsing. Contacts, ratings, saved articles, groups, messages and blocks are stored in SQLite on the current installation. An account becomes discoverable by username or UID after it opens the app on that installation.

## Local verification

With the Android SDK configured, run the following commands from the project directory:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

The second command requires a running Android emulator or connected device.

1. Sign in with two Firebase test accounts on the same installation, signing out between accounts via **Account → Sign out**.
2. As the first account, open **Contacts**, add the second account by username, and tap its row to submit a rating. Change your rating and verify that the average updates.
3. Open an article from the feed and tap **Save to library**. Confirm it appears in **Account → My profile → Recent Articles**.
4. Open **Chats → New group**, enter a name, select the contact and an optional saved article, and create the group. Send a message, reopen the group, and confirm that the history persists.
5. Sign in as the second account on that installation. Open the group and reply. From the first account, block the second account from its profile and check that its messages are hidden. Open **Contacts → Blocked users** to unblock it.
6. Remove the second account from **Contacts** and confirm the row disappears.
