package com.example.social.repository;

import com.example.social.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    List<Follow> findAllByFollowerId(Long followerId);
    Optional<Follow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    void deleteAllByFollowerId(Long followerId);
    void deleteAllByFolloweeId(Long followeeId);
}
