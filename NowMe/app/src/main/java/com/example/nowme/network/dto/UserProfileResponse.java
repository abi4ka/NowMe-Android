package com.example.nowme.network.dto;

public class UserProfileResponse {
    public Long id;
    public String username;
    public String avatar;
    public String registerTime;

    public Long followersCount;
    public Long followingCount;
    public Long friends;
    public Long streakDays;

    public boolean me;
    public boolean followingUser;
    public boolean following;
    public boolean friend;
}
