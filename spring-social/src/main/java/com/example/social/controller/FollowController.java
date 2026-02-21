package com.example.social.controller;

import com.example.social.dto.FollowRequest;
import com.example.social.service.SocialService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

// import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final SocialService service;

    public FollowController(SocialService service) {
        this.service = service;
    }

    @PostMapping
    public void follow(@Valid @RequestBody FollowRequest req) {
        service.follow(req.getFollowerId(), req.getFolloweeId());
    }

    @DeleteMapping
    public void unfollow(@Valid @RequestBody FollowRequest req) {
        service.unfollow(req.getFollowerId(), req.getFolloweeId());
    }

    @GetMapping("/{userId}")
    public Set<Long> followees(@PathVariable Long userId) {
        return service.getFollowees(userId);
    }

    @GetMapping
    public java.util.List<com.example.social.dto.FollowPair> all() {
        return service.listAllFollows();
    }
}
