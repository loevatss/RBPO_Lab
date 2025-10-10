package com.example.social.service;

import com.example.social.model.Comment;
import com.example.social.model.Post;
import com.example.social.model.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class SocialService {

    private final ConcurrentMap<Long, User> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Post> posts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Comment> comments = new ConcurrentHashMap<>();

    // likes: key "userId:postId"
    private final Set<String> likes = ConcurrentHashMap.newKeySet();

    // follows: key "followerId>followeeId"
    private final Set<String> follows = ConcurrentHashMap.newKeySet();

    private final AtomicLong userSeq = new AtomicLong(0);
    private final AtomicLong postSeq = new AtomicLong(0);
    private final AtomicLong commentSeq = new AtomicLong(0);

    // region Users
    public User createUser(User u) {
        Objects.requireNonNull(u, "user");
        if (u.getUsername() == null || u.getUsername().isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (u.getName() == null || u.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        long id = userSeq.incrementAndGet();
        u.setId(id);
        users.put(id, u);
        return u;
    }

    public List<User> listUsers() {
        return users.values().stream()
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toList());
    }

    public User getUser(Long id) {
        User u = users.get(id);
        if (u == null) throw new NoSuchElementException("User not found: " + id);
        return u;
    }

    public User updateUser(Long id, User changes) {
        User existing = getUser(id);
        if (changes.getUsername() != null && !changes.getUsername().isBlank()) {
            existing.setUsername(changes.getUsername());
        }
        if (changes.getName() != null && !changes.getName().isBlank()) {
            existing.setName(changes.getName());
        }
        return existing;
    }

    public void deleteUser(Long id) {
        if (users.remove(id) == null) throw new NoSuchElementException("User not found: " + id);
        // clean related data
        posts.values().removeIf(p -> Objects.equals(p.getUserId(), id));
        comments.values().removeIf(c -> Objects.equals(c.getUserId(), id));
        likes.removeIf(key -> key.startsWith(id + ":"));
        likes.removeIf(key -> key.endsWith(":" + id)); // not necessary but safe
        follows.removeIf(key -> key.startsWith(id + ">") || key.endsWith(">" + id));
    }
    // endregion

    // region Posts
    public Post createPost(Post p) {
        Objects.requireNonNull(p, "post");
        if (!users.containsKey(p.getUserId())) {
            throw new NoSuchElementException("User not found: " + p.getUserId());
        }
        if (p.getText() == null || p.getText().isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        long id = postSeq.incrementAndGet();
        p.setId(id);
        p.setCreatedAt(Instant.now());
        posts.put(id, p);
        return p;
    }

    public List<Post> listPosts() {
        return posts.values().stream()
                .sorted(Comparator.comparing(Post::getId))
                .collect(Collectors.toList());
    }

    public List<Post> listPostsByUser(Long userId) {
        return posts.values().stream()
                .filter(p -> Objects.equals(p.getUserId(), userId))
                .sorted(Comparator.comparing(Post::getId))
                .collect(Collectors.toList());
    }

    public Post getPost(Long id) {
        Post p = posts.get(id);
        if (p == null) throw new NoSuchElementException("Post not found: " + id);
        return p;
    }

    public Post updatePost(Long id, Post changes) {
        Post existing = getPost(id);
        if (changes.getText() != null && !changes.getText().isBlank()) {
            existing.setText(changes.getText());
        }
        return existing;
    }

    public void deletePost(Long id) {
        if (posts.remove(id) == null) throw new NoSuchElementException("Post not found: " + id);
        comments.values().removeIf(c -> Objects.equals(c.getPostId(), id));
        likes.removeIf(key -> key.endsWith(":" + id));
    }

    public List<Post> feed(Long userId) {
        return feed(userId, false);
    }

    public List<Post> feed(Long userId, boolean includeSelf) {
        // posts by users that userId follows, optionally include user's own posts; newest first
        Set<Long> authors = new LinkedHashSet<>(getFollowees(userId));
        if (includeSelf) {
            authors.add(userId);
        }
        return posts.values().stream()
                .filter(p -> authors.contains(p.getUserId()))
                .sorted(Comparator.comparing(Post::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }
    // endregion

    // region Comments
    public Comment createComment(Comment c) {
        Objects.requireNonNull(c, "comment");
        if (!users.containsKey(c.getUserId())) throw new NoSuchElementException("User not found: " + c.getUserId());
        if (!posts.containsKey(c.getPostId())) throw new NoSuchElementException("Post not found: " + c.getPostId());
        if (c.getText() == null || c.getText().isBlank()) throw new IllegalArgumentException("text is required");
        long id = commentSeq.incrementAndGet();
        c.setId(id);
        c.setCreatedAt(Instant.now());
        comments.put(id, c);
        return c;
    }

    public Comment getComment(Long id) {
        Comment c = comments.get(id);
        if (c == null) throw new NoSuchElementException("Comment not found: " + id);
        return c;
    }

    public List<Comment> listCommentsByPost(Long postId) {
        return comments.values().stream()
                .filter(c -> Objects.equals(c.getPostId(), postId))
                .sorted(Comparator.comparing(Comment::getId))
                .collect(Collectors.toList());
    }

    public Comment updateComment(Long id, Comment changes) {
        Comment existing = getComment(id);
        if (changes.getText() != null && !changes.getText().isBlank()) {
            existing.setText(changes.getText());
        }
        return existing;
    }

    public void deleteComment(Long id) {
        if (comments.remove(id) == null) throw new NoSuchElementException("Comment not found: " + id);
    }
    // endregion

    // region Likes
    public int like(Long userId, Long postId) {
        ensureUserAndPost(userId, postId);
        likes.add(key(userId, postId));
        return getLikeCount(postId);
    }

    public int unlike(Long userId, Long postId) {
        ensureUserAndPost(userId, postId);
        likes.remove(key(userId, postId));
        return getLikeCount(postId);
    }

    public int getLikeCount(Long postId) {
        if (!posts.containsKey(postId)) throw new NoSuchElementException("Post not found: " + postId);
        int[] count = new int[]{0};
        String suffix = ":" + postId;
        likes.forEach(k -> { if (k.endsWith(suffix)) count[0]++; });
        return count[0];
    }

    public List<Long> getUserIdsWhoLiked(Long postId) {
        if (!posts.containsKey(postId)) throw new NoSuchElementException("Post not found: " + postId);
        String suffix = ":" + postId;
        return likes.stream()
                .filter(k -> k.endsWith(suffix))
                .map(k -> Long.parseLong(k.substring(0, k.indexOf(':'))))
                .sorted()
                .collect(Collectors.toList());
    }
    // endregion

    // region Follows
    public void follow(Long followerId, Long followeeId) {
        if (Objects.equals(followerId, followeeId)) {
            throw new IllegalArgumentException("cannot follow self");
        }
        ensureUser(followerId);
        ensureUser(followeeId);
        follows.add(fkey(followerId, followeeId));
    }

    public void unfollow(Long followerId, Long followeeId) {
        ensureUser(followerId);
        ensureUser(followeeId);
        follows.remove(fkey(followerId, followeeId));
    }

    public Set<Long> getFollowees(Long userId) {
        ensureUser(userId);
        String prefix = userId + ">";
        return follows.stream()
                .filter(k -> k.startsWith(prefix))
                .map(k -> Long.parseLong(k.substring(k.indexOf('>') + 1)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public java.util.List<com.example.social.dto.FollowPair> listAllFollows() {
        return follows.stream()
                .map(k -> {
                    int sep = k.indexOf('>');
                    Long follower = Long.parseLong(k.substring(0, sep));
                    Long followee = Long.parseLong(k.substring(sep + 1));
                    return new com.example.social.dto.FollowPair(follower, followee);
                })
                .sorted(Comparator.comparing(com.example.social.dto.FollowPair::getFollowerId)
                        .thenComparing(com.example.social.dto.FollowPair::getFolloweeId))
                .collect(Collectors.toList());
    }
    // endregion

    private void ensureUserAndPost(Long userId, Long postId) {
        ensureUser(userId);
        if (!posts.containsKey(postId)) throw new NoSuchElementException("Post not found: " + postId);
    }

    private void ensureUser(Long userId) {
        if (!users.containsKey(userId)) throw new NoSuchElementException("User not found: " + userId);
    }

    private static String key(Long userId, Long postId) {
        return userId + ":" + postId;
    }

    private static String fkey(Long followerId, Long followeeId) {
        return followerId + ">" + followeeId;
    }
}
