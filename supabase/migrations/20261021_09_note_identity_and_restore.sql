-- ==============================================================================
-- Stable note identity, so a reinstall restores notes instead of duplicating them
-- File: supabase/migrations/20261021_09_note_identity_and_restore.sql
--
-- WHY: the phone uploaded a note with a plain INSERT whenever it had no cloud id,
-- and its only identity was client_recording_id, the local Room autoincrement id.
-- That id restarts at 1 after a reinstall, so a reinstalled phone uploaded every
-- re-processed note again as a new row, and two different notes could share one
-- client_recording_id. The app (2.6.0+) now gives each note a stable note_uuid
-- (kept inside the audio file too) and upserts on (user_id, note_uuid).
--
-- 1. notes gets note_uuid, audio_hash (SHA-256 of the audio), topics and
--    client_updated_at (when the note's content last changed on the phone,
--    used to decide merges on restore).
-- 2. A one-time cleanup merges the duplicates already uploaded. Duplicates are
--    found by content (user, timestamp, duration, transcript), NOT by
--    client_recording_id, which is not unique across reinstalls.
-- 3. A unique index on (user_id, note_uuid) stops new duplicates. NULLs are
--    distinct in Postgres, so older rows without a note_uuid coexist until the
--    phone's next sync fills theirs in.
--
-- Idempotent. Additive apart from deleting duplicate rows, whose children are
-- moved to the kept note first.
-- ==============================================================================

BEGIN;

-- ------------------------------------------------------------------------------
-- 1. New columns
-- ------------------------------------------------------------------------------
ALTER TABLE public.notes ADD COLUMN IF NOT EXISTS note_uuid         text;
ALTER TABLE public.notes ADD COLUMN IF NOT EXISTS audio_hash        text;
ALTER TABLE public.notes ADD COLUMN IF NOT EXISTS topics            jsonb NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE public.notes ADD COLUMN IF NOT EXISTS client_updated_at timestamptz;

CREATE INDEX IF NOT EXISTS idx_notes_audio_hash ON public.notes (user_id, audio_hash);

-- ------------------------------------------------------------------------------
-- 2. Merge duplicates. Kept: the most recently updated row of each group.
--    Children of the others move to it; where both have the same child (same
--    client id), the kept note's child stays, but a completion on the other
--    copy is carried over so an agent's "done" is never lost.
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.srutam_merge_duplicate_notes()
RETURNS integer
LANGUAGE plpgsql
SET search_path = ''
AS $$
DECLARE
    v_removed integer;
BEGIN
    CREATE TEMP TABLE _note_dupes ON COMMIT DROP AS
    WITH ranked AS (
        SELECT n.id,
               first_value(n.id) OVER w AS keep_id,
               row_number()      OVER w AS rn
        FROM public.notes n
        WINDOW w AS (
            PARTITION BY n.user_id, n."timestamp", coalesce(n.duration_ms, 0),
                         md5(coalesce(n.transcript, ''))
            ORDER BY n.updated_at DESC, n.id
        )
    )
    SELECT id AS dup_id, keep_id FROM ranked WHERE rn > 1;

    -- Keep the kept note's note_uuid / audio_hash if it has one, else take the duplicate's.
    UPDATE public.notes k
    SET note_uuid  = coalesce(k.note_uuid, d.note_uuid),
        audio_hash = coalesce(k.audio_hash, d.audio_hash)
    FROM (
        SELECT dd.keep_id,
               max(n.note_uuid)  AS note_uuid,
               max(n.audio_hash) AS audio_hash
        FROM _note_dupes dd JOIN public.notes n ON n.id = dd.dup_id
        GROUP BY dd.keep_id
    ) d
    WHERE k.id = d.keep_id;
    UPDATE public.notes n SET note_uuid = NULL
    FROM _note_dupes dd WHERE n.id = dd.dup_id;

    -- action_items: carry completions over, drop clashing copies, move the rest.
    UPDATE public.action_items k
    SET is_completed = true,
        completed_by = coalesce(k.completed_by, d.completed_by),
        completed_at = coalesce(k.completed_at, d.completed_at)
    FROM public.action_items d
    JOIN _note_dupes dd ON d.note_id = dd.dup_id
    WHERE k.note_id = dd.keep_id
      AND k.client_insight_id = d.client_insight_id
      AND d.is_completed AND NOT k.is_completed;
    DELETE FROM public.action_items d
    USING _note_dupes dd, public.action_items k
    WHERE d.note_id = dd.dup_id AND k.note_id = dd.keep_id
      AND k.client_insight_id = d.client_insight_id;
    UPDATE public.action_items d SET note_id = dd.keep_id
    FROM _note_dupes dd WHERE d.note_id = dd.dup_id;

    -- note_insights: same, by status (OPEN < COMPLETED < ARCHIVED).
    UPDATE public.note_insights k
    SET status       = d.status,
        completed_at = coalesce(k.completed_at, d.completed_at),
        archived_at  = coalesce(k.archived_at, d.archived_at)
    FROM public.note_insights d
    JOIN _note_dupes dd ON d.note_id = dd.dup_id
    WHERE k.note_id = dd.keep_id
      AND k.client_insight_id = d.client_insight_id
      AND (CASE d.status WHEN 'ARCHIVED' THEN 2 WHEN 'COMPLETED' THEN 1 ELSE 0 END)
        > (CASE k.status WHEN 'ARCHIVED' THEN 2 WHEN 'COMPLETED' THEN 1 ELSE 0 END);
    DELETE FROM public.note_insights d
    USING _note_dupes dd, public.note_insights k
    WHERE d.note_id = dd.dup_id AND k.note_id = dd.keep_id
      AND k.client_insight_id = d.client_insight_id;
    UPDATE public.note_insights d SET note_id = dd.keep_id
    FROM _note_dupes dd WHERE d.note_id = dd.dup_id;

    -- reminders: ACTIVE < DISMISSED/EXPIRED < COMPLETED.
    UPDATE public.reminders k
    SET status = d.status
    FROM public.reminders d
    JOIN _note_dupes dd ON d.note_id = dd.dup_id
    WHERE k.note_id = dd.keep_id
      AND k.client_reminder_id = d.client_reminder_id
      AND (CASE d.status WHEN 'COMPLETED' THEN 2 WHEN 'ACTIVE' THEN 0 ELSE 1 END)
        > (CASE k.status WHEN 'COMPLETED' THEN 2 WHEN 'ACTIVE' THEN 0 ELSE 1 END);
    DELETE FROM public.reminders d
    USING _note_dupes dd, public.reminders k
    WHERE d.note_id = dd.dup_id AND k.note_id = dd.keep_id
      AND k.client_reminder_id = d.client_reminder_id;
    UPDATE public.reminders d SET note_id = dd.keep_id
    FROM _note_dupes dd WHERE d.note_id = dd.dup_id;

    -- agent_logs have no client id: all of them move.
    UPDATE public.agent_logs l SET note_id = dd.keep_id
    FROM _note_dupes dd WHERE l.note_id = dd.dup_id;

    DELETE FROM public.notes n USING _note_dupes dd WHERE n.id = dd.dup_id;
    GET DIAGNOSTICS v_removed = ROW_COUNT;

    DROP TABLE _note_dupes;
    RETURN v_removed;
END;
$$;

REVOKE ALL ON FUNCTION public.srutam_merge_duplicate_notes() FROM PUBLIC, anon, authenticated;

SELECT public.srutam_merge_duplicate_notes();

-- ------------------------------------------------------------------------------
-- 3. One cloud row per note from now on. The phone upserts with
--    POST /notes?on_conflict=user_id,note_uuid.
-- ------------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS uq_notes_user_note_uuid
    ON public.notes (user_id, note_uuid);

COMMIT;
