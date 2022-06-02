package com.example.help_buddy_chat_app.People;

public class People
{
    //attributes of a user
    private String userName, userPicture, userID;
    //boolean to determine whether the current user has sent them a friend request or not
    private boolean personAdded;

    public People(String userName, String userPicture, String userID, boolean personAdded) {
        this.userName = userName;
        this.userPicture = userPicture;
        this.userID = userID;
        this.personAdded = personAdded;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPicture() {
        return userPicture;
    }

    public void setUserPicture(String userPicture) { this.userPicture = userPicture; }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public boolean isPersonAdded() {
        return personAdded;
    }

    public void setPersonAdded(boolean personAdded) {
        this.personAdded = personAdded;
    }
}
