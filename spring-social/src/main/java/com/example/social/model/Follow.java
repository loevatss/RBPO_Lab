package com.example.social.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "follows",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_follows_pair", columnNames = {"follower_id", "followee_id"})
        },
        indexes = {
                @Index(name = "idx_follows_follower", columnList = "follower_id"),
                @Index(name = "idx_follows_followee", columnList = "followee_id")
        })
@Check(constraints = "follower_id <> followee_id")
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", insertable = false, updatable = false)
    private User follower;

    @NotNull
    @Column(name = "followee_id", nullable = false)
    private Long followeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followee_id", insertable = false, updatable = false)
    private User followee;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFollowerId() { return followerId; }
    public void setFollowerId(Long followerId) { this.followerId = followerId; }
    public Long getFolloweeId() { return followeeId; }
    public void setFolloweeId(Long followeeId) { this.followeeId = followeeId; }
}

