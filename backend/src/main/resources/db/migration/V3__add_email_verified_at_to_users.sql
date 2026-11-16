-- FR-1.7: email verification state. Additive; reversible by dropping the column.
ALTER TABLE users
  ADD COLUMN email_verified_at DATETIME(6) NULL AFTER last_seen_at;
