package com.example.communityfeedapp.models;

public class NotificationModel {

    int profileImg;
    String notificationTitle, notificationTime;

    public NotificationModel(int profileImg, String notificationTitle, String notificationTime) {
        this.profileImg = profileImg;
        this.notificationTitle = notificationTitle;
        this.notificationTime = notificationTime;
    }

    public int getProfileImg() {
        return profileImg;
    }

    public void setProfileImg(int profileImg) {
        this.profileImg = profileImg;
    }

    public String getNotificationTitle() {
        return notificationTitle;
    }

    public void setNotificationTitle(String notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    public String getNotificationTime() {
        return notificationTime;
    }

    public void setNotificationTime(String notificationTime) {
        this.notificationTime = notificationTime;
    }
}
