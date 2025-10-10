package com.example.social.controller;

import com.example.social.dto.LikeRequest;
import com.example.social.service.SocialService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/likes")
public class LikeController {

    private final SocialService service;

    public LikeController(SocialService service) {
        this.service = service;
    }

    @PostMapping
    public Map<String, Object> like(@Valid @RequestBody LikeRequest req) {
        int count = service.like(req.getUserId(), req.getPostId());
        Map<String, Object> resp = new HashMap<>();
        resp.put("postId", req.getPostId());
        resp.put("likes", count);
        return resp;
    }

    @DeleteMapping
    public Map<String, Object> unlike(@Valid @RequestBody LikeRequest req) {
        int count = service.unlike(req.getUserId(), req.getPostId());
        Map<String, Object> resp = new HashMap<>();
        resp.put("postId", req.getPostId());
        resp.put("likes", count);
        return resp;
    }

    @GetMapping("/post/{postId}")
    public Map<String, Object> likes(@PathVariable Long postId) {
        int count = service.getLikeCount(postId);
        List<Long> userIds = service.getUserIdsWhoLiked(postId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("postId", postId);
        resp.put("likes", count);
        resp.put("userIds", userIds);
        return resp;
    }
}

