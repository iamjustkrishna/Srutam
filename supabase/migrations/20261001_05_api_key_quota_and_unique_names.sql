-- ==============================================================================
-- API key quota (max 3 active) and unique active names
-- File: supabase/migrations/20261001_05_api_key_quota_and_unique_names.sql
--
-- WHY: the 3-key limit lived only in the trigger created by migration 03. Migration 04
-- re-created the trigger FUNCTION but never the trigger itself, so a database where 03 was
-- missing (or the trigger dropped) had no limit at all. The app also allowed duplicate names
-- and blank names, and a rapid triple-tap on "Generate" created several keys.
--
-- This migration is idempotent and safe to re-run.
--   * (re)creates the trigger unconditionally, on INSERT and on un-revoking a key
--   * normalises existing names, renames duplicate active names, then enforces
--     uniqueness of active names per user (case/space-insensitive)
--   * requires a 1..60 character name for active keys
--   * does NOT revoke anything: accounts already above 3 keep their keys but cannot add more
-- ==============================================================================

BEGIN;

-- ------------------------------------------------------------------------------
-- 1. Quota function: counts active keys under a per-user advisory lock.
--    Runs on INSERT of an active key, and when an existing key is un-revoked.
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.check_active_api_keys_limit()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    active_count int;
BEGIN
    -- Inserting an already-revoked row, or updating a row that stays revoked/active, never adds an active key.
    IF NEW.revoked_at IS NOT NULL THEN
        RETURN NEW;
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.revoked_at IS NULL THEN
        RETURN NEW;
    END IF;

    PERFORM pg_advisory_xact_lock(hashtext(NEW.user_id::text));

    SELECT count(*) INTO active_count
    FROM public.api_keys k
    WHERE k.user_id = NEW.user_id AND k.revoked_at IS NULL;

    IF active_count >= 3 THEN
        RAISE EXCEPTION 'KEY_LIMIT_REACHED: Maximum 3 active API keys allowed per account. Please revoke an existing key first.'
            USING ERRCODE = 'P0001';
    END IF;

    RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION public.check_active_api_keys_limit() FROM PUBLIC, anon, authenticated;

DROP TRIGGER IF EXISTS tr_check_active_api_keys_limit ON public.api_keys;
CREATE TRIGGER tr_check_active_api_keys_limit
    BEFORE INSERT OR UPDATE OF revoked_at ON public.api_keys
    FOR EACH ROW
    EXECUTE FUNCTION public.check_active_api_keys_limit();

-- ------------------------------------------------------------------------------
-- 2. Normalise existing active names so the constraints below can be applied
-- ------------------------------------------------------------------------------
UPDATE public.api_keys
   SET name = left(coalesce(nullif(btrim(name), ''), 'Developer Key'), 60)
 WHERE revoked_at IS NULL
   AND (name IS DISTINCT FROM left(coalesce(nullif(btrim(name), ''), 'Developer Key'), 60));

-- Rename duplicate active names: the oldest keeps its name, later ones become "Name (2)", "Name (3)"...
-- Looped because a generated name could itself collide with an existing one.
DO $$
DECLARE
    pass int := 0;
    changed int;
BEGIN
    LOOP
        WITH ranked AS (
            SELECT id,
                   name,
                   row_number() OVER (
                       PARTITION BY user_id, lower(btrim(name))
                       ORDER BY created_at, id
                   ) AS rn
            FROM public.api_keys
            WHERE revoked_at IS NULL
        )
        UPDATE public.api_keys k
           SET name = left(btrim(r.name), 52) || ' (' || (r.rn + pass) || ')'
          FROM ranked r
         WHERE k.id = r.id AND r.rn > 1;

        GET DIAGNOSTICS changed = ROW_COUNT;
        EXIT WHEN changed = 0;
        pass := pass + 1;
        IF pass > 10 THEN
            RAISE EXCEPTION 'could not de-duplicate api key names';
        END IF;
    END LOOP;
END;
$$;

-- ------------------------------------------------------------------------------
-- 3. Constraints
-- ------------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS uq_api_keys_active_name
    ON public.api_keys (user_id, lower(btrim(name)))
    WHERE revoked_at IS NULL;

ALTER TABLE public.api_keys DROP CONSTRAINT IF EXISTS api_keys_active_name_len;
ALTER TABLE public.api_keys
    ADD CONSTRAINT api_keys_active_name_len
    CHECK (revoked_at IS NOT NULL OR char_length(btrim(name)) BETWEEN 1 AND 60);

COMMIT;
