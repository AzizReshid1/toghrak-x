-- Store every timestamp as an absolute moment (TIMESTAMPTZ) instead of a local wall-clock time.
-- The entities now use java.time.Instant, so times are the same whether the server runs in UTC (AWS)
-- or in Europe/Amsterdam (development laptop).
--
-- Rows written before this migration hold Europe/Amsterdam local time (the JVM default zone they were
-- created in), so the conversion interprets them in that zone. On an empty database nothing is converted.

ALTER TABLE users
    ALTER COLUMN created_at TYPE TIMESTAMPTZ(6) USING created_at AT TIME ZONE 'Europe/Amsterdam',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ(6) USING updated_at AT TIME ZONE 'Europe/Amsterdam';

ALTER TABLE posts
    ALTER COLUMN created_at TYPE TIMESTAMPTZ(6) USING created_at AT TIME ZONE 'Europe/Amsterdam',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ(6) USING updated_at AT TIME ZONE 'Europe/Amsterdam';

ALTER TABLE pages
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ(6) USING updated_at AT TIME ZONE 'Europe/Amsterdam';

ALTER TABLE refresh_tokens
    ALTER COLUMN expires_at TYPE TIMESTAMPTZ(6) USING expires_at AT TIME ZONE 'Europe/Amsterdam';
