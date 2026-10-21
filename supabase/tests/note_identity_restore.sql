-- ==============================================================================
-- Note identity / duplicate cleanup tests (needs migrations 01-09). Rolled back; scratch DBs only.
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/note_identity_restore.sql
-- Uploads run as role `authenticated` with a JWT subject, the way the phone does through PostgREST.
-- ==============================================================================

BEGIN;

CREATE SCHEMA t;
GRANT USAGE ON SCHEMA t TO PUBLIC;

CREATE FUNCTION t.ok(cond boolean, msg text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF cond IS NOT TRUE THEN RAISE EXCEPTION 'FAIL: %', msg; END IF;
    RAISE NOTICE 'ok - %', msg;
END;
$$;

CREATE FUNCTION t.raises(stmt text, expected_state text, msg text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE stmt;
    EXCEPTION WHEN OTHERS THEN
        IF SQLSTATE = expected_state THEN RAISE NOTICE 'ok - %', msg; RETURN; END IF;
        RAISE EXCEPTION 'FAIL: % (expected %, got % / %)', msg, expected_state, SQLSTATE, SQLERRM;
    END;
    RAISE EXCEPTION 'FAIL: % (did not raise)', msg;
END;
$$;

GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA t TO PUBLIC;

INSERT INTO auth.users (id) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001'),
    ('bbbbbbbb-0000-0000-0000-000000000002');

-- ------------------------------------------------------------------------------
-- 1. Upload as the phone does: POST notes?on_conflict=user_id,note_uuid
--    (resolution=merge-duplicates), i.e. INSERT ... ON CONFLICT DO UPDATE.
-- ------------------------------------------------------------------------------
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', 'aaaaaaaa-0000-0000-0000-000000000001', true);

INSERT INTO public.notes (user_id, client_recording_id, note_uuid, title, transcript, "timestamp")
VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 1, 'uuid-1', 'First upload', 'hello', '2026-10-01T10:00:00Z')
ON CONFLICT (user_id, note_uuid) DO UPDATE SET title = EXCLUDED.title, transcript = EXCLUDED.transcript;

-- After a reinstall the local id restarts at 1 again, but the note_uuid is the same note's.
INSERT INTO public.notes (user_id, client_recording_id, note_uuid, title, transcript, "timestamp")
VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 1, 'uuid-1', 'Edited after reinstall', 'hello', '2026-10-01T10:00:00Z')
ON CONFLICT (user_id, note_uuid) DO UPDATE SET title = EXCLUDED.title, transcript = EXCLUDED.transcript;

SELECT t.ok((SELECT count(*) FROM public.notes WHERE note_uuid = 'uuid-1') = 1,
            're-uploading the same note_uuid updates the row instead of adding one');
SELECT t.ok((SELECT title FROM public.notes WHERE note_uuid = 'uuid-1') = 'Edited after reinstall',
            'the upsert keeps the newer content');

-- A different note that happens to reuse client_recording_id 1 is still its own row.
INSERT INTO public.notes (user_id, client_recording_id, note_uuid, title, transcript, "timestamp")
VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 1, 'uuid-2', 'Another note', 'other', '2026-10-02T10:00:00Z')
ON CONFLICT (user_id, note_uuid) DO UPDATE SET title = EXCLUDED.title;
SELECT t.ok((SELECT count(*) FROM public.notes WHERE client_recording_id = 1) = 2,
            'notes sharing a client_recording_id are not merged by the upload');

SELECT t.raises(
    $$INSERT INTO public.notes (user_id, note_uuid, title) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'uuid-1', 'plain insert')$$,
    '23505', 'a plain second insert of the same note_uuid is rejected');

-- Another user may use the same note_uuid string without clashing, and can't see user A's rows.
SELECT set_config('request.jwt.claim.sub', 'bbbbbbbb-0000-0000-0000-000000000002', true);
INSERT INTO public.notes (user_id, note_uuid, title) VALUES ('bbbbbbbb-0000-0000-0000-000000000002', 'uuid-1', 'B''s note');
SELECT t.ok((SELECT count(*) FROM public.notes) = 1, 'RLS: user B only sees their own note');
RESET ROLE;

-- Older rows without a note_uuid coexist (NULLs are distinct).
INSERT INTO public.notes (user_id, title, "timestamp") VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'legacy a', '2026-08-01T10:00:00Z'),
    ('aaaaaaaa-0000-0000-0000-000000000001', 'legacy b', '2026-08-02T10:00:00Z');
SELECT t.ok((SELECT count(*) FROM public.notes WHERE note_uuid IS NULL) >= 2,
            'several notes without a note_uuid are allowed');

-- ------------------------------------------------------------------------------
-- 2. Duplicate cleanup: the same recording uploaded twice before 2.6.0.
-- ------------------------------------------------------------------------------
INSERT INTO public.notes (id, user_id, client_recording_id, title, transcript, duration_ms, "timestamp", updated_at)
VALUES
    ('11111111-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 7, 'Standup', 'same words', 60000,
     '2026-09-20T09:00:00Z', '2026-09-20T09:05:00Z'),
    -- The reinstall's re-upload: newer, so it is the one kept.
    ('11111111-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 3, 'Standup', 'same words', 60000,
     '2026-09-20T09:00:00Z', '2026-10-10T12:00:00Z');

-- Same task on both copies; only the old copy was completed by an agent.
INSERT INTO public.action_items (note_id, user_id, client_insight_id, description, is_completed, completed_by, completed_at) VALUES
    ('11111111-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'task-1', 'Send notes', true, 'agent:Cursor', '2026-09-21T10:00:00Z'),
    ('11111111-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'task-1', 'Send notes', false, NULL, NULL),
    -- A task only the old copy has.
    ('11111111-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'task-2', 'Book call', false, NULL, NULL);
INSERT INTO public.note_insights (note_id, user_id, client_insight_id, kind, text, status) VALUES
    ('11111111-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'idea-1', 'idea', 'Sample note', 'ARCHIVED'),
    ('11111111-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'idea-1', 'idea', 'Sample note', 'OPEN');
INSERT INTO public.reminders (note_id, user_id, client_reminder_id, title, status) VALUES
    ('11111111-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'rem-1', 'Check installs', 'COMPLETED'),
    ('11111111-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'rem-1', 'Check installs', 'ACTIVE');
INSERT INTO public.agent_logs (note_id, user_id, agent_name, message) VALUES
    ('11111111-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Cursor', 'Done');

-- A note with the same time but different words is NOT a duplicate.
INSERT INTO public.notes (id, user_id, title, transcript, duration_ms, "timestamp")
VALUES ('11111111-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000001', 'Standup', 'different words', 60000,
        '2026-09-20T09:00:00Z');

SELECT t.ok(public.srutam_merge_duplicate_notes() = 1, 'cleanup removes exactly the one duplicate');

SELECT t.ok(NOT EXISTS (SELECT 1 FROM public.notes WHERE id = '11111111-0000-0000-0000-000000000001'),
            'the older copy is gone');
SELECT t.ok(EXISTS (SELECT 1 FROM public.notes WHERE id = '11111111-0000-0000-0000-000000000003'),
            'a different note at the same time is kept');
SELECT t.ok((SELECT count(*) FROM public.action_items WHERE note_id = '11111111-0000-0000-0000-000000000002') = 2,
            'tasks end up on the kept note, without doubles');
SELECT t.ok((SELECT is_completed AND completed_by = 'agent:Cursor' FROM public.action_items
             WHERE note_id = '11111111-0000-0000-0000-000000000002' AND client_insight_id = 'task-1'),
            'an agent''s "done" on the removed copy is kept');
SELECT t.ok((SELECT status FROM public.note_insights
             WHERE note_id = '11111111-0000-0000-0000-000000000002' AND client_insight_id = 'idea-1') = 'ARCHIVED',
            'the most-progressed insight status wins');
SELECT t.ok((SELECT status FROM public.reminders
             WHERE note_id = '11111111-0000-0000-0000-000000000002' AND client_reminder_id = 'rem-1') = 'COMPLETED',
            'the most-progressed reminder status wins');
SELECT t.ok((SELECT count(*) FROM public.reminders WHERE client_reminder_id = 'rem-1') = 1,
            'no duplicate reminders remain');
SELECT t.ok((SELECT note_id FROM public.agent_logs WHERE message = 'Done') = '11111111-0000-0000-0000-000000000002',
            'agent logs move to the kept note');
SELECT t.ok(public.srutam_merge_duplicate_notes() = 0, 'running the cleanup again changes nothing');

-- The phone can't run the cleanup.
SET LOCAL ROLE authenticated;
SELECT t.raises($$SELECT public.srutam_merge_duplicate_notes()$$, '42501', 'authenticated users cannot call the cleanup');
RESET ROLE;

ROLLBACK;
