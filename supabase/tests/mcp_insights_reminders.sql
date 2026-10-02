-- ==============================================================================
-- Ideas/decisions/reminders tests + the action_items upsert fix.
--
-- Dependency-free (no pgTAP). Everything runs in one transaction that is ROLLED
-- BACK, so it is safe on a scratch database. NEVER run against production.
--
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/mcp_insights_reminders.sql
--
-- Requires migrations 01-08.
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
        RAISE EXCEPTION 'FAIL: % (expected SQLSTATE %, got % / %)', msg, expected_state, SQLSTATE, SQLERRM;
    END;
    RAISE EXCEPTION 'FAIL: % (statement did not raise)', msg;
END;
$$;

CREATE FUNCTION t.h(raw text) RETURNS text LANGUAGE sql IMMUTABLE AS
$$ SELECT encode(sha256(convert_to(raw, 'UTF8')), 'hex') $$;

-- ------------------------------------------------------------------------------
-- Fixtures: two users, one note each (plus a private note for user A)
-- ------------------------------------------------------------------------------
INSERT INTO auth.users (id, email) VALUES
    ('aaaaaaaa-0000-4000-8000-000000000001', 'alice@example.com'),
    ('bbbbbbbb-0000-4000-8000-000000000002', 'bob@example.com');

INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name) VALUES
    ('aaaaaaaa-0000-4000-8000-000000000001', t.h('keyA'), 'p', 'A'),
    ('bbbbbbbb-0000-4000-8000-000000000002', t.h('keyB'), 'p', 'B');

INSERT INTO public.notes (id, user_id, title, is_private) VALUES
    ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'Alpha public note', false),
    ('a2000000-0000-4000-8000-000000000002', 'aaaaaaaa-0000-4000-8000-000000000001', 'Alpha private note', true),
    ('b1000000-0000-4000-8000-000000000001', 'bbbbbbbb-0000-4000-8000-000000000002', 'Bob public note', false);

-- ------------------------------------------------------------------------------
-- 1. Grants: these are read tools, callable by anon (the key-hash credential),
--    same as every other MCP RPC.
-- ------------------------------------------------------------------------------
SELECT t.ok(has_function_privilege('anon', 'public.mcp_list_insights(text,text,boolean,integer)', 'EXECUTE'),
            'anon can call mcp_list_insights');
SELECT t.ok(has_function_privilege('anon', 'public.mcp_list_reminders(text,boolean,integer)', 'EXECUTE'),
            'anon can call mcp_list_reminders');
SELECT t.ok(NOT has_function_privilege('authenticated', 'public.mcp_list_insights(text,text,boolean,integer)', 'EXECUTE'),
            'authenticated (the phone JWT) is not a valid MCP caller for list_insights');
SELECT t.ok(
    (SELECT bool_and(EXISTS (SELECT 1 FROM unnest(p.proconfig) c WHERE c LIKE 'search_path=%'))
       FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
      WHERE n.nspname = 'public' AND p.proname IN ('mcp_list_insights', 'mcp_list_reminders')),
    'both new functions pin search_path');

-- ------------------------------------------------------------------------------
-- 2. mcp_list_insights: kind filter + private-note exclusion + cross-tenant isolation
-- ------------------------------------------------------------------------------
INSERT INTO public.note_insights (note_id, user_id, client_insight_id, kind, text) VALUES
    ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'i1', 'idea', 'Alpha idea public'),
    ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'i2', 'decision', 'Alpha decision public'),
    ('a2000000-0000-4000-8000-000000000002', 'aaaaaaaa-0000-4000-8000-000000000001', 'i3', 'idea', 'Alpha idea PRIVATE'),
    ('b1000000-0000-4000-8000-000000000001', 'bbbbbbbb-0000-4000-8000-000000000002', 'i4', 'idea', 'Bob idea');

SET LOCAL ROLE anon;

SELECT t.ok(
    (SELECT array_agg(text ORDER BY text) FROM public.mcp_list_insights(t.h('keyA'), 'all', false, 20))
        = ARRAY['Alpha decision public', 'Alpha idea public'],
    'key A sees only its own public-note insights (not its private note, not Bob''s)');

SELECT t.ok(
    (SELECT array_agg(text) FROM public.mcp_list_insights(t.h('keyA'), 'idea', false, 20)) = ARRAY['Alpha idea public'],
    'kind=idea filters to ideas only');

SELECT t.ok(
    (SELECT array_agg(text) FROM public.mcp_list_insights(t.h('keyA'), 'decision', false, 20)) = ARRAY['Alpha decision public'],
    'kind=decision filters to decisions only');

SELECT t.ok(
    (SELECT array_agg(text) FROM public.mcp_list_insights(t.h('keyB'), 'all', false, 20)) = ARRAY['Bob idea'],
    'key B sees only Bob''s insight, never Alpha''s');

SELECT t.raises(format('SELECT * FROM public.mcp_list_insights(%L, ''bogus'', false, 20)', t.h('keyA')),
                '22023', 'an invalid kind is rejected');
SELECT t.raises('SELECT * FROM public.mcp_list_insights(''not-a-hash'', ''all'', false, 20)',
                '28000', 'an unknown key is rejected');

RESET ROLE;

-- ------------------------------------------------------------------------------
-- 3. mcp_list_reminders: upcoming-only filter + private-note exclusion
-- ------------------------------------------------------------------------------
INSERT INTO public.reminders (note_id, user_id, client_reminder_id, title, event_time, status) VALUES
    ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'r1', 'Future meeting', now() + interval '1 day', 'ACTIVE'),
    ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'r2', 'Past meeting', now() - interval '1 day', 'ACTIVE'),
    ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'r3', 'Dismissed future', now() + interval '2 day', 'DISMISSED'),
    ('a2000000-0000-4000-8000-000000000002', 'aaaaaaaa-0000-4000-8000-000000000001', 'r4', 'Private future', now() + interval '1 day', 'ACTIVE');

SET LOCAL ROLE anon;

SELECT t.ok(
    (SELECT array_agg(title) FROM public.mcp_list_reminders(t.h('keyA'), true, 20)) = ARRAY['Future meeting'],
    'upcoming_only excludes past, dismissed, and private-note reminders');

SELECT t.ok(
    (SELECT array_agg(title ORDER BY title) FROM public.mcp_list_reminders(t.h('keyA'), false, 20))
        = ARRAY['Dismissed future', 'Future meeting', 'Past meeting'],
    'upcoming_only=false returns all statuses/times but still excludes the private note');

SELECT t.ok(
    (SELECT count(*) FROM public.mcp_list_reminders(t.h('keyB'), false, 20)) = 0,
    'key B sees none of Alpha''s reminders');

RESET ROLE;

-- ------------------------------------------------------------------------------
-- 4. The upsert fix: resyncing a note must not lose an agent's completion,
--    must not change the row id, and must coexist with pre-migration rows that
--    have no client_insight_id at all.
-- ------------------------------------------------------------------------------

-- a "legacy" row from before this migration: no client id
INSERT INTO public.action_items (note_id, user_id, description, is_completed)
    VALUES ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'Legacy task', false);

-- first "sync": upsert with a client id
INSERT INTO public.action_items (note_id, user_id, client_insight_id, description, is_completed)
    VALUES ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'client-1', 'Ship it', false)
    ON CONFLICT (note_id, client_insight_id) DO UPDATE SET description = excluded.description;

CREATE TEMP TABLE first_sync_id AS
    SELECT id FROM public.action_items WHERE client_insight_id = 'client-1';
GRANT SELECT ON first_sync_id TO PUBLIC;  -- readable after SET LOCAL ROLE anon below

SET LOCAL ROLE anon;
SELECT t.ok(
    (public.mcp_update_action_item(t.h('keyA'), (SELECT id FROM first_sync_id), true, 'TestAgent') ->> 'is_completed')::boolean,
    'an agent completes the synced task');
RESET ROLE;

-- second "sync" of the SAME note: the phone upserts the same client id again
INSERT INTO public.action_items (note_id, user_id, client_insight_id, description, is_completed)
    VALUES ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'client-1', 'Ship it', false)
    ON CONFLICT (note_id, client_insight_id) DO UPDATE SET description = excluded.description;

SELECT t.ok(
    (SELECT id FROM public.action_items WHERE client_insight_id = 'client-1') = (SELECT id FROM first_sync_id),
    'resyncing the same note keeps the SAME row id (no delete+reinsert)');
SELECT t.ok(
    (SELECT is_completed FROM public.action_items WHERE client_insight_id = 'client-1') = true,
    'resyncing does not wipe the agent''s completion');
SELECT t.ok(
    (SELECT completed_by FROM public.action_items WHERE client_insight_id = 'client-1') = 'agent:TestAgent',
    'completed_by survives the resync too');
SELECT t.ok(
    (SELECT count(*) FROM public.action_items WHERE note_id = 'a1000000-0000-4000-8000-000000000001') = 2,
    'exactly 2 rows exist: the legacy no-client-id row and the one upserted row (no duplicate)');

-- a second local item on the SAME note must not collide with the first (composite key, not just client id)
INSERT INTO public.action_items (note_id, user_id, client_insight_id, description, is_completed)
    VALUES ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'client-2', 'A second task', false)
    ON CONFLICT (note_id, client_insight_id) DO UPDATE SET description = excluded.description;
SELECT t.ok(
    (SELECT count(*) FROM public.action_items WHERE note_id = 'a1000000-0000-4000-8000-000000000001') = 3,
    'a different client_insight_id on the same note inserts a new row rather than colliding');

-- the SAME client_insight_id on a DIFFERENT note must also not collide (composite uniqueness)
INSERT INTO public.action_items (note_id, user_id, client_insight_id, description, is_completed)
    VALUES ('b1000000-0000-4000-8000-000000000001', 'bbbbbbbb-0000-4000-8000-000000000002', 'client-1', 'Bob''s own task', false)
    ON CONFLICT (note_id, client_insight_id) DO UPDATE SET description = excluded.description;
SELECT t.ok(
    (SELECT count(*) FROM public.action_items WHERE client_insight_id = 'client-1') = 2,
    'the same client id on a different note is a distinct row (uniqueness is per-note, not global)');

-- note_insights and reminders use the identical upsert pattern; prove it once each
INSERT INTO public.note_insights (note_id, user_id, client_insight_id, kind, text)
    VALUES ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'idea-1', 'idea', 'v1')
    ON CONFLICT (note_id, client_insight_id) DO UPDATE SET text = excluded.text;
INSERT INTO public.note_insights (note_id, user_id, client_insight_id, kind, text)
    VALUES ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'idea-1', 'idea', 'v2 - edited')
    ON CONFLICT (note_id, client_insight_id) DO UPDATE SET text = excluded.text;
SELECT t.ok(
    (SELECT count(*) FROM public.note_insights WHERE client_insight_id = 'idea-1') = 1
        AND (SELECT text FROM public.note_insights WHERE client_insight_id = 'idea-1') = 'v2 - edited',
    'note_insights upserts in place (one row, latest text) rather than duplicating');

INSERT INTO public.reminders (note_id, user_id, client_reminder_id, title, event_time, status)
    VALUES ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'rem-x', 'v1', now(), 'ACTIVE')
    ON CONFLICT (note_id, client_reminder_id) DO UPDATE SET title = excluded.title;
INSERT INTO public.reminders (note_id, user_id, client_reminder_id, title, event_time, status)
    VALUES ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'rem-x', 'v2 - edited', now(), 'ACTIVE')
    ON CONFLICT (note_id, client_reminder_id) DO UPDATE SET title = excluded.title;
SELECT t.ok(
    (SELECT count(*) FROM public.reminders WHERE client_reminder_id = 'rem-x') = 1
        AND (SELECT title FROM public.reminders WHERE client_reminder_id = 'rem-x') = 'v2 - edited',
    'reminders upserts in place too');

-- ------------------------------------------------------------------------------
-- 5. Migration 08 field parity.
--
--    These cover the three things that made reminders and decisions invisible or
--    untrustworthy to an agent: whole categories dropped by the upcoming filter,
--    review state that never left the device, and archived insights that still
--    read as open.
-- ------------------------------------------------------------------------------

-- 5a. An undated MILESTONE is a live commitment, not noise. The pre-08 predicate
--     required event_time IS NOT NULL and erased every target date without a clock
--     time - exactly the rows the user could see in the app but no agent could.
INSERT INTO public.reminders (note_id, user_id, client_reminder_id, title, event_time, type, status, needs_review)
    VALUES ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001',
            'r-undated', 'Undated profit target', NULL, 'MILESTONE', 'ACTIVE', false);

SET LOCAL ROLE anon;

SELECT t.ok(
    'Undated profit target' = ANY (SELECT title FROM public.mcp_list_reminders(t.h('keyA'), true, 20)),
    'an undated milestone IS returned under upcoming_only (migration 08 fix)');

SELECT t.ok(
    NOT ('Past meeting' = ANY (SELECT title FROM public.mcp_list_reminders(t.h('keyA'), true, 20))),
    'a past-dated reminder is still excluded under upcoming_only');

SELECT t.ok(
    NOT ('Dismissed future' = ANY (SELECT title FROM public.mcp_list_reminders(t.h('keyA'), true, 20))),
    'a dismissed future reminder is still excluded under upcoming_only');

-- 5b. Review state round-trips as three states, and NULL stays NULL. Defaulting
--     it either way would assert something untrue about pre-08 rows.
SELECT t.ok(
    (SELECT needs_review FROM public.mcp_list_reminders(t.h('keyA'), true, 20)
      WHERE title = 'Undated profit target') IS FALSE,
    'needs_review = false round-trips through the RPC');

SELECT t.ok(
    (SELECT needs_review FROM public.mcp_list_reminders(t.h('keyA'), false, 20)
      WHERE title = 'Future meeting') IS NULL,
    'a pre-parity reminder keeps needs_review = NULL (unknown, not confirmed)');

RESET ROLE;

UPDATE public.reminders
   SET needs_review = true, time_precision = 'UNKNOWN', local_date = '2026-11-20', linked_task_id = 'task-7'
 WHERE client_reminder_id = 'r-undated';

SET LOCAL ROLE anon;

SELECT t.ok(
    (SELECT needs_review AND time_precision = 'UNKNOWN' AND local_date = '2026-11-20' AND linked_task_id = 'task-7'
       FROM public.mcp_list_reminders(t.h('keyA'), true, 20) WHERE title = 'Undated profit target'),
    'needs_review/time_precision/local_date/linked_task_id all surface through the RPC');

RESET ROLE;

-- 5c. Insight lifecycle: archived is hidden by default, visible on request, and
--     the provenance links survive the round trip.
UPDATE public.note_insights SET status = 'ARCHIVED', archived_at = now()
 WHERE client_insight_id = 'i1';
UPDATE public.note_insights SET source_insight_id = 'src-idea', source_reminder_id = 'src-rem'
 WHERE client_insight_id = 'i2';

SET LOCAL ROLE anon;

SELECT t.ok(
    (SELECT array_agg(text) FROM public.mcp_list_insights(t.h('keyA'), 'all', false, 20)) = ARRAY['Alpha decision public'],
    'an archived insight is hidden by default');

SELECT t.ok(
    (SELECT array_agg(text ORDER BY text) FROM public.mcp_list_insights(t.h('keyA'), 'all', true, 20))
        = ARRAY['Alpha decision public', 'Alpha idea public'],
    'include_archived = true brings the archived insight back');

SELECT t.ok(
    (SELECT status FROM public.mcp_list_insights(t.h('keyA'), 'all', true, 20) WHERE text = 'Alpha idea public') = 'ARCHIVED',
    'insight status surfaces through the RPC');

SELECT t.ok(
    (SELECT source_insight_id = 'src-idea' AND source_reminder_id = 'src-rem'
       FROM public.mcp_list_insights(t.h('keyA'), 'all', false, 20) WHERE text = 'Alpha decision public'),
    'provenance links surface so a derived task can be tied back to its source');

-- Cross-tenant isolation must still hold on every new column.
SELECT t.ok(
    (SELECT count(*) FROM public.mcp_list_insights(t.h('keyB'), 'all', true, 20)) = 1,
    'include_archived does not leak another user''s insights');

RESET ROLE;

-- 5d. status is constrained, so a typo cannot become a silent third state.
SELECT t.raises(
    $q$INSERT INTO public.note_insights (note_id, user_id, client_insight_id, kind, text, status)
       VALUES ('a1000000-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', 'i-bad', 'idea', 'x', 'NOPE')$q$,
    '23514', 'an unknown insight status is rejected by the CHECK constraint');

ROLLBACK;

\echo 'ALL INSIGHTS/REMINDERS/UPSERT TESTS PASSED'
