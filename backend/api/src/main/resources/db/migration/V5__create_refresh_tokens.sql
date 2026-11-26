-- Opaque refresh tokens. Only the SHA-256 hex of each raw token is stored;
-- the raw value is handed to the member once, by the auth endpoint, and never persisted.
-- Forward-only; reversible by dropping this table (no other migration depends on it).
CREATE TABLE refresh_tokens (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  token_hash  CHAR(64)     NOT NULL,
  expires_at  DATETIME(6)  NOT NULL,
  created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  revoked_at  DATETIME(6)  NULL,
  -- id of the successor row when this row was rotated. NULL on logout/theft
  -- revocation, which is what makes a later presentation count as reuse.
  replaced_by BIGINT       NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  INDEX idx_refresh_tokens_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
