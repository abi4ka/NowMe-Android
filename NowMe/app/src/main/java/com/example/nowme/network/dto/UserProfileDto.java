package com.example.nowme.network.dto;

public class UserProfileDto {
    public Long id;
    public String username;
    public String avatar;
    public String registerTime;

    public Long followerCount;
    public Long followingCount;
    public Long friends;

    public boolean me;
    public boolean followingUser;
    public boolean friend;
}