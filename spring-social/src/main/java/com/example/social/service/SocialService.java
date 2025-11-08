package com.example.social.service;

import com.example.social.dto.FollowPair;
import com.example.social.model.*;
import com.example.social.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SocialService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final FollowRepository followRepository;

    public SocialService(UserRepository userRepository,
                         PostRepository postRepository,
                         CommentRepository commentRepository,
                         PostLikeRepository postLikeRepository,
                         FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.followRepository = followRepository;
    }

    // region Users
    public User createUser(User u) {
        Objects.requireNonNull(u, "user");
        if (u.getUsername() == null || u.getUsername().isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (u.getName() == null || u.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        return userRepository.save(u);
    }

    public List<User> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    public User updateUser(Long id, User changes) {
        User existing = getUser(id);
        if (changes.getUsername() != null && !changes.getUsername().isBlank()) {
            existing.setUsername(changes.getUsername());
        }
        if (changes.getName() != null && !changes.getName().isBlank()) {
            existing.setName(changes.getName());
        }
        return userRepository.save(existing);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) throw new NoSuchElementException("User not found: " + id);

        // clean related data in safe order
        // delete follows
        followRepository.deleteAllByFollowerId(id);
        followRepository.deleteAllByFolloweeId(id);
        // delete likes by user
        postLikeRepository.deleteAllByUserId(id);
        // delete comments by user
        commentRepository.deleteAllByUserId(id);
        // delete posts by user -> and related likes/comments
        postRepository.findAllByUserId(id).forEach(p -> {
            postLikeRepository.deleteAllByPostId(p.getId());
            commentRepository.deleteAllByPostId(p.getId());
        });
        postRepository.deleteAllByUserId(id);

        userRepository.deleteById(id);
    }
    // endregion

    // region Posts
    public Post createPost(Post p) {
        Objects.requireNonNull(p, "post");
        ensureUser(p.getUserId());
        if (p.getText() == null || p.getText().isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        return postRepository.save(p);
    }

    public List<Post> listPosts() {
        return postRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public List<Post> listPostsByUser(Long userId) {
        return postRepository.findAllByUserId(userId);
    }

    public Post getPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Post not found: " + id));
    }

    public Post updatePost(Long id, Post changes) {
        Post existing = getPost(id);
        if (changes.getText() != null && !changes.getText().isBlank()) {
            existing.setText(changes.getText());
        }
        return postRepository.save(existing);
    }

    public void deletePost(Long id) {
        // ensure exists
        if (!postRepository.existsById(id)) throw new NoSuchElementException("Post not found: " + id);
        postLikeRepository.deleteAllByPostId(id);
        commentRepository.deleteAllByPostId(id);
        postRepository.deleteById(id);
    }

    public List<Post> feed(Long userId) { return feed(userId, false); }

    public List<Post> feed(Long userId, boolean includeSelf) {
        ensureUser(userId);
        Set<Long> authors = getFollowees(userId);
        if (includeSelf) authors.add(userId);
        if (authors.isEmpty()) return List.of();
        return postRepository.findAllByUserIdInOrderByCreatedAtDesc(authors);
    }
    // endregion

    // region Comments
    public Comment createComment(Comment c) {
        Objects.requireNonNull(c, "comment");
        ensureUser(c.getUserId());
        ensurePost(c.getPostId());
        if (c.getText() == null || c.getText().isBlank()) throw new IllegalArgumentException("text is required");
        return commentRepository.save(c);
    }

    public Comment getComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Comment not found: " + id));
    }

    public List<Comment> listCommentsByPost(Long postId) {
        return commentRepository.findAllByPostId(postId);
    }

    public Comment updateComment(Long id, Comment changes) {
        Comment existing = getComment(id);
        if (changes.getText() != null && !changes.getText().isBlank()) {
            existing.setText(changes.getText());
        }
        return commentRepository.save(existing);
    }

    public void deleteComment(Long id) {
        if (!commentRepository.existsById(id)) throw new NoSuchElementException("Comment not found: " + id);
        commentRepository.deleteById(id);
    }
    // endregion

    // region Likes
    public int like(Long userId, Long postId) {
        ensureUserAndPost(userId, postId);
        postLikeRepository.findByUserIdAndPostId(userId, postId)
                .orElseGet(() -> postLikeRepository.save(newLike(userId, postId)));
        return getLikeCount(postId);
    }

    public int unlike(Long userId, Long postId) {
        ensureUserAndPost(userId, postId);
        postLikeRepository.deleteByUserIdAndPostId(userId, postId);
        return getLikeCount(postId);
    }

    public int getLikeCount(Long postId) {
        ensurePost(postId);
        return postLikeRepository.countByPostId(postId);
    }

    public List<Long> getUserIdsWhoLiked(Long postId) {
        ensurePost(postId);
        return postLikeRepository.findAllByPostId(postId)
                .stream()
                .map(PostLike::getUserId)
                .sorted()
                .toList();
    }
    // endregion

    // region Follows
    public void follow(Long followerId, Long followeeId) {
        if (Objects.equals(followerId, followeeId)) throw new IllegalArgumentException("cannot follow self");
        ensureUser(followerId);
        ensureUser(followeeId);
        followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .orElseGet(() -> followRepository.save(newFollow(followerId, followeeId)));
    }

    public void unfollow(Long followerId, Long followeeId) {
        ensureUser(followerId);
        ensureUser(followeeId);
        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    public Set<Long> getFollowees(Long userId) {
        ensureUser(userId);
        return followRepository.findAllByFollowerId(userId)
                .stream()
                .map(Follow::getFolloweeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<FollowPair> listAllFollows() {
        return followRepository.findAll(Sort.by("followerId", "followeeId"))
                .stream()
                .map(f -> new FollowPair(f.getFollowerId(), f.getFolloweeId()))
                .toList();
    }
    // endregion

    private void ensureUserAndPost(Long userId, Long postId) {
        ensureUser(userId);
        ensurePost(postId);
    }

    private void ensureUser(Long userId) {
        if (!userRepository.existsById(userId)) throw new NoSuchElementException("User not found: " + userId);
    }

    private void ensurePost(Long postId) {
        if (!postRepository.existsById(postId)) throw new NoSuchElementException("Post not found: " + postId);
    }

    private static PostLike newLike(Long userId, Long postId) {
        PostLike pl = new PostLike();
        pl.setUserId(userId);
        pl.setPostId(postId);
        return pl;
    }

    private static Follow newFollow(Long followerId, Long followeeId) {
        Follow f = new Follow();
        f.setFollowerId(followerId);
        f.setFolloweeId(followeeId);
        return f;
    }
}
