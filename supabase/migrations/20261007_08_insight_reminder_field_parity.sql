-- ==============================================================================
-- Field parity between the phone's insight/reminder model and the cloud
-- File: supabase/migrations/20261007_08_insight_reminder_field_parity.sql
--
-- WHY: migration 07 gave ideas/decisions and reminders a cloud home, but the
-- phone uploads only a lossy projection of each. ReminderEntity carries 25
-- fields and uploadNote() sent 10 - needsReview, confirmedAt and the whole
-- time-precision group never left the device, so an agent could not tell a
-- freshly extracted guess from a reminder the user had reviewed and locked in.
-- note_insights never received `status` either, so an archived idea still read
-- as open. And "convert to next step" creates a NEW action row linked by
-- sourceInsightId rather than mutating kind, so without the source columns an
-- agent sees the idea and its derived task as unrelated rows.
--
-- ALSO: mcp_list_reminders' upcoming-only filter required
-- `event_time IS NOT NULL AND event_time > now()`, which silently erased every
-- undated MILESTONE and every past target date - whole categories the user
-- could see in the app but no agent ever could. Fixed below to keep undated
-- milestones visible.
--
-- Additive. Idempotent. Both RPCs change their RETURNS TABLE signature, so they
-- are DROPped and recreated (Postgres refuses CREATE OR REPLACE across a return
-- type change) and their grants are re-applied, since DROP discards them.
-- ==============================================================================

BEGIN;

-- ------------------------------------------------------------------------------
-- 1. note_insights: lifecycle status and the provenance links.
--
--    status defaults to 'OPEN' for pre-parity rows. Unlike needs_review below,
--    'OPEN' is both the overwhelmingly common case and the harmless one to
--    assume: it shows the insight rather than hiding it, and the app's next
--    sync of each note overwrites it with the truth.
-- ------------------------------------------------------------------------------
ALTER TABLE public.note_insights ADD COLUMN IF NOT EXISTS status            text NOT NULL DEFAULT 'OPEN';
ALTER TABLE public.note_insights ADD COLUMN IF NOT EXISTS completed_at      timestamptz;
ALTER TABLE public.note_insights ADD COLUMN IF NOT EXISTS archived_at       timestamptz;
ALTER TABLE public.note_insights ADD COLUMN IF NOT EXISTS source_insight_id text;
ALTER TABLE public.note_insights ADD COLUMN IF NOT EXISTS source_reminder_id text;

-- ADD CONSTRAINT has no IF NOT EXISTS, so guard on the catalogue to stay re-runnable.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.note_insights'::regclass
          AND conname  = 'note_insights_status_check'
    ) THEN
        ALTER TABLE public.note_insights
            ADD CONSTRAINT note_insights_status_check
            CHECK (status IN ('OPEN', 'COMPLETED', 'ARCHIVED'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_note_insights_status ON public.note_insights (status);

-- ------------------------------------------------------------------------------
-- 2. reminders: review state, confirmation, resolved local time, task link.
--
--    needs_review is deliberately NULLABLE WITH NO DEFAULT. The app's local
--    default is true, but for rows already in the cloud we genuinely do not
--    know: defaulting to false would falsely assert "the user reviewed this",
--    and defaulting to true would falsely assert "this is unconfirmed". NULL
--    means "unknown, pre-parity row" and the one-time backfill replaces it with
--    the real value. MCP must render NULL distinctly from false - an unknown
--    review state must never read as confirmed.
-- ------------------------------------------------------------------------------
ALTER TABLE public.reminders ADD COLUMN IF NOT EXISTS needs_review   boolean;
ALTER TABLE public.reminders ADD COLUMN IF NOT EXISTS confirmed_at   timestamptz;
ALTER TABLE public.reminders ADD COLUMN IF NOT EXISTS time_precision text;
ALTER TABLE public.reminders ADD COLUMN IF NOT EXISTS local_date     text;
ALTER TABLE public.reminders ADD COLUMN IF NOT EXISTS local_time     text;
ALTER TABLE public.reminders ADD COLUMN IF NOT EXISTS zone_id        text;
ALTER TABLE public.reminders ADD COLUMN IF NOT EXISTS linked_task_id text;

-- ------------------------------------------------------------------------------
-- 3. Drop the old RPCs. Both change their return type, and mcp_list_insights
--    also gains a parameter - leaving the 3-arg version in place would create a
--    second overload that a {key_hash, kind, limit} call could resolve to
--    instead, so the old signature must go rather than merely be shadowed.
-- ------------------------------------------------------------------------------
DROP FUNCTION IF EXISTS public.mcp_list_insights(text, text, int);
DROP FUNCTION IF EXISTS public.mcp_list_reminders(text, boolean, int);

-- ------------------------------------------------------------------------------
-- 4. mcp_list_insights - now carries lifecycle status and provenance.
--    Archived insights are hidden unless explicitly asked for.
-- ------------------------------------------------------------------------------
CREATE FUNCTION public.mcp_list_insights(
    p_key_hash         text,
    p_kind             text    DEFAULT 'all',
    p_include_archived boolean DEFAULT false,
    p_limit            int     DEFAULT 20
)
RETURNS TABLE (
    id                 uuid,
    note_id            uuid,
    kind               text,
    text               text,
    evidence           text,
    rationale          text,
    status             text,
    completed_at       timestamptz,
    archived_at        timestamptz,
    source_insight_id  text,
    source_reminder_id text,
    created_at         timestamptz
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
    SELECT i.id, i.note_id, i.kind, i.text, i.evidence, i.rationale,
           i.status, i.completed_at, i.archived_at,
           i.source_insight_id, i.source_reminder_id, i.created_at
    FROM public.note_insights i
    JOIN public.notes n ON n.id = i.note_id
    WHERE i.user_id = v_user_id
      AND n.user_id = v_user_id
      AND n.is_private = false
      AND (p_kind = 'all' OR i.kind = p_kind)
      AND (coalesce(p_include_archived, false) OR i.status <> 'ARCHIVED')
    ORDER BY i.created_at DESC
    LIMIT v_limit;
END;
$$;

-- ------------------------------------------------------------------------------
-- 5. mcp_list_reminders - read-only, now carrying review state and resolved
--    local time.
--
--    upcoming_only no longer requires a non-null event_time. A MILESTONE or
--    target date with no clock time is still a live, upcoming commitment; the
--    old predicate dropped it entirely. Undated rows sort last (NULLS LAST).
--    Past-dated reminders remain excluded from upcoming_only, as intended.
-- ------------------------------------------------------------------------------
CREATE FUNCTION public.mcp_list_reminders(
    p_key_hash       text,
    p_upcoming_only  boolean DEFAULT true,
    p_limit          int     DEFAULT 20
)
RETURNS TABLE (
    id             uuid,
    note_id        uuid,
    title          text,
    event_time     timestamptz,
    original_text  text,
    person         text,
    location       text,
    type           text,
    status         text,
    needs_review   boolean,
    confirmed_at   timestamptz,
    time_precision text,
    local_date     text,
    local_time     text,
    zone_id        text,
    linked_task_id text,
    created_at     timestamptz
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
    SELECT r.id, r.note_id, r.title, r.event_time, r.original_text, r.person,
           r.location, r.type, r.status,
           r.needs_review, r.confirmed_at, r.time_precision,
           r.local_date, r.local_time, r.zone_id, r.linked_task_id,
           r.created_at
    FROM public.reminders r
    JOIN public.notes n ON n.id = r.note_id
    WHERE r.user_id = v_user_id
      AND n.user_id = v_user_id
      AND n.is_private = false
      AND (
          p_upcoming_only = false
          OR (r.status = 'ACTIVE' AND (r.event_time IS NULL OR r.event_time > now()))
      )
    ORDER BY CASE WHEN p_upcoming_only THEN r.event_time ELSE r.created_at END ASC NULLS LAST
    LIMIT v_limit;
END;
$$;

-- ------------------------------------------------------------------------------
-- 6. Grants: default deny, then the narrowest allow (anon only, same as every
--    other MCP RPC - the key hash is the credential, not a Supabase session).
--    Re-applied because the DROPs above discarded the previous grants.
-- ------------------------------------------------------------------------------
REVOKE ALL ON FUNCTION public.mcp_list_insights(text, text, boolean, int)   FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.mcp_list_reminders(text, boolean, int)        FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION public.mcp_list_insights(text, text, boolean, int) TO anon;
GRANT EXECUTE ON FUNCTION public.mcp_list_reminders(text, boolean, int)      TO anon;

COMMIT;
