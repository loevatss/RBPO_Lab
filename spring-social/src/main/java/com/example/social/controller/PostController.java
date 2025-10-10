package com.example.social.controller;

import com.example.social.model.Post;
import com.example.social.service.SocialService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final SocialService service;

    public PostController(SocialService service) {
        this.service = service;
    }

    @PostMapping
    public Post create(@Valid @RequestBody Post post) {
        return service.createPost(post);
    }

    @GetMapping
    public List<Post> list() {
        return service.listPosts();
    }

    @GetMapping("/{id}")
    public Post get(@PathVariable Long id) {
        return service.getPost(id);
    }

    @GetMapping("/user/{userId}")
    public List<Post> byUser(@PathVariable Long userId) {
        return service.listPostsByUser(userId);
    }

    @PutMapping("/{id}")
    public Post update(@PathVariable Long id, @Valid @RequestBody Post changes) {
        return service.updatePost(id, changes);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deletePost(id);
    }

    @GetMapping("/feed/{userId}")
    public List<Post> feed(@PathVariable Long userId,
                           @RequestParam(name = "includeSelf", defaultValue = "false") boolean includeSelf) {
        return service.feed(userId, includeSelf);
    }
}
