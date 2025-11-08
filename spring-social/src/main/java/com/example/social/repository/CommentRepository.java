package com.example.social.repository;

import com.example.social.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByPostId(Long postId);
    void deleteAllByPostId(Long postId);
    void deleteAllByUserId(Long userId);
}
