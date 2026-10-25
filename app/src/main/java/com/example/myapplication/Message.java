package com.example.myapplication;

public class Message {
    String userID;
    String message;
    public Message(String message, String userID){
        this.message = message;
        this.userID = userID;
    }

    public String getMessage(){
        return message;
    }
    public String getUserID(){
        return userID;
    }
}
