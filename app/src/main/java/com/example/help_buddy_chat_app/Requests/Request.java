package com.example.help_buddy_chat_app.Requests;

public class Request {

    private String userId, userName, userPicture;

    public Request(String userId, String userName, String userPicture) {
        this.userId = userId;
        this.userName = userName;
        this.userPicture = userPicture;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPicture() { return userPicture; }

    public void setUserPicture(String userPicture) { this.userPicture = userPicture; }
}
