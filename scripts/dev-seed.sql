-- Chafgram dev fixtures — applied by `just seed` after a fresh Flyway migrate.
-- Dev-only. Resets everything with `just seed`. Never ship to real deployments.
--
-- Fixture summary (all tables seeded except `flyway_schema_history`,
-- `user_entities` and `user_credentials` — the latter two are passkey tables
-- and cannot be meaningfully faked: register passkeys from the UI instead):
--
--   * workspace_settings: default "Chafgram" row (from Flyway)
--   * users: admin (admin), maya, tao, lena — all password `chafgram-dev-password`
--   * invites: one reusable demo invite, raw token `demo-invite-token` (/join?token=demo-invite-token)
--   * media_assets: avatars for all members, post images, one short video (30s), one watch-length video (75s)
--   * follows graph, posts (note/photo/video + one tombstoned), comments (incl. a reply), reactions
--   * saved_posts, user_blocks, conversations (2 direct + 1 group), messages (text + one image), message_media
--   * notifications (follow/like/comment/reply/mention/message/system), per-type mute for one member
--   * reports: one open report + one actioned report, audit_log entries (+ tombstone record)
--   * account_tokens: one consumed email-verification token
--
-- Media rows reference fake `demo/...` storage keys — there are no blobs on disk.
-- Images/videos will 404 until real uploads happen through the app.

-- --------------------------------------------------------------------------
-- 1. Users (with bio and last-seen presence)
-- --------------------------------------------------------------------------
INSERT INTO users (username, display_name, email, password_hash, role, status, bio, last_seen_at)
VALUES
  ('admin', 'Dev Admin', 'admin@chafgram.test', '$2a$10$zwvlkrZ08TH9Ce.b1.LuBuI/g4/SqUqcqhIk9C6t8kGrJE7gtQxT.', 'admin',  'active', 'Runs this workspace.', DATE_SUB(NOW(6), INTERVAL 5 MINUTE)),
  ('maya',  'Maya',      'maya@chafgram.test',  '$2a$10$zwvlkrZ08TH9Ce.b1.LuBuI/g4/SqUqcqhIk9C6t8kGrJE7gtQxT.', 'member', 'active', 'Product engineer. Coffee first. ☕', DATE_SUB(NOW(6), INTERVAL 2 MINUTE)),
  ('tao',   'Tao',       'tao@chafgram.test',   '$2a$10$zwvlkrZ08TH9Ce.b1.LuBuI/g4/SqUqcqhIk9C6t8kGrJE7gtQxT.', 'member', 'active', 'Likes long walks and short videos.', DATE_SUB(NOW(6), INTERVAL 13 MINUTE)),
  ('lena',  'Lena',      'lena@chafgram.test',  '$2a$10$zwvlkrZ08TH9Ce.b1.LuBuI/g4/SqUqcqhIk9C6t8kGrJE7gtQxT.', 'member', 'active', 'Ops. Keeper of the calendar.', DATE_SUB(NOW(6), INTERVAL 1 HOUR));

-- --------------------------------------------------------------------------
-- 2. Media assets (avatars + post/message media) — status 'ready' so the
--    feed and watch queries surface them
-- --------------------------------------------------------------------------
INSERT INTO media_assets (owner_user_id, kind, status, storage_key, mime_type, width, height, duration_ms, size_bytes, blurhash)
VALUES
  ((SELECT id FROM users WHERE username='admin'), 'image', 'ready', 'demo/avatars/admin.png', 'image/png', 256, 256, NULL, 48213, 'LEHV6nWB2yk8pyo0adR*.7kCMdnj'),
  ((SELECT id FROM users WHERE username='admin'), 'video', 'ready', 'demo/videos/all-hands-q3.mp4', 'video/mp4', 1280, 720, 75000, 18432000, 'LEHV6nWB2yk8pyo0adR*.7kCMdnj'),
  ((SELECT id FROM users WHERE username='maya'),  'image', 'ready', 'demo/avatars/maya.png',  'image/png', 256, 256, NULL, 51102, 'LEHV6nWB2yk8pyo0adR*.7kCMdnj'),
  ((SELECT id FROM users WHERE username='maya'),  'image', 'ready', 'demo/posts/tea-board.png', 'image/png', 1600, 900, NULL, 834400, 'LEHV6nWB2yk8pyo0adR*.7kCMdnj'),
  ((SELECT id FROM users WHERE username='tao'),   'image', 'ready', 'demo/avatars/tao.png',   'image/png', 256, 256, NULL, 44721, 'LEHV6nWB2yk8pyo0adR*.7kCMdnj'),
  ((SELECT id FROM users WHERE username='tao'),   'video', 'ready', 'demo/shorts/walkthrough.mp4', 'video/mp4', 720, 1280, 30000, 6214400, 'LEHV6nWB2yk8pyo0adR*.7kCMdnj'),
  ((SELECT id FROM users WHERE username='lena'),  'image', 'ready', 'demo/avatars/lena.png',  'image/png', 256, 256, NULL, 42888, 'LEHV6nWB2yk8pyo0adR*.7kCMdnj'),
  ((SELECT id FROM users WHERE username='lena'),  'image', 'ready', 'demo/posts/offsite-agenda.png', 'image/png', 1600, 900, NULL, 902144, 'LEHV6nWB2yk8pyo0adR*.7kCMdnj'),
  ((SELECT id FROM users WHERE username='maya'),  'image', 'ready', 'demo/messages/whiteboard.png', 'image/png', 1600, 1200, NULL, 733184, 'LEHV6nWB2yk8pyo0adR*.7kCMdnj');

UPDATE users SET avatar_media_id = (SELECT id FROM media_assets WHERE storage_key='demo/avatars/admin.png') WHERE username='admin';
UPDATE users SET avatar_media_id = (SELECT id FROM media_assets WHERE storage_key='demo/avatars/maya.png')  WHERE username='maya';
UPDATE users SET avatar_media_id = (SELECT id FROM media_assets WHERE storage_key='demo/avatars/tao.png')   WHERE username='tao';
UPDATE users SET avatar_media_id = (SELECT id FROM media_assets WHERE storage_key='demo/avatars/lena.png')  WHERE username='lena';

-- --------------------------------------------------------------------------
-- 3. Social graph
-- --------------------------------------------------------------------------
INSERT INTO follows (follower_id, followee_id)
VALUES
  ((SELECT id FROM users WHERE username='maya'),  (SELECT id FROM users WHERE username='admin')),
  ((SELECT id FROM users WHERE username='tao'),   (SELECT id FROM users WHERE username='maya')),
  ((SELECT id FROM users WHERE username='lena'),  (SELECT id FROM users WHERE username='admin')),
  ((SELECT id FROM users WHERE username='admin'), (SELECT id FROM users WHERE username='maya')),
  ((SELECT id FROM users WHERE username='maya'),  (SELECT id FROM users WHERE username='tao')),
  ((SELECT id FROM users WHERE username='lena'),  (SELECT id FROM users WHERE username='maya'));

INSERT INTO user_blocks (blocker_id, blocked_id)
VALUES ((SELECT id FROM users WHERE username='lena'), (SELECT id FROM users WHERE username='tao'));

-- --------------------------------------------------------------------------
-- 4. Posts (note / photo / video + one tombstoned by a moderator)
-- --------------------------------------------------------------------------
INSERT INTO posts (author_id, type, body, created_at, updated_at)
VALUES
  ((SELECT id FROM users WHERE username='admin'), 'note',  'Welcome to the private Chafgram workspace! Everything here stays inside the company.', DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
  ((SELECT id FROM users WHERE username='maya'),  'photo', 'Whiteboard from sprint planning — we are shipping the feed next sprint.', DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
  ((SELECT id FROM users WHERE username='tao'),   'video', '30-second walkthrough of the new onboarding flow.', DATE_SUB(NOW(6), INTERVAL 20 HOUR), DATE_SUB(NOW(6), INTERVAL 20 HOUR)),
  ((SELECT id FROM users WHERE username='admin'), 'video', 'Q3 all-hands recording — worth the watch, especially the roadmap section.', DATE_SUB(NOW(6), INTERVAL 10 HOUR), DATE_SUB(NOW(6), INTERVAL 10 HOUR)),
  ((SELECT id FROM users WHERE username='lena'),  'photo', 'Offsite agenda draft — comments welcome before Friday.', DATE_SUB(NOW(6), INTERVAL 5 HOUR), DATE_SUB(NOW(6), INTERVAL 5 HOUR)),
  ((SELECT id FROM users WHERE username='maya'),  'note',  'Teaser: dark mode lands in the next build.', DATE_SUB(NOW(6), INTERVAL 2 HOUR), DATE_SUB(NOW(6), INTERVAL 2 HOUR)),
  ((SELECT id FROM users WHERE username='tao'),   'note',  '[removed by moderator]', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY));

UPDATE posts SET deleted_at = DATE_SUB(NOW(6), INTERVAL 3 DAY)
WHERE body = '[removed by moderator]';

INSERT INTO post_media (post_id, media_id, position)
VALUES
  ((SELECT id FROM posts WHERE body LIKE 'Whiteboard from sprint%'), (SELECT id FROM media_assets WHERE storage_key='demo/posts/tea-board.png'), 0),
  ((SELECT id FROM posts WHERE body LIKE '30-second walkthrough%'),  (SELECT id FROM media_assets WHERE storage_key='demo/shorts/walkthrough.mp4'), 0),
  ((SELECT id FROM posts WHERE body LIKE 'Q3 all-hands%'),           (SELECT id FROM media_assets WHERE storage_key='demo/videos/all-hands-q3.mp4'), 0),
  ((SELECT id FROM posts WHERE body LIKE 'Offsite agenda%'),         (SELECT id FROM media_assets WHERE storage_key='demo/posts/offsite-agenda.png'), 0);

-- --------------------------------------------------------------------------
-- 5. Comments (one reply, one on the tombstoned thread kept for history)
-- --------------------------------------------------------------------------
-- parent inserted first; its id is captured in a session variable (a VALUES
-- subquery cannot reference the table being inserted into)
INSERT INTO comments (post_id, author_id, parent_id, body, created_at)
VALUES
  ((SELECT id FROM posts WHERE body LIKE 'Welcome to the%'), (SELECT id FROM users WHERE username='maya'), NULL, 'Glad to be here!', DATE_SUB(NOW(6), INTERVAL 47 HOUR));
SET @cmt_glad := LAST_INSERT_ID();

INSERT INTO comments (post_id, author_id, parent_id, body, created_at)
VALUES
  ((SELECT id FROM posts WHERE body LIKE 'Welcome to the%'), (SELECT id FROM users WHERE username='tao'),  @cmt_glad, 'Same! Passkeys everywhere.', DATE_SUB(NOW(6), INTERVAL 46 HOUR)),
  ((SELECT id FROM posts WHERE body LIKE 'Whiteboard%'),     (SELECT id FROM users WHERE username='lena'), NULL, 'Nice — the feed order looks great.', DATE_SUB(NOW(6), INTERVAL 20 HOUR)),
  ((SELECT id FROM posts WHERE body LIKE 'Teaser%'),         (SELECT id FROM users WHERE username='admin'), NULL, 'Shipping it soon.', DATE_SUB(NOW(6), INTERVAL 90 MINUTE)),
  ((SELECT id FROM posts WHERE body LIKE '[removed%'),       (SELECT id FROM users WHERE username='maya'), NULL, 'Why is this one removed?', DATE_SUB(NOW(6), INTERVAL 3 DAY));

-- --------------------------------------------------------------------------
-- 6. Reactions + saved posts
-- --------------------------------------------------------------------------
INSERT INTO post_reactions (post_id, user_id, type)
VALUES
  ((SELECT id FROM posts WHERE body LIKE 'Welcome to the%'), (SELECT id FROM users WHERE username='maya'), 'love'),
  ((SELECT id FROM posts WHERE body LIKE 'Welcome to the%'), (SELECT id FROM users WHERE username='tao'),  'like'),
  ((SELECT id FROM posts WHERE body LIKE 'Welcome to the%'), (SELECT id FROM users WHERE username='lena'), 'like'),
  ((SELECT id FROM posts WHERE body LIKE 'Whiteboard%'),     (SELECT id FROM users WHERE username='tao'),  'love'),
  ((SELECT id FROM posts WHERE body LIKE 'Whiteboard%'),     (SELECT id FROM users WHERE username='admin'),'like'),
  ((SELECT id FROM posts WHERE body LIKE '30-second%'),      (SELECT id FROM users WHERE username='maya'), 'wow'),
  ((SELECT id FROM posts WHERE body LIKE 'Q3 all-hands%'),   (SELECT id FROM users WHERE username='maya'), 'like'),
  ((SELECT id FROM posts WHERE body LIKE 'Teaser%'),         (SELECT id FROM users WHERE username='tao'),  'like');

INSERT INTO comment_reactions (comment_id, user_id, type)
VALUES
  ((SELECT id FROM comments WHERE body='Glad to be here!'),       (SELECT id FROM users WHERE username='admin'), 'love'),
  ((SELECT id FROM comments WHERE body='Same! Passkeys everywhere.'), (SELECT id FROM users WHERE username='maya'), 'haha');

INSERT INTO saved_posts (user_id, post_id)
VALUES
  ((SELECT id FROM users WHERE username='maya'), (SELECT id FROM posts WHERE body LIKE 'Q3 all-hands%')),
  ((SELECT id FROM users WHERE username='tao'),  (SELECT id FROM posts WHERE body LIKE 'Whiteboard%'));

-- --------------------------------------------------------------------------
-- 7. Direct messages + one group conversation
-- --------------------------------------------------------------------------
-- conversations reference each other via session variables — deterministic,
-- never depends on insert order or auto-increment values
INSERT INTO conversations (type, title, created_by, created_at, updated_at)
VALUES ('direct', NULL, (SELECT id FROM users WHERE username='admin'), DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 15 MINUTE));
SET @conv_admin_maya := LAST_INSERT_ID();

INSERT INTO conversations (type, title, created_by, created_at, updated_at)
VALUES ('direct', NULL, (SELECT id FROM users WHERE username='maya'), DATE_SUB(NOW(6), INTERVAL 40 HOUR), DATE_SUB(NOW(6), INTERVAL 25 MINUTE));
SET @conv_maya_tao := LAST_INSERT_ID();

INSERT INTO conversations (type, title, created_by, created_at, updated_at)
VALUES ('group', 'Weekend plans', (SELECT id FROM users WHERE username='admin'), DATE_SUB(NOW(6), INTERVAL 12 HOUR), DATE_SUB(NOW(6), INTERVAL 10 MINUTE));
SET @conv_group := LAST_INSERT_ID();

INSERT INTO conversation_members (conversation_id, user_id, role, joined_at, last_read_message_id, muted_until)
VALUES
  (@conv_admin_maya, (SELECT id FROM users WHERE username='admin'), 'member', DATE_SUB(NOW(6), INTERVAL 1 DAY), NULL, NULL),
  (@conv_admin_maya, (SELECT id FROM users WHERE username='maya'),  'member', DATE_SUB(NOW(6), INTERVAL 1 DAY), NULL, NULL),
  (@conv_maya_tao,   (SELECT id FROM users WHERE username='maya'),  'member', DATE_SUB(NOW(6), INTERVAL 40 HOUR), NULL, NULL),
  (@conv_maya_tao,   (SELECT id FROM users WHERE username='tao'),   'member', DATE_SUB(NOW(6), INTERVAL 40 HOUR), NULL, DATE_ADD(NOW(6), INTERVAL 1 DAY)),
  (@conv_group,      (SELECT id FROM users WHERE username='admin'), 'admin',  DATE_SUB(NOW(6), INTERVAL 12 HOUR), NULL, NULL),
  (@conv_group,      (SELECT id FROM users WHERE username='maya'),  'member', DATE_SUB(NOW(6), INTERVAL 12 HOUR), NULL, NULL),
  (@conv_group,      (SELECT id FROM users WHERE username='lena'),  'member', DATE_SUB(NOW(6), INTERVAL 12 HOUR), NULL, NULL);

INSERT INTO messages (conversation_id, sender_id, type, body, created_at)
VALUES
  (@conv_admin_maya, (SELECT id FROM users WHERE username='admin'), 'text', 'Welcome aboard, Maya!', DATE_SUB(NOW(6), INTERVAL 23 HOUR)),
  (@conv_admin_maya, (SELECT id FROM users WHERE username='maya'),  'text', 'Thanks! The passkey flow is slick.', DATE_SUB(NOW(6), INTERVAL 22 HOUR)),
  (@conv_admin_maya, (SELECT id FROM users WHERE username='admin'), 'text', 'Right? And zero passwords to forget.', DATE_SUB(NOW(6), INTERVAL 15 MINUTE)),
  (@conv_maya_tao,   (SELECT id FROM users WHERE username='maya'),  'text', 'Did you see the new watch feed?', DATE_SUB(NOW(6), INTERVAL 50 MINUTE)),
  (@conv_maya_tao,   (SELECT id FROM users WHERE username='tao'),   'text', 'Yeah — the walkthrough video is great.', DATE_SUB(NOW(6), INTERVAL 25 MINUTE)),
  (@conv_group,      (SELECT id FROM users WHERE username='admin'), 'text', 'Team: offsite agenda is in the feed, please skim it.', DATE_SUB(NOW(6), INTERVAL 55 MINUTE)),
  (@conv_group,      (SELECT id FROM users WHERE username='lena'),  'text', 'On it. Bringing the room bookings.', DATE_SUB(NOW(6), INTERVAL 40 MINUTE)),
  (@conv_group,      (SELECT id FROM users WHERE username='maya'),  'text', 'Shared the whiteboard too:', DATE_SUB(NOW(6), INTERVAL 10 MINUTE));

INSERT INTO message_media (message_id, media_id, position)
VALUES ((SELECT id FROM messages WHERE body='Shared the whiteboard too:'), (SELECT id FROM media_assets WHERE storage_key='demo/messages/whiteboard.png'), 0);

-- unread markers: maya has read everything with admin; tao only the first message in the other direct chat;
-- admin hasn't read Maya's latest message in the group
UPDATE conversation_members m
SET last_read_message_id = (SELECT MAX(id) FROM messages WHERE conversation_id = m.conversation_id)
WHERE m.conversation_id = @conv_admin_maya
  AND m.user_id = (SELECT id FROM users WHERE username='maya');
UPDATE conversation_members m
SET last_read_message_id = (SELECT MIN(id) FROM messages WHERE conversation_id = m.conversation_id)
WHERE m.conversation_id = @conv_maya_tao
  AND m.user_id = (SELECT id FROM users WHERE username='tao');
UPDATE conversation_members m
SET last_read_message_id = (SELECT MAX(id) FROM messages WHERE conversation_id = m.conversation_id) - 1
WHERE m.conversation_id = @conv_group
  AND m.user_id = (SELECT id FROM users WHERE username='admin');

-- --------------------------------------------------------------------------
-- 8. Notifications + per-member type mute
-- --------------------------------------------------------------------------
INSERT INTO notifications (recipient_id, actor_id, type, entity_type, entity_id, created_at)
VALUES
  ((SELECT id FROM users WHERE username='admin'), (SELECT id FROM users WHERE username='tao'),  'follow',  'user', (SELECT id FROM users WHERE username='tao'),  DATE_SUB(NOW(6), INTERVAL 30 HOUR)),
  ((SELECT id FROM users WHERE username='admin'), (SELECT id FROM users WHERE username='maya'), 'like',    'post', (SELECT id FROM posts WHERE body LIKE 'Welcome to the%'), DATE_SUB(NOW(6), INTERVAL 47 HOUR)),
  ((SELECT id FROM users WHERE username='admin'), (SELECT id FROM users WHERE username='tao'),  'comment', 'post', (SELECT id FROM posts WHERE body LIKE 'Welcome to the%'), DATE_SUB(NOW(6), INTERVAL 46 HOUR)),
  ((SELECT id FROM users WHERE username='tao'),   (SELECT id FROM users WHERE username='maya'), 'mention', 'comment', (SELECT id FROM comments WHERE body='Same! Passkeys everywhere.'), DATE_SUB(NOW(6), INTERVAL 46 HOUR)),
  ((SELECT id FROM users WHERE username='maya'),  (SELECT id FROM users WHERE username='admin'),'reply',   'comment', (SELECT id FROM comments WHERE body='Same! Passkeys everywhere.'), DATE_SUB(NOW(6), INTERVAL 46 HOUR)),
  ((SELECT id FROM users WHERE username='lena'),  (SELECT id FROM users WHERE username='admin'),'message', 'message', (SELECT id FROM messages WHERE body LIKE 'Team: offsite%'), DATE_SUB(NOW(6), INTERVAL 55 MINUTE)),
  ((SELECT id FROM users WHERE username='admin'), NULL,                                        'system',  NULL, NULL, DATE_SUB(NOW(6), INTERVAL 2 DAY));

-- maya muted like-notifications
INSERT INTO notification_preferences (user_id, type)
VALUES ((SELECT id FROM users WHERE username='maya'), 'like');

-- --------------------------------------------------------------------------
-- 9. Reports (one open, one actioned) + audit log (+ tombstone record)
-- --------------------------------------------------------------------------
INSERT INTO reports (reporter_id, target_type, target_id, reason, details, status, resolved_by, resolved_at, created_at)
VALUES
  ((SELECT id FROM users WHERE username='maya'), 'post', (SELECT id FROM posts WHERE body LIKE 'Offsite agenda%'), 'spam', 'Looks like accidental cross-posting?', 'open', NULL, NULL, DATE_SUB(NOW(6), INTERVAL 2 HOUR)),
  ((SELECT id FROM users WHERE username='tao'),  'post', (SELECT id FROM posts WHERE body LIKE '[removed%'),       'harassment', 'Dismissive reply, please review.', 'actioned', (SELECT id FROM users WHERE username='admin'), DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY));

INSERT INTO audit_log (actor_id, action, target_type, target_id, metadata, created_at)
VALUES
  ((SELECT id FROM users WHERE username='admin'), 'report.resolve', 'post', (SELECT id FROM posts WHERE body LIKE '[removed%'), JSON_OBJECT('reason', 'harassment', 'resolution', 'post tombstoned'), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
  ((SELECT id FROM users WHERE username='admin'), 'post.delete',    'post', (SELECT id FROM posts WHERE body LIKE '[removed%'), JSON_OBJECT('tombstone', true), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
  ((SELECT id FROM users WHERE username='admin'), 'workspace.update', 'workspace', 1, JSON_OBJECT('field', 'name', 'value', 'Chafgram'), DATE_SUB(NOW(6), INTERVAL 3 DAY));

-- --------------------------------------------------------------------------
-- 10. A consumed email-verification token (fake hash; demo of the pattern)
-- --------------------------------------------------------------------------
INSERT INTO account_tokens (user_id, type, token_hash, expires_at, used_at, created_at)
VALUES ((SELECT id FROM users WHERE username='admin'), 'verify_email', SHA2('demo-verify-token', 256), DATE_SUB(NOW(6), INTERVAL 40 HOUR), DATE_SUB(NOW(6), INTERVAL 48 HOUR), DATE_SUB(NOW(6), INTERVAL 48 HOUR));

-- --------------------------------------------------------------------------
-- 11. Invites (one reusable demo invite + one email-bound example)
-- --------------------------------------------------------------------------
INSERT INTO invites (email, token_hash, role, created_by, expires_at)
VALUES
  (NULL, '3aa030fe3ebc63be729f46fc6282b1cf4e85abe555eb1acda2a9b895a2fb70e8', 'member', NULL, DATE_ADD(NOW(6), INTERVAL 30 DAY)),
  ('new-member@company.example', SHA2('demo-invite-bound', 256), 'member', (SELECT id FROM users WHERE username='admin'), DATE_ADD(NOW(6), INTERVAL 7 DAY));
