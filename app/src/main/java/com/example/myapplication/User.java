package com.example.myapplication;

public class User {
    public String username;
    public String uID;
    public String[] articles = new String[]{"1", "2", "https://console.firebase.google.com/project/mobileapp2project/database/mobileapp2project-default-rtdb/data"};
    public User(){}

    public User(String username, String uID){

        this.username = username;
        this.uID = uID;
    }

}
