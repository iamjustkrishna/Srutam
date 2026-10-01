-- ==============================================================================
-- Ideas, decisions, reminders for MCP + upsert-safe action item sync
-- File: supabase/migrations/20261006_07_insights_reminders_and_upsert_sync.sql
--
-- WHY: the app captures four things per voice note (notes, action items, ideas/
-- decisions, reminders) but MCP only ever exposed the first two. This adds read
-- access to ideas/decisions (mcp_list_insights) and reminders (mcp_list_reminders,
-- read-only: reminders drive real device notifications, so no MCP mutation yet).
--
-- ALSO: action_items had no stable identity of its own - the phone's uploadNote()
-- deleted every action item for a note and reinserted them on each resync, so a
-- completion an agent made via update_action_item was silently wiped on the next
-- sync and the row got a new id. client_insight_id is the id the app already
-- generates locally (InsightEntity.id); adding it as an upsert key lets the phone
-- switch from delete+reinsert to an upsert that preserves server-side state.
--
-- Additive except for the new column + index on action_items. Idempotent.
-- ==============================================================================

BEGIN;

-- ------------------------------------------------------------------------------
-- 1. action_items: add the stable client id so sync can upsert instead of
--    delete-and-reinsert. Existing rows have no client id and are left alone;
--    only the app's NEXT sync of each note starts sending one.
-- ------------------------------------------------------------------------------
ALTER TABLE public.action_items ADD COLUMN IF NOT EXISTS client_insight_id text;
-- A plain (non-partial) unique index: Postgres already treats NULL <> NULL, so existing
-- rows without a client id coexist freely, and PostgREST's on_conflict upsert (which
-- cannot target a partial index's WHERE predicate) can infer this index directly.

CREATE UNIQUE INDEX IF NOT EXISTS uq_action_items_note_client
    ON public.action_items (note_id, client_insight_id);

-- ------------------------------------------------------------------------------
-- 2. note_insights: ideas and decisions (action items stay in action_items,
--    unchanged, so existing MCP tools and data are untouched).
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.note_insights (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id           uuid NOT NULL REFERENCES public.notes(id) ON DELETE CASCADE,
    user_id           uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    client_insight_id text,
    kind              text NOT NULL CHECK (kind IN ('idea', 'decision')),
    text              text NOT NULL,
    evidence          text,
    rationale         text,
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_note_insights_user_id ON public.note_insights (user_id);
CREATE INDEX IF NOT EXISTS idx_note_insights_note_id ON public.note_insights (note_id);
CREATE INDEX IF NOT EXISTS idx_note_insights_kind     ON public.note_insights (kind);
CREATE UNIQUE INDEX IF NOT EXISTS uq_note_insights_note_client
    ON public.note_insights (note_id, client_insight_id);

ALTER TABLE public.note_insights ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own note insights" ON public.note_insights;
CREATE POLICY "Users can manage their own note insights"
    ON public.note_insights FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 3. reminders: meetings, deadlines, calls, milestones with real times.
--    Read-only through MCP; the app remains the sole writer of status/schedule.
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.reminders (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id            uuid NOT NULL REFERENCES public.notes(id) ON DELETE CASCADE,
    user_id            uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    client_reminder_id text,
    title              text NOT NULL,
    event_time         timestamptz,
    original_text      text,
    person             text,
    location           text,
    type               text,
    status             text NOT NULL DEFAULT 'ACTIVE',
    created_at         timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_reminders_user_id    ON public.reminders (user_id);
CREATE INDEX IF NOT EXISTS idx_reminders_note_id    ON public.reminders (note_id);
CREATE INDEX IF NOT EXISTS idx_reminders_event_time ON public.reminders (event_time);
CREATE UNIQUE INDEX IF NOT EXISTS uq_reminders_note_client
    ON public.reminders (note_id, client_reminder_id);

ALTER TABLE public.reminders ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own reminders" ON public.reminders;
CREATE POLICY "Users can manage their own reminders"
    ON public.reminders FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 4. mcp_list_insights - ideas and decisions, same shape as mcp_list_action_items
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.mcp_list_insights(
    p_key_hash text,
    p_kind     text DEFAULT 'all',
    p_limit    int  DEFAULT 20
)
RETURNS TABLE (
    id         uuid,
    note_id    uuid,
    kind       text,
    text       text,
    evidence   text,
    rationale  text,
    created_at timestamptz
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

    IF p_kind IS NULL OR p_kind NOT IN ('all', 'idea', 'decision') THEN
        RAISE EXCEPTION 'invalid kind: expected all, idea or decision' USING ERRCODE = '22023';
    END IF;

    RETURN QUERY
    SELECT i.id, i.note_id, i.kind, i.text, i.evidence, i.rationale, i.created_at
    FROM public.note_insights i
    JOIN public.notes n ON n.id = i.note_id
    WHERE i.user_id = v_user_id
      AND n.user_id = v_user_id
      AND n.is_private = false
      AND (p_kind = 'all' OR i.kind = p_kind)
    ORDER BY i.created_at DESC
    LIMIT v_limit;
END;
$$;

-- ------------------------------------------------------------------------------
-- 5. mcp_list_reminders - read-only; upcoming-only filters to ACTIVE + future
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.mcp_list_reminders(
    p_key_hash       text,
    p_upcoming_only  boolean DEFAULT true,
    p_limit          int     DEFAULT 20
)
RETURNS TABLE (
    id            uuid,
    note_id       uuid,
    title         text,
    event_time    timestamptz,
    original_text text,
    person        text,
    location      text,
    type          text,
    status        text,
    created_at    timestamptz
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

    RETURN QUERY
    SELECT r.id, r.note_id, r.title, r.event_time, r.original_text, r.person, r.location, r.type, r.status, r.created_at
    FROM public.reminders r
    JOIN public.notes n ON n.id = r.note_id
    WHERE r.user_id = v_user_id
      AND n.user_id = v_user_id
      AND n.is_private = false
      AND (
          p_upcoming_only = false
          OR (r.status = 'ACTIVE' AND r.event_time IS NOT NULL AND r.event_time > now())
      )
    ORDER BY CASE WHEN p_upcoming_only THEN r.event_time ELSE r.created_at END ASC NULLS LAST
    LIMIT v_limit;
END;
$$;

-- ------------------------------------------------------------------------------
-- 6. Grants: default deny, then the narrowest allow (anon only, same as every
--    other MCP RPC - the key hash is the credential, not a Supabase session).
-- ------------------------------------------------------------------------------
REVOKE ALL ON FUNCTION public.mcp_list_insights(text, text, int)            FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.mcp_list_reminders(text, boolean, int)        FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION public.mcp_list_insights(text, text, int)         TO anon;
GRANT EXECUTE ON FUNCTION public.mcp_list_reminders(text, boolean, int)     TO anon;

COMMIT;
