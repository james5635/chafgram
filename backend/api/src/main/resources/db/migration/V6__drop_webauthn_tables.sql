-- Passkeys/WebAuthn are removed with the stateless token-auth cutover.
-- user_credentials has a FK to user_entities, so drop it first.
-- Forward-only; to restore, re-create both tables from their V1 definitions.
DROP TABLE IF EXISTS user_credentials;
DROP TABLE IF EXISTS user_entities;
