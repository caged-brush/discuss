package com.example.myapplication;

public class ChatGroup {
    private final String id;
    private final String name;
    private final String articleId;
    private final int memberCount;
    private final String lastMessage;
    private final long lastActiveAt;

    public ChatGroup(String id, String name, String articleId, int memberCount,
                     String lastMessage, long lastActiveAt) {
        this.id = id;
        this.name = name;
        this.articleId = articleId;
        this.memberCount = memberCount;
        this.lastMessage = lastMessage;
        this.lastActiveAt = lastActiveAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    /** Article the group was created around, or null. */
    public String getArticleId() { return articleId; }
    public int getMemberCount() { return memberCount; }
    /** Preview of the newest message, or null if the group has no messages. */
    public String getLastMessage() { return lastMessage; }
    public long getLastActiveAt() { return lastActiveAt; }
}
