package com.example.myapplication;

public class ArticleBrowseRecyclerDataModel {
    private String title, thumbnail;
    private int id;
    public ArticleBrowseRecyclerDataModel(String title, String thumbnail, int id){
        this.title = title;
        this.thumbnail = thumbnail;
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title){
        this.title = title;
    }
    public String getThumbnail(){
        return thumbnail;
    }
    public void setThumbnail(String thumbnail){
        this.thumbnail = thumbnail;
    }
    public int getId(){
        return this.id;
    }
}
