ALTER TABLE accounts ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE accounts ADD COLUMN google_subject VARCHAR(255);
CREATE UNIQUE INDEX uq_accounts_google_subject ON accounts(google_subject) WHERE google_subject IS NOT NULL;
