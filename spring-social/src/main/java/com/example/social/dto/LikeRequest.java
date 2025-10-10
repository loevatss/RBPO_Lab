package com.example.social.dto;

import jakarta.validation.constraints.NotNull;

public class LikeRequest {
    @NotNull
    private Long userId;
    @NotNull
    private Long postId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }
}

