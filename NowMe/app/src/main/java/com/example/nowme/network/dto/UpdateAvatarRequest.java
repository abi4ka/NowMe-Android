package com.example.nowme.network.dto;

public class UpdateAvatarRequest {

    private String avatar;

    public UpdateAvatarRequest(String avatar) {
        this.avatar = avatar;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}