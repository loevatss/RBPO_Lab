-- Users (idempotent)
INSERT INTO users (username, name) VALUES ('alice', 'Alice')
ON CONFLICT (username) DO NOTHING;
INSERT INTO users (username, name) VALUES ('bob', 'Bob')
ON CONFLICT (username) DO NOTHING;
INSERT INTO users (username, name) VALUES ('carol', 'Carol')
ON CONFLICT (username) DO NOTHING;

-- Posts (idempotent per (user,text))
INSERT INTO posts (user_id, text, created_at)
SELECT u.id, 'Hello, this is Alice''s first post!', NOW()
FROM users u
WHERE u.username = 'alice'
  AND NOT EXISTS (
    SELECT 1 FROM posts p WHERE p.user_id = u.id AND p.text = 'Hello, this is Alice''s first post!'
  );

INSERT INTO posts (user_id, text, created_at)
SELECT u.id, 'Bob here. Loving Spring Boot!', NOW()
FROM users u
WHERE u.username = 'bob'
  AND NOT EXISTS (
    SELECT 1 FROM posts p WHERE p.user_id = u.id AND p.text = 'Bob here. Loving Spring Boot!'
  );

INSERT INTO posts (user_id, text, created_at)
SELECT u.id, 'Carol joined the network.', NOW()
FROM users u
WHERE u.username = 'carol'
  AND NOT EXISTS (
    SELECT 1 FROM posts p WHERE p.user_id = u.id AND p.text = 'Carol joined the network.'
  );

-- Comments (idempotent per (post_id,user_id,text))
INSERT INTO comments (post_id, user_id, text, created_at)
SELECT p.id, u_bob.id, 'Nice to see you, Alice!', NOW()
FROM posts p
JOIN users u_alice ON u_alice.id = p.user_id AND u_alice.username = 'alice'
JOIN users u_bob   ON u_bob.username = 'bob'
WHERE p.text = 'Hello, this is Alice''s first post!'
  AND NOT EXISTS (
    SELECT 1 FROM comments c WHERE c.post_id = p.id AND c.user_id = u_bob.id AND c.text = 'Nice to see you, Alice!'
  );

INSERT INTO comments (post_id, user_id, text, created_at)
SELECT p.id, u_alice.id, 'Welcome Bob!', NOW()
FROM posts p
JOIN users u_bob   ON u_bob.id = p.user_id AND u_bob.username = 'bob'
JOIN users u_alice ON u_alice.username = 'alice'
WHERE p.text = 'Bob here. Loving Spring Boot!'
  AND NOT EXISTS (
    SELECT 1 FROM comments c WHERE c.post_id = p.id AND c.user_id = u_alice.id AND c.text = 'Welcome Bob!'
  );

-- Seed follows
INSERT INTO follows (follower_id, followee_id)
VALUES
  ((SELECT id FROM users WHERE username='bob'), (SELECT id FROM users WHERE username='alice')),
  ((SELECT id FROM users WHERE username='carol'), (SELECT id FROM users WHERE username='alice')),
  ((SELECT id FROM users WHERE username='alice'), (SELECT id FROM users WHERE username='bob'))
ON CONFLICT DO NOTHING;

-- Seed likes
-- INSERT INTO post_likes (user_id, post_id)
-- VALUES
--   ((SELECT id FROM users WHERE username='bob'), (SELECT p.id FROM posts p WHERE p.text LIKE 'Hello, this is Alice%')),
--   ((SELECT id FROM users WHERE username='carol'), (SELECT p.id FROM posts p WHERE p.text LIKE 'Hello, this is Alice%')),
--   ((SELECT id FROM users WHERE username='alice'), (SELECT p.id FROM posts p WHERE p.text LIKE 'Bob here.%'))
-- ON CONFLICT DO NOTHING;


INSERT INTO post_likes (user_id, post_id)
VALUES
  (
    (SELECT id FROM users WHERE username = 'bob'),
    (SELECT p.id
     FROM posts p
     WHERE p.text = 'Hello, this is Alice''s first post!'
     ORDER BY p.id
     LIMIT 1)
  ),
  (
    (SELECT id FROM users WHERE username = 'carol'),
    (SELECT p.id
     FROM posts p
     WHERE p.text = 'Hello, this is Alice''s first post!'
     ORDER BY p.id
     LIMIT 1)
  ),
  (
    (SELECT id FROM users WHERE username = 'alice'),
    (SELECT p.id
     FROM posts p
     WHERE p.text = 'Bob here. Loving Spring Boot!'
     ORDER BY p.id
     LIMIT 1)
  )
ON CONFLICT DO NOTHING;
