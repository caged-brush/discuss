package com.example.myapplication;

public class Article {
    private final String id;
    private final String title;
    private final String author;
    private final String category;
    private final String body;
    private final double averageRating;
    private final int ratingCount;

    public Article(String id, String title, String author, String category, String body,
                   double averageRating, int ratingCount) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.body = body;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public String getBody() { return body; }
    public double getAverageRating() { return averageRating; }
    public int getRatingCount() { return ratingCount; }
}
