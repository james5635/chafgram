-- FR-5.2: video thumbnails. The pipeline generates a JPG per processed video;
-- store its key so clients can render it. Additive; reversible by dropping the column.
ALTER TABLE media_assets
  ADD COLUMN thumbnail_key VARCHAR(512) NULL AFTER blurhash;
