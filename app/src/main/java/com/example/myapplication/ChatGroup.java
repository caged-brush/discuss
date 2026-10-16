package com.example.myapplication;

public class ChatGroup {
    private final String id;
    private final String name;
    private final String articleId;
    private final int memberCount;
    private final String lastMessage;
    private final long lastActiveAt;
    private final boolean direct;
    private final String peerUid;

    public ChatGroup(String id, String name, String articleId, int memberCount,
                     String lastMessage, long lastActiveAt, boolean direct, String peerUid) {
        this.id = id;
        this.name = name;
        this.articleId = articleId;
        this.memberCount = memberCount;
        this.lastMessage = lastMessage;
        this.lastActiveAt = lastActiveAt;
        this.direct = direct;
        this.peerUid = peerUid;
    }

    public String getId() { return id; }
    /** For a direct chat this is the other person's name. */
    public String getName() { return name; }
    /** Article the group was created around, or null. */
    public String getArticleId() { return articleId; }
    public int getMemberCount() { return memberCount; }
    /** Preview of the newest message, or null if the group has no messages. */
    public String getLastMessage() { return lastMessage; }
    public long getLastActiveAt() { return lastActiveAt; }
    /** True for a one-to-one chat. */
    public boolean isDirect() { return direct; }
    /** The other person in a direct chat, or null (also null if they left). */
    public String getPeerUid() { return peerUid; }
}
