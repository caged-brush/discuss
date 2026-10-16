package com.example.myapplication;

public class ChatMessage {
    public static final String MEDIA_IMAGE = "image";
    public static final String MEDIA_AUDIO = "audio";

    private final long id;
    private final UserProfile sender;
    private final String body;
    private final String articleId;
    private final String articleTitle;
    private final String mediaType;
    private final String mediaPath;
    private final long mediaDurationMs;
    private final long sentAt;

    public ChatMessage(long id, UserProfile sender, String body, String articleId,
                       String articleTitle, String mediaType, String mediaPath,
                       long mediaDurationMs, long sentAt) {
        this.id = id;
        this.sender = sender;
        this.body = body;
        this.articleId = articleId;
        this.articleTitle = articleTitle;
        this.mediaType = mediaType;
        this.mediaPath = mediaPath;
        this.mediaDurationMs = mediaDurationMs;
        this.sentAt = sentAt;
    }

    public long getId() { return id; }
    public UserProfile getSender() { return sender; }
    public String getBody() { return body; }
    /** Shared article, or null for a plain text message. */
    public String getArticleId() { return articleId; }
    public String getArticleTitle() { return articleTitle; }
    /** {@link #MEDIA_IMAGE}, {@link #MEDIA_AUDIO}, or null for text and article messages. */
    public String getMediaType() { return mediaType; }
    /** File name inside the chat media folder. */
    public String getMediaPath() { return mediaPath; }
    public long getMediaDurationMs() { return mediaDurationMs; }
    public long getSentAt() { return sentAt; }
}
