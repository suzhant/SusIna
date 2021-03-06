package com.sushant.whatsapp.Models;

import java.util.Objects;

public class Messages {
    private String uId, message, messageId, profilePic, senderId, receiverId, senderName, receiverName, type, imageUrl, audioFile, videoFile;
    private Long timestamp;
    private int Reaction = -1;

    public Messages(String uId, String profilePic, Long timestamp) {
        this.uId = uId;
        this.profilePic = profilePic;
        this.timestamp = timestamp;
    }

    public Messages(String uId, String message) {
        this.uId = uId;
        this.message = message;
    }


    public Messages() {
    }

    public Messages(String uId, String message, String profilePic) {
        this.uId = uId;
        this.message = message;
        this.profilePic = profilePic;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(String audioFile) {
        this.audioFile = audioFile;
    }

    public int getReaction() {
        return Reaction;
    }

    public void setReaction(int reaction) {
        Reaction = reaction;
    }

    public String getVideoFile() {
        return videoFile;
    }

    public void setVideoFile(String videoFile) {
        this.videoFile = videoFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Messages messages = (Messages) o;
        return Reaction == messages.Reaction && Objects.equals(message, messages.message) && Objects.equals(messageId, messages.messageId)
                && Objects.equals(profilePic, messages.profilePic) && Objects.equals(senderId, messages.senderId) && Objects.equals(receiverId, messages.receiverId)
                && Objects.equals(senderName, messages.senderName) && Objects.equals(receiverName, messages.receiverName) && Objects.equals(type, messages.type)
                && Objects.equals(imageUrl, messages.imageUrl) && Objects.equals(audioFile, messages.audioFile) && Objects.equals(videoFile, messages.videoFile)
                && Objects.equals(timestamp, messages.timestamp) && Objects.equals(uId, messages.uId);
    }
}
