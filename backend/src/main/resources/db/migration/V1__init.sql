-- Chafgram — V1 baseline schema
-- MySQL 8.0 / InnoDB / utf8mb4. See docs/db-schema.md for rationale.
-- Forward-only: future changes go in V2__…, V3__…

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. Core identity (users first WITHOUT avatar FK — media table comes later)
-- ---------------------------------------------------------------------------
CREATE TABLE users (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  username        VARCHAR(30)  NOT NULL,
  display_name    VARCHAR(80)  NOT NULL,
  email           VARCHAR(254) NOT NULL,
  password_hash   VARCHAR(255) NULL,
  bio             VARCHAR(160) NULL,
  avatar_media_id BIGINT       NULL,
  role            ENUM('member','moderator','admin') NOT NULL DEFAULT 'member',
  status          ENUM('active','suspended','deactivated') NOT NULL DEFAULT 'active',
  last_seen_at    DATETIME(6)  NULL,
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_users_username UNIQUE (username),
  CONSTRAINT uk_users_email    UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE account_tokens (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  user_id    BIGINT      NOT NULL,
  type       ENUM('verify_email','reset_password') NOT NULL,
  token_hash CHAR(64)    NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  used_at    DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_account_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_account_tokens_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Managed by Spring Security WebAuthn (JdbcPublicKeyCredentialUserEntityRepository).
-- `name` carries users.username. Not mapped to JPA entities on purpose.
CREATE TABLE user_entities (
  id           VARCHAR(512) NOT NULL PRIMARY KEY,
  name         VARCHAR(100)  NOT NULL,
  display_name VARCHAR(200),
  CONSTRAINT uk_user_entities_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Managed by Spring Security WebAuthn (JdbcUserCredentialRepository).
CREATE TABLE user_credentials (
  credential_id                VARCHAR(512) NOT NULL PRIMARY KEY,
  user_entity_user_id          VARCHAR(512) NOT NULL,
  public_key                   BLOB          NOT NULL,
  signature_count              BIGINT,
  uv_initialized               BOOLEAN,
  backup_eligible              BOOLEAN       NOT NULL,
  authenticator_transports     VARCHAR(512),
  public_key_credential_type   VARCHAR(100),
  backup_state                 BOOLEAN       NOT NULL,
  attestation_object           BLOB,
  attestation_client_data_json BLOB,
  created                      TIMESTAMP,
  last_used                    TIMESTAMP,
  label                        VARCHAR(512) NOT NULL,
  INDEX idx_user_credentials_entity (user_entity_user_id),
  CONSTRAINT fk_user_credentials_entity FOREIGN KEY (user_entity_user_id) REFERENCES user_entities (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE invites (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  email      VARCHAR(254) NULL,
  token_hash CHAR(64)    NOT NULL,
  role       ENUM('member','moderator','admin') NOT NULL DEFAULT 'member',
  created_by BIGINT      NULL,
  expires_at DATETIME(6) NOT NULL,
  accepted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_invites_token_hash UNIQUE (token_hash),
  CONSTRAINT fk_invites_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 2. Media pipeline
-- ---------------------------------------------------------------------------
CREATE TABLE media_assets (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  owner_user_id BIGINT        NOT NULL,
  kind          ENUM('image','video') NOT NULL,
  status        ENUM('processing','ready','failed','deleted') NOT NULL DEFAULT 'processing',
  storage_key   VARCHAR(512)  NOT NULL,
  mime_type     VARCHAR(127)  NOT NULL,
  width         INT UNSIGNED  NULL,
  height        INT UNSIGNED  NULL,
  duration_ms   INT UNSIGNED  NULL,
  size_bytes    BIGINT UNSIGNED NOT NULL,
  blurhash      VARCHAR(64)   NULL,
  created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_media_owner FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_media_status_created (status, created_at),
  INDEX idx_media_owner (owner_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE users
  ADD CONSTRAINT fk_users_avatar FOREIGN KEY (avatar_media_id) REFERENCES media_assets (id) ON DELETE SET NULL;

-- ---------------------------------------------------------------------------
-- 3. Social graph & feed
-- ---------------------------------------------------------------------------
CREATE TABLE follows (
  follower_id BIGINT     NOT NULL,
  followee_id BIGINT     NOT NULL,
  created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (follower_id, followee_id),
  CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_follows_followee FOREIGN KEY (followee_id) REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_follows_followee (followee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE posts (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  author_id  BIGINT      NOT NULL,
  type       ENUM('note','photo','video') NOT NULL DEFAULT 'note',
  body       TEXT        NULL,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_posts_author_created (author_id, created_at),
  INDEX idx_posts_created (created_at),
  FULLTEXT INDEX ft_posts_body (body)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE post_media (
  post_id  BIGINT   NOT NULL,
  media_id BIGINT   NOT NULL,
  position TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (post_id, media_id),
  CONSTRAINT fk_post_media_post  FOREIGN KEY (post_id)  REFERENCES posts (id) ON DELETE CASCADE,
  CONSTRAINT fk_post_media_asset FOREIGN KEY (media_id) REFERENCES media_assets (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comments (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  post_id    BIGINT      NOT NULL,
  author_id  BIGINT      NOT NULL,
  parent_id  BIGINT      NULL,
  body       TEXT        NOT NULL,
  edited_at  DATETIME(6) NULL,
  deleted_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_comments_post   FOREIGN KEY (post_id)   REFERENCES posts (id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id) ON DELETE CASCADE,
  INDEX idx_comments_post_created (post_id, created_at),
  INDEX idx_comments_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE post_reactions (
  post_id    BIGINT      NOT NULL,
  user_id    BIGINT      NOT NULL,
  type       ENUM('like','love','haha','wow','sad','angry') NOT NULL DEFAULT 'like',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (post_id, user_id),
  CONSTRAINT fk_post_reactions_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
  CONSTRAINT fk_post_reactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_post_reactions_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comment_reactions (
  comment_id BIGINT      NOT NULL,
  user_id    BIGINT      NOT NULL,
  type       ENUM('like','love','haha','wow','sad','angry') NOT NULL DEFAULT 'like',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (comment_id, user_id),
  CONSTRAINT fk_comment_reactions_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
  CONSTRAINT fk_comment_reactions_user    FOREIGN KEY (user_id)    REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_comment_reactions_comment (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE saved_posts (
  user_id    BIGINT      NOT NULL,
  post_id    BIGINT      NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id, post_id),
  CONSTRAINT fk_saved_posts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_saved_posts_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_blocks (
  blocker_id BIGINT      NOT NULL,
  blocked_id BIGINT      NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (blocker_id, blocked_id),
  CONSTRAINT fk_blocks_blocker FOREIGN KEY (blocker_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_blocks_blocked FOREIGN KEY (blocked_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 4. Messaging
-- ---------------------------------------------------------------------------
CREATE TABLE conversations (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  type       ENUM('direct','group') NOT NULL DEFAULT 'direct',
  title      VARCHAR(80) NULL,
  created_by BIGINT      NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_conversations_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
  INDEX idx_conversations_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE conversation_members (
  conversation_id      BIGINT      NOT NULL,
  user_id              BIGINT      NOT NULL,
  role                 ENUM('member','admin') NOT NULL DEFAULT 'member',
  joined_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  left_at              DATETIME(6) NULL,
  last_read_message_id BIGINT      NULL,  -- plain id, FK would be circular; enforced at app layer
  muted_until          DATETIME(6) NULL,
  PRIMARY KEY (conversation_id, user_id),
  CONSTRAINT fk_conv_members_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
  CONSTRAINT fk_conv_members_user         FOREIGN KEY (user_id)         REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_conv_members_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE messages (
  id              BIGINT      NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT      NOT NULL,
  sender_id       BIGINT      NULL,
  type            ENUM('text','image','video','file','system') NOT NULL DEFAULT 'text',
  body            TEXT        NULL,
  edited_at       DATETIME(6) NULL,
  deleted_at      DATETIME(6) NULL,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
  CONSTRAINT fk_messages_sender       FOREIGN KEY (sender_id)       REFERENCES users (id) ON DELETE SET NULL,
  INDEX idx_messages_conversation_id (conversation_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE message_media (
  message_id BIGINT   NOT NULL,
  media_id   BIGINT   NOT NULL,
  position   TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (message_id, media_id),
  CONSTRAINT fk_message_media_message FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE,
  CONSTRAINT fk_message_media_asset   FOREIGN KEY (media_id)   REFERENCES media_assets (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 5. Notifications (polymorphic read model — no FKs to targets by design)
-- ---------------------------------------------------------------------------
CREATE TABLE notifications (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  recipient_id BIGINT     NOT NULL,
  actor_id    BIGINT      NULL,
  type        ENUM('follow','like','comment','reply','mention','message','system') NOT NULL,
  entity_type ENUM('post','comment','message','user') NULL,
  entity_id   BIGINT      NULL,
  read_at     DATETIME(6) NULL,
  created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_notifications_actor     FOREIGN KEY (actor_id)     REFERENCES users (id) ON DELETE SET NULL,
  INDEX idx_notifications_recipient_unread (recipient_id, read_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Per-member mute of notification types (plans.md FR-7.3).
-- Presence of a row = that type is muted for the member; absence = enabled.
CREATE TABLE notification_preferences (
  user_id    BIGINT    NOT NULL,
  type       ENUM('follow','like','comment','reply','mention','message','system') NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id, type),
  CONSTRAINT fk_notification_prefs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 6. Moderation, reports & audit
-- ---------------------------------------------------------------------------
CREATE TABLE reports (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  reporter_id BIGINT      NULL,
  target_type ENUM('post','comment','message','user') NOT NULL,
  target_id   BIGINT      NOT NULL,
  reason      ENUM('spam','harassment','nsfw','violence','impersonation','other') NOT NULL,
  details     TEXT        NULL,
  status      ENUM('open','reviewing','actioned','dismissed') NOT NULL DEFAULT 'open',
  resolved_by BIGINT      NULL,
  resolved_at DATETIME(6) NULL,
  created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_reports_reporter    FOREIGN KEY (reporter_id) REFERENCES users (id) ON DELETE SET NULL,
  CONSTRAINT fk_reports_resolver    FOREIGN KEY (resolved_by) REFERENCES users (id) ON DELETE SET NULL,
  INDEX idx_reports_status_created (status, created_at),
  INDEX idx_reports_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE audit_log (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  actor_id    BIGINT      NULL,
  action      VARCHAR(100) NOT NULL,
  target_type VARCHAR(64) NULL,
  target_id   BIGINT      NULL,
  metadata    JSON        NULL,
  created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL,
  INDEX idx_audit_actor_created (actor_id, created_at),
  INDEX idx_audit_target (target_type, target_id),
  INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 7. Workspace instance settings (single-tenant singleton, id = 1)
-- ---------------------------------------------------------------------------
CREATE TABLE workspace_settings (
  id            TINYINT     NOT NULL PRIMARY KEY CHECK (id = 1),
  name          VARCHAR(80) NOT NULL,
  slug          VARCHAR(80) NOT NULL,
  logo_media_id BIGINT      NULL,
  primary_color CHAR(7)     NULL,
  signup_mode   ENUM('invite_only','open') NOT NULL DEFAULT 'invite_only',
  created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT uk_workspace_slug UNIQUE (slug),
  CONSTRAINT fk_workspace_logo FOREIGN KEY (logo_media_id) REFERENCES media_assets (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO workspace_settings (id, name, slug) VALUES (1, 'Chafgram', 'chafgram');
