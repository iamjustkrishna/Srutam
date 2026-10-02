-- ==============================================================================
-- MCP RPC isolation & authorization tests
--
-- Dependency-free (no pgTAP): plain assertions that abort on the first failure.
-- Everything runs inside one transaction that is ROLLED BACK, so it is safe to run
-- against a scratch/local database (never run it against production).
--
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/mcp_rpc_isolation.sql
--
-- With the Supabase CLI:  supabase start && psql "postgresql://postgres:postgres@127.0.0.1:54322/postgres" \
--                            -v ON_ERROR_STOP=1 -f supabase/tests/mcp_rpc_isolation.sql
--
-- Requires migrations 01-04 applied. Simulates the MCP server: role `anon`, no JWT.
-- ==============================================================================

BEGIN;

CREATE SCHEMA t;
GRANT USAGE ON SCHEMA t TO PUBLIC;

CREATE FUNCTION t.ok(cond boolean, msg text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF cond IS NOT TRUE THEN
        RAISE EXCEPTION 'FAIL: %', msg;
    END IF;
    RAISE NOTICE 'ok - %', msg;
END;
$$;

-- Runs `stmt` and asserts it raises the given SQLSTATE.
CREATE FUNCTION t.raises(stmt text, expected_state text, msg text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE stmt;
    EXCEPTION WHEN OTHERS THEN
        IF SQLSTATE = expected_state THEN
            RAISE NOTICE 'ok - %', msg;
            RETURN;
        END IF;
        RAISE EXCEPTION 'FAIL: % (expected SQLSTATE %, got % / %)', msg, expected_state, SQLSTATE, SQLERRM;
    END;
    RAISE EXCEPTION 'FAIL: % (statement did not raise)', msg;
END;
$$;

-- helper: sha256 hex of a raw key string (matches the app and the Node client)
CREATE FUNCTION t.h(raw text) RETURNS text LANGUAGE sql IMMUTABLE AS
$$ SELECT encode(sha256(convert_to(raw, 'UTF8')), 'hex') $$;

-- ------------------------------------------------------------------------------
-- Fixtures (as the migration owner)
-- ------------------------------------------------------------------------------
INSERT INTO auth.users (id) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001'),  -- user A
    ('bbbbbbbb-0000-0000-0000-000000000002'),  -- user B (victim of cross-tenant attempts)
    ('cccccccc-0000-0000-0000-000000000003'),  -- user C (many notes)
    ('dddddddd-0000-0000-0000-000000000004');  -- user D (revoked key)

INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name, revoked_at) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', t.h('key-A'), 'srtm_live_aaaaaa...', 'A', NULL),
    ('bbbbbbbb-0000-0000-0000-000000000002', t.h('key-B'), 'srtm_live_bbbbbb...', 'B', NULL),
    ('cccccccc-0000-0000-0000-000000000003', t.h('key-C'), 'srtm_live_cccccc...', 'C', NULL),
    ('dddddddd-0000-0000-0000-000000000004', t.h('key-D'), 'srtm_live_dddddd...', 'D', now());

INSERT INTO public.notes (id, user_id, title, transcript, summary, duration_ms, is_private) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Alpha architecture', 'we discussed the alpha module in detail', 'alpha summary', 90000, false),
    ('a2000000-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'Secret alpha diary', 'private alpha thoughts', 'private summary', 1000, true),
    ('a3000000-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000001', '100% done', 'nothing', 'literal percent title', 2000, false),
    ('b1000000-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000002', 'Bravo alpha secrets', 'bravo owns this alpha', 'bravo summary', 5000, false);

INSERT INTO public.notes (user_id, title, transcript, summary)
SELECT 'cccccccc-0000-0000-0000-000000000003', 'C note ' || g, 'text ' || g, 'summary ' || g
FROM generate_series(1, 30) g;

INSERT INTO public.action_items (id, note_id, user_id, description) VALUES
    ('a1a10000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'A public task'),
    ('a2a20000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'A PRIVATE task'),
    ('b1b10000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000002', 'B task');

-- ------------------------------------------------------------------------------
-- 1. Surface: old vulnerable signatures gone; grants are default-deny
-- ------------------------------------------------------------------------------
SELECT t.ok(to_regprocedure('public.verify_srutam_api_key(text)') IS NULL, 'verify_srutam_api_key removed');
SELECT t.ok(to_regprocedure('public.mcp_search_notes(uuid,text,integer)') IS NULL, 'old uuid-first mcp_search_notes removed');
SELECT t.ok(to_regprocedure('public.mcp_get_note_detail(uuid,uuid)') IS NULL, 'old uuid-first mcp_get_note_detail removed');
SELECT t.ok(to_regprocedure('public.mcp_list_action_items(uuid,text,integer)') IS NULL, 'old uuid-first mcp_list_action_items removed');
SELECT t.ok(to_regprocedure('public.mcp_update_action_item(uuid,uuid,boolean,text)') IS NULL, 'old uuid-first mcp_update_action_item removed');
SELECT t.ok(to_regprocedure('public.mcp_append_agent_log(uuid,uuid,text,text)') IS NULL, 'old uuid-first mcp_append_agent_log removed');

SELECT t.ok(NOT has_schema_privilege('anon', 'private', 'USAGE'), 'anon has no USAGE on schema private');
SELECT t.ok(NOT has_function_privilege('anon', 'private.resolve_mcp_key(text)', 'EXECUTE'), 'anon cannot execute private.resolve_mcp_key');
SELECT t.ok(NOT has_function_privilege('authenticated', 'private.resolve_mcp_key(text)', 'EXECUTE'), 'authenticated cannot execute private.resolve_mcp_key');
SELECT t.ok(NOT has_function_privilege('anon', 'public.check_active_api_keys_limit()', 'EXECUTE'), 'anon cannot execute trigger function');

SELECT t.ok(has_function_privilege('anon', 'public.mcp_search_notes(text,text,integer)', 'EXECUTE'), 'anon can execute mcp_search_notes');
SELECT t.ok(has_function_privilege('anon', 'public.mcp_get_note_detail(text,uuid)', 'EXECUTE'), 'anon can execute mcp_get_note_detail');
SELECT t.ok(has_function_privilege('anon', 'public.mcp_list_action_items(text,text,integer)', 'EXECUTE'), 'anon can execute mcp_list_action_items');
SELECT t.ok(has_function_privilege('anon', 'public.mcp_update_action_item(text,uuid,boolean,text)', 'EXECUTE'), 'anon can execute mcp_update_action_item');
SELECT t.ok(has_function_privilege('anon', 'public.mcp_append_agent_log(text,uuid,text,text)', 'EXECUTE'), 'anon can execute mcp_append_agent_log');
SELECT t.ok(has_function_privilege('anon', 'public.mcp_cloud_status(text)', 'EXECUTE'), 'anon can execute mcp_cloud_status');
SELECT t.ok(NOT has_function_privilege('authenticated', 'public.mcp_search_notes(text,text,integer)', 'EXECUTE'), 'authenticated (phone JWT) is not a valid MCP caller');

SELECT t.ok(pg_get_function_result('public.mcp_search_notes(text,text,integer)'::regprocedure) NOT LIKE '%transcript%', 'search RPC does not return transcripts');
SELECT t.ok(pg_get_function_result('public.mcp_search_notes(text,text,integer)'::regprocedure) LIKE '%duration_ms%', 'search RPC returns duration_ms');

SELECT t.ok(
    (SELECT bool_and(p.prosecdef IS FALSE OR EXISTS (SELECT 1 FROM unnest(p.proconfig) c WHERE c LIKE 'search_path=%'))
       FROM pg_proc p JOIN pg_namespace ns ON ns.oid = p.pronamespace
      WHERE (ns.nspname = 'public' AND (p.proname LIKE 'mcp\_%' OR p.proname = 'check_active_api_keys_limit'))
         OR (ns.nspname = 'private')),
    'every SECURITY DEFINER function pins search_path');

-- ------------------------------------------------------------------------------
-- 2. From here on we behave like the MCP server: role anon, no JWT
-- ------------------------------------------------------------------------------
SET LOCAL ROLE anon;

SELECT t.ok((SELECT count(*) FROM public.notes) = 0, 'anon cannot read notes table directly (RLS)');
SELECT t.ok((SELECT count(*) FROM public.api_keys) = 0, 'anon cannot read api_keys table directly (RLS)');

-- 2a. bad credentials are rejected by EVERY rpc
SELECT t.raises($$SELECT * FROM public.mcp_search_notes('nonsense', '', 5)$$, '28000', 'search: malformed key rejected');
SELECT t.raises($$SELECT * FROM public.mcp_search_notes(t.h('no-such-key'), '', 5)$$, '28000', 'search: unknown key rejected');
SELECT t.raises($$SELECT * FROM public.mcp_search_notes(t.h('key-D'), '', 5)$$, '28000', 'search: revoked key rejected');
SELECT t.raises($$SELECT public.mcp_get_note_detail(t.h('no-such-key'), 'a1000000-0000-0000-0000-000000000001')$$, '28000', 'detail: unknown key rejected');
SELECT t.raises($$SELECT * FROM public.mcp_list_action_items(t.h('no-such-key'), 'all', 5)$$, '28000', 'list tasks: unknown key rejected');
SELECT t.raises($$SELECT public.mcp_update_action_item(t.h('no-such-key'), 'a1a10000-0000-0000-0000-000000000001', true, 'x')$$, '28000', 'update task: unknown key rejected');
SELECT t.raises($$SELECT public.mcp_append_agent_log(t.h('no-such-key'), 'a1000000-0000-0000-0000-000000000001', 'x', 'y')$$, '28000', 'append log: unknown key rejected');
SELECT t.raises($$SELECT public.mcp_cloud_status(t.h('no-such-key'))$$, '28000', 'status: unknown key rejected');
SELECT t.raises($$SELECT public.mcp_cloud_status(NULL)$$, '28000', 'status: NULL key rejected');

-- 2b. reads are scoped to the key's owner and honour is_private
SELECT t.ok(
    (SELECT array_agg(id ORDER BY id::text) FROM public.mcp_search_notes(t.h('key-A'), '', 25))
      = ARRAY['a1000000-0000-0000-0000-000000000001', 'a3000000-0000-0000-0000-000000000003']::uuid[],
    'key A lists only A''s public notes (no B note, no private note)');
SELECT t.ok(
    (SELECT duration_ms FROM public.mcp_search_notes(t.h('key-A'), '', 25) WHERE id = 'a1000000-0000-0000-0000-000000000001') = 90000,
    'duration_ms is returned (fixes NaN in list_recent_notes)');
SELECT t.ok(
    (SELECT array_agg(id) FROM public.mcp_search_notes(t.h('key-A'), 'alpha', 25)) = ARRAY['a1000000-0000-0000-0000-000000000001']::uuid[],
    'search "alpha" as A finds only A''s public alpha note (not B''s, not A''s private one)');
SELECT t.ok(
    (SELECT array_agg(id) FROM public.mcp_search_notes(t.h('key-B'), 'alpha', 25)) = ARRAY['b1000000-0000-0000-0000-000000000001']::uuid[],
    'search "alpha" as B finds only B''s note');
SELECT t.ok(
    (SELECT array_agg(id) FROM public.mcp_search_notes(t.h('key-A'), '%', 25)) = ARRAY['a3000000-0000-0000-0000-000000000003']::uuid[],
    'LIKE wildcard "%" is matched literally, not as match-everything');
SELECT t.ok((SELECT count(*) FROM public.mcp_search_notes(t.h('key-C'), '', 9999)) = 25, 'p_limit is clamped to 25 server-side');
SELECT t.ok((SELECT count(*) FROM public.mcp_search_notes(t.h('key-C'), '', -5)) = 1, 'p_limit below 1 is clamped to 1');

-- 2c. note detail
SELECT t.ok(public.mcp_get_note_detail(t.h('key-A'), 'b1000000-0000-0000-0000-000000000001') IS NULL, 'A cannot read B''s note by id');
SELECT t.ok(public.mcp_get_note_detail(t.h('key-A'), 'a2000000-0000-0000-0000-000000000002') IS NULL, 'A cannot read A''s own PRIVATE note through MCP');
SELECT t.ok((public.mcp_get_note_detail(t.h('key-A'), 'a1000000-0000-0000-0000-000000000001') -> 'note' ->> 'title') = 'Alpha architecture', 'A can read own public note');
SELECT t.ok(NOT ((public.mcp_get_note_detail(t.h('key-A'), 'a1000000-0000-0000-0000-000000000001') -> 'note') ?| ARRAY['embedding', 'user_id']), 'note detail leaks neither embedding nor user_id');
SELECT t.ok(jsonb_array_length(public.mcp_get_note_detail(t.h('key-A'), 'a1000000-0000-0000-0000-000000000001') -> 'actionItems') = 1, 'note detail includes its task');

-- 2d. action items honour is_private and tenancy
SELECT t.ok(
    (SELECT array_agg(id) FROM public.mcp_list_action_items(t.h('key-A'), 'all', 50)) = ARRAY['a1a10000-0000-0000-0000-000000000001']::uuid[],
    'task list hides tasks of private notes and other users'' tasks');
SELECT t.raises($$SELECT * FROM public.mcp_list_action_items(t.h('key-A'), 'bogus', 5)$$, '22023', 'invalid status is rejected');

-- status counts only what MCP may see (checked BEFORE any mutation below)
SELECT t.ok((public.mcp_cloud_status(t.h('key-A')) ->> 'noteCount')::int = 2, 'status noteCount excludes private notes');
SELECT t.ok((public.mcp_cloud_status(t.h('key-A')) ->> 'pendingActionsCount')::int = 1, 'status pending count excludes private-note tasks');
SELECT t.ok((public.mcp_cloud_status(t.h('key-A')) ->> 'userId') = 'aaaaaaaa-0000-0000-0000-000000000001', 'status returns the key owner');

-- 2e. writes: cross-tenant and private-note writes are no-ops / errors
SELECT t.ok(public.mcp_update_action_item(t.h('key-A'), 'b1b10000-0000-0000-0000-000000000001', true, 'evil') IS NULL, 'A cannot complete B''s task');
SELECT t.ok(public.mcp_update_action_item(t.h('key-A'), 'a2a20000-0000-0000-0000-000000000002', true, 'evil') IS NULL, 'A cannot complete a task of A''s PRIVATE note via MCP');
SELECT t.raises($$SELECT public.mcp_append_agent_log(t.h('key-A'), 'b1000000-0000-0000-0000-000000000001', 'evil', 'pwn')$$, 'P0002', 'A cannot log on B''s note');
SELECT t.raises($$SELECT public.mcp_append_agent_log(t.h('key-A'), 'a2000000-0000-0000-0000-000000000002', 'evil', 'pwn')$$, 'P0002', 'A cannot log on A''s private note');
SELECT t.raises($$SELECT public.mcp_append_agent_log(t.h('key-A'), 'a1000000-0000-0000-0000-000000000001', 'x', '   ')$$, '22023', 'blank log message rejected');

-- 2f. legitimate write + attribution hardening
SELECT t.ok(
    (public.mcp_update_action_item(t.h('key-A'), 'a1a10000-0000-0000-0000-000000000001', true, 'Cursor<script>') ->> 'completed_by') = 'agent:Cursorscript',
    'agent name is sanitised (charset) and prefixed');
SELECT t.ok(
    (public.mcp_update_action_item(t.h('key-A'), 'a1a10000-0000-0000-0000-000000000001', true, 'user') ->> 'completed_by') = 'agent:user',
    'an agent named "user" is still attributed as agent:user');
SELECT t.ok(
    (public.mcp_update_action_item(t.h('key-A'), 'a1a10000-0000-0000-0000-000000000001', true, E'  \n ') ->> 'completed_by') = 'agent:AI Agent',
    'blank agent name falls back to "AI Agent"');
SELECT t.ok(
    (public.mcp_update_action_item(t.h('key-A'), 'a1a10000-0000-0000-0000-000000000001', false, 'Cursor') ->> 'completed_by') IS NULL,
    'reopening clears completed_by');
SELECT t.ok(
    NOT ((public.mcp_update_action_item(t.h('key-A'), 'a1a10000-0000-0000-0000-000000000001', true, 'Cursor')) ? 'user_id'),
    'update result does not expose user_id');
SELECT t.ok(
    (public.mcp_append_agent_log(t.h('key-A'), 'a1000000-0000-0000-0000-000000000001', 'Cursor', 'implemented in abc123') ->> 'agent_name') = 'Cursor',
    'A can log on own public note');

-- ------------------------------------------------------------------------------
-- 3. Effects verified as the owner (RLS bypassed): unauthorized writes changed nothing
-- ------------------------------------------------------------------------------
RESET ROLE;

SELECT t.ok((SELECT is_completed FROM public.action_items WHERE id = 'b1b10000-0000-0000-0000-000000000001') = false, 'B''s task is untouched');
SELECT t.ok((SELECT is_completed FROM public.action_items WHERE id = 'a2a20000-0000-0000-0000-000000000002') = false, 'A''s private task is untouched');
SELECT t.ok((SELECT count(*) FROM public.agent_logs WHERE note_id = 'b1000000-0000-0000-0000-000000000001') = 0, 'no log was written on B''s note');
SELECT t.ok((SELECT count(*) FROM public.agent_logs WHERE note_id = 'a2000000-0000-0000-0000-000000000002') = 0, 'no log was written on A''s private note');

-- last_used_at is throttled to one write per 5 minutes
UPDATE public.api_keys SET last_used_at = now() - interval '1 minute' WHERE key_hash = t.h('key-B');
SET LOCAL ROLE anon;
SELECT public.mcp_cloud_status(t.h('key-B'));
RESET ROLE;
SELECT t.ok((SELECT last_used_at < now() - interval '30 seconds' FROM public.api_keys WHERE key_hash = t.h('key-B')), 'last_used_at not rewritten within 5 minutes');
UPDATE public.api_keys SET last_used_at = now() - interval '10 minutes' WHERE key_hash = t.h('key-B');
SET LOCAL ROLE anon;
SELECT public.mcp_cloud_status(t.h('key-B'));
RESET ROLE;
SELECT t.ok((SELECT last_used_at > now() - interval '30 seconds' FROM public.api_keys WHERE key_hash = t.h('key-B')), 'last_used_at refreshed after 5 minutes');

-- revocation takes effect on the very next call (no client-side auth cache anymore)
SET LOCAL ROLE anon;
SELECT t.ok(public.mcp_cloud_status(t.h('key-B')) IS NOT NULL, 'key B works before revocation');
RESET ROLE;
UPDATE public.api_keys SET revoked_at = now() WHERE key_hash = t.h('key-B');
SET LOCAL ROLE anon;
SELECT t.raises($$SELECT public.mcp_cloud_status(t.h('key-B'))$$, '28000', 'revoked key is rejected immediately');
RESET ROLE;

-- 3-active-key quota still enforced (trigger rewritten with search_path + advisory lock)
INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', t.h('key-A2'), 'p', 'A2'),
    ('aaaaaaaa-0000-0000-0000-000000000001', t.h('key-A3'), 'p', 'A3');
SELECT t.raises(
    $$INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', t.h('key-A4'), 'p', 'A4')$$,
    'P0001', '4th active key for a user is rejected');

ROLLBACK;

\echo 'ALL MCP RPC ISOLATION TESTS PASSED'
