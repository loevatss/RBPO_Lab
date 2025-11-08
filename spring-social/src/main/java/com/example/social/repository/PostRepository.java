package com.example.social.repository;

import com.example.social.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByUserId(Long userId);
    List<Post> findAllByUserIdInOrderByCreatedAtDesc(Collection<Long> userIds);
    void deleteAllByUserId(Long userId);
}
