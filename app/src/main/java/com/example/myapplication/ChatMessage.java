package com.example.myapplication;

public class ChatMessage {
    private final long id;
    private final UserProfile sender;
    private final String body;
    private final String articleId;
    private final String articleTitle;
    private final long sentAt;

    public ChatMessage(long id, UserProfile sender, String body, String articleId,
                       String articleTitle, long sentAt) {
        this.id = id;
        this.sender = sender;
        this.body = body;
        this.articleId = articleId;
        this.articleTitle = articleTitle;
        this.sentAt = sentAt;
    }

    public long getId() { return id; }
    public UserProfile getSender() { return sender; }
    public String getBody() { return body; }
    /** Shared article, or null for a plain text message. */
    public String getArticleId() { return articleId; }
    public String getArticleTitle() { return articleTitle; }
    public long getSentAt() { return sentAt; }
}
