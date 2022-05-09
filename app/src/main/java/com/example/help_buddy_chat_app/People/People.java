package com.example.help_buddy_chat_app.People;

public class People
{
    //attributes of a user
    private String userName, userID;
    //boolean to determine whether the current user has sent them a friend request or not
    private boolean personAdded;

    public People(String userName, String userID, boolean personAdded) {
        this.userName = userName;
        this.userID = userID;
        this.personAdded = personAdded;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

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
