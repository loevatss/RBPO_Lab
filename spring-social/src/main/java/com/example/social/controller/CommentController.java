package com.example.social.controller;

import com.example.social.model.Comment;
import com.example.social.service.SocialService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final SocialService service;

    public CommentController(SocialService service) {
        this.service = service;
    }

    @PostMapping
    public Comment create(@Valid @RequestBody Comment comment) {
        return service.createComment(comment);
    }

    @GetMapping("/{id}")
    public Comment get(@PathVariable Long id) {
        return service.getComment(id);
    }

    @GetMapping("/post/{postId}")
    public List<Comment> byPost(@PathVariable Long postId) {
        return service.listCommentsByPost(postId);
    }

    @PutMapping("/{id}")
    public Comment update(@PathVariable Long id, @Valid @RequestBody Comment changes) {
        return service.updateComment(id, changes);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteComment(id);
    }
}

