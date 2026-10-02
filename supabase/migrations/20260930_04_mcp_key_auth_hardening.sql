-- ==============================================================================
-- Srutam MCP: key-bound authorization (security hardening)
-- File: supabase/migrations/20260930_04_mcp_key_auth_hardening.sql
--
-- PROBLEM (migrations 01/02): every mcp_* RPC took a caller-supplied p_user_id and was
-- SECURITY DEFINER with default EXECUTE for `anon`. The anon key ships publicly, so anyone
-- who knew a user's UUID could read that user's notes and modify their tasks. The API key
-- was only checked in a *separate* call whose result the server never re-verified.
--
-- FIX: the API-key hash is now an argument of every RPC and the owner is resolved INSIDE
-- the function, so authentication and authorization can no longer be separated.
--   * old uuid-first signatures are dropped (hard cut - srutam-mcp <= 1.2.x stops working)
--   * private.resolve_mcp_key() lives in a schema PostgREST does not expose
--   * action-item RPCs now honour notes.is_private
--   * explicit column lists (no embedding / user_id leakage), server-side limit clamps
--   * every SECURITY DEFINER function pins search_path
--   * EXECUTE is revoked from PUBLIC/anon/authenticated and granted back to anon only
--     for the new mcp_* set
-- ==============================================================================

BEGIN;

-- ------------------------------------------------------------------------------
-- 0. Drop the vulnerable signatures (hard cut)
-- ------------------------------------------------------------------------------
DROP FUNCTION IF EXISTS public.verify_srutam_api_key(text);
DROP FUNCTION IF EXISTS public.mcp_search_notes(uuid, text, int);
DROP FUNCTION IF EXISTS public.mcp_get_note_detail(uuid, uuid);
DROP FUNCTION IF EXISTS public.mcp_list_action_items(uuid, text, int);
DROP FUNCTION IF EXISTS public.mcp_update_action_item(uuid, uuid, boolean, text);
DROP FUNCTION IF EXISTS public.mcp_append_agent_log(uuid, uuid, text, text);

-- ------------------------------------------------------------------------------
-- 1. Key resolution helper (not reachable through the REST API)
-- ------------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS private;
REVOKE ALL ON SCHEMA private FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION private.resolve_mcp_key(p_key_hash text)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id uuid;
BEGIN
    -- SHA-256 hex digest only; anything else can never match a stored hash.
    IF p_key_hash IS NULL OR p_key_hash !~ '^[0-9a-f]{64}$' THEN
        RETURN NULL;
    END IF;

    SELECT k.user_id INTO v_user_id
    FROM public.api_keys k
    WHERE k.key_hash = p_key_hash
      AND k.revoked_at IS NULL;

    IF v_user_id IS NOT NULL THEN
        -- Throttled write: at most one last_used_at update per key per 5 minutes.
        UPDATE public.api_keys k
           SET last_used_at = now()
         WHERE k.key_hash = p_key_hash
           AND (k.last_used_at IS NULL OR k.last_used_at < now() - interval '5 minutes');
    END IF;

    RETURN v_user_id;
END;
$$;

REVOKE ALL ON FUNCTION private.resolve_mcp_key(text) FROM PUBLIC, anon, authenticated;

-- ------------------------------------------------------------------------------
-- 2. search / list recent notes
--    Returns duration_ms (was missing -> "NaNs" in list_recent_notes) and NO transcript
--    (never displayed by the server, and previously shipped + cached in bulk).
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.mcp_search_notes(
    p_key_hash text,
    p_query    text,
    p_limit    int DEFAULT 5
)
RETURNS TABLE (
    id              uuid,
    title           text,
    summary         text,
    key_points      jsonb,
    wiifm           text,
    duration_ms     bigint,
    "timestamp"     timestamptz,
    similarity_rank real
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id uuid;
    v_limit   int := LEAST(GREATEST(coalesce(p_limit, 5), 1), 25);
    v_pattern text;
BEGIN
    v_user_id := private.resolve_mcp_key(p_key_hash);
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'unauthorized' USING ERRCODE = '28000';
    END IF;

    IF p_query IS NULL OR trim(p_query) = '' THEN
        RETURN QUERY
        SELECT n.id, n.title, n.summary, n.key_points, n.wiifm, n.duration_ms, n."timestamp",
               1.0::real AS similarity_rank
        FROM public.notes n
        WHERE n.user_id = v_user_id
          AND n.is_private = false
        ORDER BY n."timestamp" DESC
        LIMIT v_limit;
    ELSE
        -- Escape LIKE wildcards so "%" / "_" in a query are matched literally.
        v_pattern := '%' ||
            replace(replace(replace(trim(p_query), chr(92), chr(92) || chr(92)), '%', chr(92) || '%'), '_', chr(92) || '_')
            || '%';

        RETURN QUERY
        SELECT n.id, n.title, n.summary, n.key_points, n.wiifm, n.duration_ms, n."timestamp",
               ts_rank_cd(
                   to_tsvector('english', coalesce(n.title, '') || ' ' || coalesce(n.summary, '') || ' ' || coalesce(n.transcript, '')),
                   plainto_tsquery('english', p_query)
               ) AS similarity_rank
        FROM public.notes n
        WHERE n.user_id = v_user_id
          AND n.is_private = false
          AND (
              to_tsvector('english', coalesce(n.title, '') || ' ' || coalesce(n.summary, '') || ' ' || coalesce(n.transcript, ''))
                  @@ plainto_tsquery('english', p_query)
              OR n.title ILIKE v_pattern
              OR n.summary ILIKE v_pattern
          )
        ORDER BY similarity_rank DESC, n."timestamp" DESC
        LIMIT v_limit;
    END IF;
END;
$$;

-- ------------------------------------------------------------------------------
-- 3. note detail (explicit columns: no embedding, no user_id; bounded children)
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.mcp_get_note_detail(
    p_key_hash text,
    p_note_id  uuid
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id uuid;
    v_note    jsonb;
    v_actions jsonb;
    v_logs    jsonb;
BEGIN
    v_user_id := private.resolve_mcp_key(p_key_hash);
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'unauthorized' USING ERRCODE = '28000';
    END IF;

    SELECT jsonb_build_object(
               'id', n.id,
               'title', n.title,
               'transcript', n.transcript,
               'summary', n.summary,
               'key_points', n.key_points,
               'wiifm', n.wiifm,
               'ai_status', n.ai_status,
               'duration_ms', n.duration_ms,
               'timestamp', n."timestamp"
           )
    INTO v_note
    FROM public.notes n
    WHERE n.id = p_note_id
      AND n.user_id = v_user_id
      AND n.is_private = false;

    IF v_note IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY t.created_at ASC), '[]'::jsonb)
    INTO v_actions
    FROM (
        SELECT a.id, a.note_id, a.description, a.is_completed, a.completed_by, a.completed_at, a.created_at
        FROM public.action_items a
        WHERE a.note_id = p_note_id
          AND a.user_id = v_user_id
        ORDER BY a.created_at ASC
        LIMIT 200
    ) t;

    -- Most recent 100 logs, returned oldest-first.
    SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY t.created_at ASC), '[]'::jsonb)
    INTO v_logs
    FROM (
        SELECT l.id, l.note_id, l.agent_name, l.message, l.created_at
        FROM public.agent_logs l
        WHERE l.note_id = p_note_id
          AND l.user_id = v_user_id
        ORDER BY l.created_at DESC
        LIMIT 100
    ) t;

    RETURN jsonb_build_object('note', v_note, 'actionItems', v_actions, 'agentLogs', v_logs);
END;
$$;

-- ------------------------------------------------------------------------------
-- 4. list action items - now honours notes.is_private
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.mcp_list_action_items(
    p_key_hash text,
    p_status   text DEFAULT 'pending',
    p_limit    int  DEFAULT 20
)
RETURNS TABLE (
    id           uuid,
    note_id      uuid,
    description  text,
    is_completed boolean,
    completed_by text,
    completed_at timestamptz,
    created_at   timestamptz
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id uuid;
    v_limit   int := LEAST(GREATEST(coalesce(p_limit, 20), 1), 50);
BEGIN
    v_user_id := private.resolve_mcp_key(p_key_hash);
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'unauthorized' USING ERRCODE = '28000';
    END IF;

    IF p_status IS NULL OR p_status NOT IN ('all', 'pending', 'completed') THEN
        RAISE EXCEPTION 'invalid status: expected all, pending or completed' USING ERRCODE = '22023';
    END IF;

    RETURN QUERY
    SELECT a.id, a.note_id, a.description, a.is_completed, a.completed_by, a.completed_at, a.created_at
    FROM public.action_items a
    JOIN public.notes n ON n.id = a.note_id
    WHERE a.user_id = v_user_id
      AND n.user_id = v_user_id
      AND n.is_private = false
      AND (
          p_status = 'all'
          OR (p_status = 'pending'   AND a.is_completed = false)
          OR (p_status = 'completed' AND a.is_completed = true)
      )
    ORDER BY a.created_at DESC
    LIMIT v_limit;
END;
$$;

-- ------------------------------------------------------------------------------
-- 5. update action item - now honours notes.is_private, sanitises attribution
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.mcp_update_action_item(
    p_key_hash   text,
    p_item_id    uuid,
    p_completed  boolean,
    p_agent_name text DEFAULT 'AI Agent'
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id uuid;
    v_agent   text;
    v_row     jsonb;
BEGIN
    v_user_id := private.resolve_mcp_key(p_key_hash);
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'unauthorized' USING ERRCODE = '28000';
    END IF;

    -- Free-text agent names are untrusted: strip to a safe charset and bound the length.
    -- The mandatory 'agent:' prefix means an agent can never masquerade as 'user'.
    v_agent := left(regexp_replace(coalesce(p_agent_name, ''), '[^A-Za-z0-9 ._@/-]', '', 'g'), 50);
    v_agent := nullif(trim(v_agent), '');
    v_agent := coalesce(v_agent, 'AI Agent');

    UPDATE public.action_items a
       SET is_completed = p_completed,
           completed_by = CASE WHEN p_completed THEN 'agent:' || v_agent ELSE NULL END,
           completed_at = CASE WHEN p_completed THEN now() ELSE NULL END
      FROM public.notes n
     WHERE a.id = p_item_id
       AND a.user_id = v_user_id
       AND n.id = a.note_id
       AND n.user_id = v_user_id
       AND n.is_private = false
    RETURNING jsonb_build_object(
        'id', a.id,
        'note_id', a.note_id,
        'description', a.description,
        'is_completed', a.is_completed,
        'completed_by', a.completed_by,
        'completed_at', a.completed_at,
        'created_at', a.created_at
    ) INTO v_row;

    RETURN v_row;
END;
$$;

-- ------------------------------------------------------------------------------
-- 6. append agent log - bounded per note
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.mcp_append_agent_log(
    p_key_hash   text,
    p_note_id    uuid,
    p_agent_name text,
    p_message    text
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id uuid;
    v_agent   text;
    v_message text;
    v_row     jsonb;
BEGIN
    v_user_id := private.resolve_mcp_key(p_key_hash);
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'unauthorized' USING ERRCODE = '28000';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM public.notes n
        WHERE n.id = p_note_id AND n.user_id = v_user_id AND n.is_private = false
    ) THEN
        RAISE EXCEPTION 'Note not found or access denied' USING ERRCODE = 'P0002';
    END IF;

    IF (SELECT count(*) FROM public.agent_logs l WHERE l.note_id = p_note_id) >= 500 THEN
        RAISE EXCEPTION 'Agent log limit (500) reached for this note' USING ERRCODE = '54000';
    END IF;

    v_message := left(trim(coalesce(p_message, '')), 4000);
    IF v_message = '' THEN
        RAISE EXCEPTION 'message must not be empty' USING ERRCODE = '22023';
    END IF;

    v_agent := left(regexp_replace(coalesce(p_agent_name, ''), '[^A-Za-z0-9 ._@/-]', '', 'g'), 50);
    v_agent := coalesce(nullif(trim(v_agent), ''), 'AI Agent');

    INSERT INTO public.agent_logs (note_id, user_id, agent_name, message, created_at)
    VALUES (p_note_id, v_user_id, v_agent, v_message, now())
    RETURNING jsonb_build_object(
        'id', agent_logs.id,
        'note_id', agent_logs.note_id,
        'agent_name', agent_logs.agent_name,
        'message', agent_logs.message,
        'created_at', agent_logs.created_at
    ) INTO v_row;

    RETURN v_row;
END;
$$;

-- ------------------------------------------------------------------------------
-- 7. cloud status for `srutam-mcp status` (the client used to read the tables directly
--    with the anon role, where RLS hides every row -> it always reported 0)
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.mcp_cloud_status(p_key_hash text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id uuid;
    v_notes   bigint;
    v_pending bigint;
BEGIN
    v_user_id := private.resolve_mcp_key(p_key_hash);
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'unauthorized' USING ERRCODE = '28000';
    END IF;

    SELECT count(*) INTO v_notes
    FROM public.notes n
    WHERE n.user_id = v_user_id AND n.is_private = false;

    SELECT count(*) INTO v_pending
    FROM public.action_items a
    JOIN public.notes n ON n.id = a.note_id
    WHERE a.user_id = v_user_id AND n.is_private = false AND a.is_completed = false;

    RETURN jsonb_build_object('userId', v_user_id, 'noteCount', v_notes, 'pendingActionsCount', v_pending);
END;
$$;

-- ------------------------------------------------------------------------------
-- 8. Grants: default-deny, then allow anon (the MCP server's role) on the mcp_* set only
-- ------------------------------------------------------------------------------
REVOKE ALL ON FUNCTION public.mcp_search_notes(text, text, int)                FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.mcp_get_note_detail(text, uuid)                  FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.mcp_list_action_items(text, text, int)           FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.mcp_update_action_item(text, uuid, boolean, text) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.mcp_append_agent_log(text, uuid, text, text)     FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.mcp_cloud_status(text)                           FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION public.mcp_search_notes(text, text, int)                TO anon;
GRANT EXECUTE ON FUNCTION public.mcp_get_note_detail(text, uuid)                  TO anon;
GRANT EXECUTE ON FUNCTION public.mcp_list_action_items(text, text, int)           TO anon;
GRANT EXECUTE ON FUNCTION public.mcp_update_action_item(text, uuid, boolean, text) TO anon;
GRANT EXECUTE ON FUNCTION public.mcp_append_agent_log(text, uuid, text, text)     TO anon;
GRANT EXECUTE ON FUNCTION public.mcp_cloud_status(text)                           TO anon;

-- ------------------------------------------------------------------------------
-- 9. Existing trigger function: pin search_path and close the concurrent-insert race
--    (two simultaneous inserts could both observe count < 3 and create a 4th key)
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
    PERFORM pg_advisory_xact_lock(hashtext(NEW.user_id::text));

    SELECT count(*) INTO active_count
    FROM public.api_keys k
    WHERE k.user_id = NEW.user_id AND k.revoked_at IS NULL;

    IF active_count >= 3 THEN
        RAISE EXCEPTION 'KEY_LIMIT_REACHED: Maximum 3 active API keys allowed per account. Please revoke an existing key first.';
    END IF;

    RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION public.check_active_api_keys_limit() FROM PUBLIC, anon, authenticated;

COMMIT;
