-- Accounts created by historical development seeds used predictable IDs.
-- Revoke their sessions and disable them before opt-in bootstrap creates new,
-- environment-supplied accounts. This migration is safe on fresh databases.
UPDATE refresh_tokens
   SET revoked_at = now()
 WHERE revoked_at IS NULL;

UPDATE users
   SET active = FALSE,
       password_hash = 'LEGACY_SEED_ACCOUNT_DISABLED',
       updated_at = now()
 WHERE (id = 1 AND role = 'ADMIN' AND full_name = 'Administrador del Sistema')
    OR (id = 2 AND role = 'RECEPCIONISTA' AND full_name = 'Recepcionista Demo');
