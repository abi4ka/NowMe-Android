package com.example.nowme.network.dto;

public class UserSearchResponse {

    public Long id;
    public String username;
    public String avatar;

    public UserSearchResponse() {
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatar() {
        return avatar;
    }
}