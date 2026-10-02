-- Migration: 20260917_03_key_limits_and_indexes.sql
-- Description: Enforces maximum 3 active API keys per user and adds performance indexes for concurrent queries

-- 1. Trigger function to enforce active API key quota (max 3)
CREATE OR REPLACE FUNCTION check_active_api_keys_limit()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  active_count int;
BEGIN
  SELECT count(*) INTO active_count
  FROM public.api_keys
  WHERE user_id = NEW.user_id AND revoked_at IS NULL;

  IF active_count >= 3 THEN
    RAISE EXCEPTION 'KEY_LIMIT_REACHED: Maximum 3 active API keys allowed per account. Please revoke an existing key first.';
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_check_active_api_keys_limit ON public.api_keys;
CREATE TRIGGER tr_check_active_api_keys_limit
  BEFORE INSERT ON public.api_keys
  FOR EACH ROW
  EXECUTE FUNCTION check_active_api_keys_limit();

-- 2. Performance indexes for concurrent agent queries
CREATE INDEX IF NOT EXISTS idx_notes_user_timestamp ON public.notes (user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_notes_user_updated ON public.notes (user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_action_items_user_completed ON public.action_items (user_id, is_completed);
CREATE INDEX IF NOT EXISTS idx_api_keys_user_active ON public.api_keys (user_id) WHERE revoked_at IS NULL;
