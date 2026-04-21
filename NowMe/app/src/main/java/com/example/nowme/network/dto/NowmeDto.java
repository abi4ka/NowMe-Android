package com.example.nowme.network.dto;

import java.io.Serializable;

public class NowmeDto implements Serializable {
    public Long id;
    public Long userId;
    public String description;
    public String creationTime;
    public Long likes;
    public Long comments;
    public String username;
    public String userAvatar;
    public Boolean favorite;
    public Boolean liked;
}
