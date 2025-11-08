package com.example.social.repository;

import com.example.social.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    int countByPostId(Long postId);
    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);
    void deleteByUserIdAndPostId(Long userId, Long postId);
    List<PostLike> findAllByPostId(Long postId);
    void deleteAllByPostId(Long postId);
    void deleteAllByUserId(Long userId);
}
