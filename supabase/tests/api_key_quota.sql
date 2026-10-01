-- ==============================================================================
-- API key quota and unique-name tests (needs migrations 01-05). Rolled back; scratch DBs only.
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/api_key_quota.sql
-- Exercises the path the phone uses: role `authenticated` with a JWT subject, subject to RLS.
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

CREATE FUNCTION t.h(raw text) RETURNS text LANGUAGE sql IMMUTABLE AS
$$ SELECT encode(sha256(convert_to(raw, 'UTF8')), 'hex') $$;

GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA t TO PUBLIC;

INSERT INTO auth.users (id) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001'),
    ('bbbbbbbb-0000-0000-0000-000000000002');

-- The trigger exists (the reason "more than three" was possible when migration 03 was missing)
SELECT t.ok(EXISTS (
    SELECT 1 FROM pg_trigger
    WHERE tgrelid = 'public.api_keys'::regclass AND tgname = 'tr_check_active_api_keys_limit' AND NOT tgisinternal
), 'quota trigger exists');

-- Act as the phone: authenticated role, JWT subject = user A
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', 'aaaaaaaa-0000-0000-0000-000000000001', true);

INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name)
VALUES ('aaaaaaaa-0000-0000-0000-000000000001', t.h('a1'), 'p', 'Cursor IDE');
SELECT t.ok(true, 'first key created');

SELECT t.raises(
    $$INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', t.h('a1dup'), 'p', 'Cursor IDE')$$,
    '23505', 'exact duplicate name rejected');
SELECT t.raises(
    $$INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', t.h('a1case'), 'p', 'cursor ide')$$,
    '23505', 'duplicate differing by case rejected');
SELECT t.raises(
    $$INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', t.h('a1sp'), 'p', '  Cursor IDE  ')$$,
    '23505', 'duplicate differing by surrounding spaces rejected');
SELECT t.raises(
    $$INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', t.h('blank'), 'p', '   ')$$,
    '23514', 'blank name rejected');
SELECT t.raises(
    $$INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', t.h('long'), 'p', repeat('x', 61))$$,
    '23514', 'name over 60 characters rejected');

INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', t.h('a2'), 'p', 'Windsurf'),
    ('aaaaaaaa-0000-0000-0000-000000000001', t.h('a3'), 'p', 'Zed');
SELECT t.ok((SELECT count(*) FROM public.api_keys WHERE revoked_at IS NULL) = 3, 'three active keys');

SELECT t.raises(
    $$INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', t.h('a4'), 'p', 'Fourth')$$,
    'P0001', '4th active key rejected (KEY_LIMIT_REACHED)');

-- The app revokes with PATCH revoked_at; a revoked key frees both the slot and the name
UPDATE public.api_keys SET revoked_at = now() WHERE key_hash = t.h('a1');
INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name)
VALUES ('aaaaaaaa-0000-0000-0000-000000000001', t.h('a5'), 'p', 'Cursor IDE');
SELECT t.ok(true, 'name and slot reusable after revoke');

-- Un-revoking must respect the limit too (active = 3 again)
SELECT t.raises(
    $$UPDATE public.api_keys SET revoked_at = NULL WHERE key_hash = t.h('a1')$$,
    'P0001', 'un-revoking beyond 3 rejected');

-- Renaming / touching an active row must not trip the quota
UPDATE public.api_keys SET last_used_at = now() WHERE key_hash = t.h('a2');
UPDATE public.api_keys SET name = 'Windsurf 2' WHERE key_hash = t.h('a2');
SELECT t.ok(true, 'updating an active key at the limit is allowed');

-- Revoking an already-revoked key and revoking active keys is unaffected
UPDATE public.api_keys SET revoked_at = now() WHERE key_hash = t.h('a2');
SELECT t.ok((SELECT count(*) FROM public.api_keys WHERE revoked_at IS NULL) = 2, 'revoking lowers the active count');

-- Another user has an independent quota and namespace
SELECT set_config('request.jwt.claim.sub', 'bbbbbbbb-0000-0000-0000-000000000002', true);
INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name)
VALUES ('bbbbbbbb-0000-0000-0000-000000000002', t.h('b1'), 'p', 'Cursor IDE');
SELECT t.ok(true, 'same name allowed for a different user');

-- The phone cannot create a key for someone else (RLS with-check), and the trigger cannot be called directly
SELECT t.raises(
    $$INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name) VALUES ('aaaaaaaa-0000-0000-0000-000000000001', t.h('evil'), 'p', 'Evil')$$,
    '42501', 'cannot insert a key for another user');
SELECT t.ok(NOT has_function_privilege('authenticated', 'public.check_active_api_keys_limit()', 'EXECUTE'), 'trigger function not callable by clients');

RESET ROLE;
ROLLBACK;

\echo 'ALL API KEY QUOTA TESTS PASSED'
