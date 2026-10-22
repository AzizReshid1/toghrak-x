-- Emails are now stored in lower case (UserService normalises them), so existing rows are converted too.
-- A row is skipped if lower-casing it would clash with another user's email; such duplicates need a
-- manual decision, and the application's case-insensitive check still treats them as one address.
UPDATE users u
SET email = lower(u.email)
WHERE u.email <> lower(u.email)
  AND NOT EXISTS (SELECT 1 FROM users other WHERE other.id <> u.id AND lower(other.email) = lower(u.email));
