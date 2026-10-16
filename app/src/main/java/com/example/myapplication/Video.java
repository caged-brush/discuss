package com.example.myapplication;

/** A video uploaded by a user. File names are relative to the app's videos folder. */
public class Video {
    private final String id;
    private final UserProfile uploader;
    private final String title;
    private final String description;
    private final String fileName;
    private final String thumbName;
    private final long durationMs;
    private final long createdAt;

    public Video(String id, UserProfile uploader, String title, String description,
                 String fileName, String thumbName, long durationMs, long createdAt) {
        this.id = id;
        this.uploader = uploader;
        this.title = title;
        this.description = description;
        this.fileName = fileName;
        this.thumbName = thumbName;
        this.durationMs = durationMs;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public UserProfile getUploader() { return uploader; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getFileName() { return fileName; }
    /** Empty when no thumbnail could be generated. */
    public String getThumbName() { return thumbName; }
    public long getDurationMs() { return durationMs; }
    public long getCreatedAt() { return createdAt; }
}
