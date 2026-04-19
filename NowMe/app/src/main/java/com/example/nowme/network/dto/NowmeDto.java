package com.example.nowme.network.dto;

import java.io.Serializable;

public class NowmeDto implements Serializable {
    public Long id;
    public String description;
    public String creationTime;
    public Long likes;
    public Long comments;
    public String username;
    public String userAvatar;
    public Boolean liked;
}
