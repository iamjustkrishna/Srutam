-- ==============================================================================
-- QR pairing tests: authorization split, single-use codes, brute-force lockout,
-- quota interaction and key replacement.
--
-- Dependency-free (no pgTAP). Everything runs in one transaction that is ROLLED
-- BACK, so it is safe on a scratch database. NEVER run against production.
--
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/mcp_pairing.sql
--
-- Requires migrations 01-06.
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

-- Impersonate a signed-in phone: `authenticated` + a JWT sub claim.
CREATE FUNCTION t.as_user(p_user uuid) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    PERFORM set_config('request.jwt.claim.sub', p_user::text, true);
    EXECUTE 'SET LOCAL ROLE authenticated';
END;
$$;

CREATE FUNCTION t.as_cli() RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    PERFORM set_config('request.jwt.claim.sub', '', true);
    EXECUTE 'SET LOCAL ROLE anon';
END;
$$;

-- Codes are produced while acting as anon and read back while acting as the phone, so they
-- cannot live in a TEMP table owned by postgres. SECURITY DEFINER helpers keep them reachable
-- from every role without granting anything on the pairing tables themselves.
CREATE TABLE t.codes (name text PRIMARY KEY, code text NOT NULL, expires_at timestamptz);

CREATE FUNCTION t.save(p_name text, p_result jsonb) RETURNS text
LANGUAGE sql SECURITY DEFINER AS $$
    INSERT INTO t.codes (name, code, expires_at)
    VALUES (p_name, p_result ->> 'code', (p_result ->> 'expiresAt')::timestamptz)
    ON CONFLICT (name) DO UPDATE SET code = excluded.code, expires_at = excluded.expires_at
    RETURNING code;
$$;

CREATE FUNCTION t.code(p_name text) RETURNS text
LANGUAGE sql STABLE SECURITY DEFINER AS $$ SELECT code FROM t.codes WHERE name = p_name $$;

CREATE FUNCTION t.expires(p_name text) RETURNS timestamptz
LANGUAGE sql STABLE SECURITY DEFINER AS $$ SELECT expires_at FROM t.codes WHERE name = p_name $$;

-- ------------------------------------------------------------------------------
-- Fixtures
-- ------------------------------------------------------------------------------
INSERT INTO auth.users (id, email) VALUES
    ('aaaaaaaa-0000-4000-8000-000000000001', 'alice@example.com'),
    ('bbbbbbbb-0000-4000-8000-000000000002', 'bob@example.com'),
    ('cccccccc-0000-4000-8000-000000000003', 'carol@example.com'),
    ('dddddddd-0000-4000-8000-000000000004', 'dave@example.com');

-- ------------------------------------------------------------------------------
-- 1. Grants: the CLI and the phone must not be able to reach each other's calls
-- ------------------------------------------------------------------------------
SELECT t.ok(    has_function_privilege('anon',          'public.mcp_pair_start(text,text,text,text,text,text)', 'EXECUTE'), 'anon may start a pairing');
SELECT t.ok(    has_function_privilege('anon',          'public.mcp_pair_status(text)',    'EXECUTE'), 'anon may poll status');
SELECT t.ok(    has_function_privilege('anon',          'public.mcp_pair_cancel(text)',    'EXECUTE'), 'anon may cancel');
SELECT t.ok(    has_function_privilege('anon',          'public.mcp_revoke_self(text)',    'EXECUTE'), 'anon may revoke its own key');
SELECT t.ok(NOT has_function_privilege('anon',          'public.mcp_pair_preview(text)',   'EXECUTE'), 'anon may NOT preview a code');
SELECT t.ok(NOT has_function_privilege('anon',          'public.mcp_pair_approve(text,text)', 'EXECUTE'), 'anon may NOT approve');
SELECT t.ok(    has_function_privilege('authenticated', 'public.mcp_pair_preview(text)',   'EXECUTE'), 'phone may preview');
SELECT t.ok(    has_function_privilege('authenticated', 'public.mcp_pair_approve(text,text)', 'EXECUTE'), 'phone may approve');
SELECT t.ok(NOT has_function_privilege('authenticated', 'public.mcp_pair_start(text,text,text,text,text,text)', 'EXECUTE'), 'phone may NOT start a pairing');
SELECT t.ok(NOT has_schema_privilege('anon',          'private', 'USAGE'), 'anon has no access to schema private');
SELECT t.ok(NOT has_schema_privilege('authenticated', 'private', 'USAGE'), 'authenticated has no access to schema private');

SELECT t.ok(
    (SELECT bool_and(EXISTS (SELECT 1 FROM unnest(p.proconfig) c WHERE c LIKE 'search_path=%'))
       FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
      WHERE p.prosecdef AND (n.nspname = 'private' OR (n.nspname = 'public' AND p.proname LIKE 'mcp\_pair%'))),
    'every pairing SECURITY DEFINER function pins search_path');

-- ------------------------------------------------------------------------------
-- 2. Happy path
-- ------------------------------------------------------------------------------
SELECT t.as_cli();
SELECT t.save('a', public.mcp_pair_start(t.h('keyA'), 'srtm_live_aaaaaa...', 'DESKTOP-KRISH', 'Windows', '1.4.0', NULL));

SELECT t.ok(t.code('a') ~ '^[0-9A-HJKMNP-TV-Z]{8}$', 'code is 8 unambiguous base32 chars');
SELECT t.ok(t.expires('a') > now(), 'code has a future expiry');
SELECT t.ok((SELECT public.mcp_pair_status(t.h('keyA')) ->> 'status') = 'pending', 'status starts pending');
SELECT t.ok((SELECT public.mcp_pair_status(t.h('keyA')) ->> 'accountHint') IS NULL, 'no account is revealed before approval');

SELECT t.as_user('aaaaaaaa-0000-4000-8000-000000000001');
SELECT t.ok((SELECT public.mcp_pair_preview(t.code('a')) ->> 'label') = 'DESKTOP-KRISH', 'preview returns the machine label');
SELECT t.ok((SELECT public.mcp_pair_approve(t.code('a'), 'My Laptop') ->> 'ok')::boolean, 'approve succeeds');

RESET ROLE;
SELECT t.ok((SELECT count(*) FROM public.api_keys k
              WHERE k.key_hash = t.h('keyA')
                AND k.user_id = 'aaaaaaaa-0000-4000-8000-000000000001'
                AND k.name = 'My Laptop' AND k.revoked_at IS NULL) = 1,
            'an active key now exists for the approving user with the chosen name');

SELECT t.as_cli();
SELECT t.ok((SELECT public.mcp_pair_status(t.h('keyA')) ->> 'status') = 'approved', 'CLI sees approved');
SELECT t.ok((SELECT public.mcp_pair_status(t.h('keyA')) ->> 'accountHint') = 'a***@example.com', 'account hint is masked, not the full address');
SELECT t.ok((SELECT public.mcp_pair_status(t.h('keyA')) ->> 'keyName') = 'My Laptop', 'CLI learns the key name');

-- the paired key really works against the MCP surface
SELECT t.ok(public.mcp_cloud_status(t.h('keyA')) ->> 'userId' = 'aaaaaaaa-0000-4000-8000-000000000001',
            'the paired key authenticates against the MCP RPCs');

-- ------------------------------------------------------------------------------
-- 3. Single use, expiry, and wrong codes
-- ------------------------------------------------------------------------------
SELECT t.as_user('aaaaaaaa-0000-4000-8000-000000000001');
SELECT t.ok((public.mcp_pair_approve(t.code('a'), NULL) ->> 'error') = 'invalid', 'a code cannot be approved twice');
SELECT t.ok((public.mcp_pair_preview(t.code('a')) ->> 'error') = 'invalid', 'an approved code can no longer be previewed');

SELECT t.as_cli();
SELECT t.save('exp', public.mcp_pair_start(t.h('keyExpired'), 'p', 'OLD-PC', 'Linux', '1.4.0', NULL));
RESET ROLE;
UPDATE private.mcp_pairings SET expires_at = now() - interval '1 second' WHERE key_hash = t.h('keyExpired');
SELECT t.as_cli();
SELECT t.ok((SELECT public.mcp_pair_status(t.h('keyExpired')) ->> 'status') = 'expired', 'CLI sees an expired code as expired');
SELECT t.as_user('aaaaaaaa-0000-4000-8000-000000000001');
SELECT t.ok((public.mcp_pair_preview(t.code('exp')) ->> 'error') = 'invalid', 'an expired code cannot be previewed');

-- cancelled codes are dead too
SELECT t.as_cli();
SELECT t.save('cancel', public.mcp_pair_start(t.h('keyCancel'), 'p', 'CANCEL-PC', 'macOS', '1.4.0', NULL));
SELECT t.ok((public.mcp_pair_cancel(t.h('keyCancel')) ->> 'cancelled')::boolean, 'cancel works');
SELECT t.as_user('aaaaaaaa-0000-4000-8000-000000000001');
SELECT t.ok((public.mcp_pair_preview(t.code('cancel')) ->> 'error') = 'invalid', 'a cancelled code cannot be previewed');

-- ------------------------------------------------------------------------------
-- 4. Brute force: 10 bad codes locks the user out, and a good code is then refused
-- ------------------------------------------------------------------------------
SELECT t.as_cli();
SELECT t.save('bob', public.mcp_pair_start(t.h('keyBob'), 'p', 'BOB-PC', 'Windows', '1.4.0', NULL));

SELECT t.as_user('cccccccc-0000-4000-8000-000000000003');
DO $$
DECLARE i int;
BEGIN
    FOR i IN 1..10 LOOP
        -- A miss returns {ok:false}; it must NOT raise, or the attempt row would roll back.
        PERFORM public.mcp_pair_preview('ZZZZZZZ' || i::text);
    END LOOP;
END;
$$;

RESET ROLE;   -- reading private.* needs the owner, not the phone role
SELECT t.ok((SELECT count(*) FROM private.mcp_pair_attempts
              WHERE user_id = 'cccccccc-0000-4000-8000-000000000003') = 10,
            'every failed attempt is actually persisted (not rolled back by an exception)');
SELECT t.as_user('cccccccc-0000-4000-8000-000000000003');
SELECT t.ok((public.mcp_pair_preview(t.code('bob')) ->> 'error') = 'rate_limited',
            'after 10 misses the user is rate limited even with a valid code');
SELECT t.ok((public.mcp_pair_preview(t.code('bob')) ->> 'retryAfterSeconds')::int BETWEEN 1 AND 600,
            'the lockout reports a sane retry delay');
SELECT t.ok((public.mcp_pair_approve(t.code('bob'), 'x') ->> 'error') = 'rate_limited',
            'approve is rate limited too, so the lockout cannot be bypassed');

-- a different user is unaffected
SELECT t.as_user('bbbbbbbb-0000-4000-8000-000000000002');
SELECT t.ok((SELECT public.mcp_pair_preview(t.code('bob')) ->> 'label') = 'BOB-PC',
            'the lockout is per user, not global');

-- ------------------------------------------------------------------------------
-- 5. Unauthenticated and malformed input
-- ------------------------------------------------------------------------------
SELECT t.as_cli();
SELECT t.raises('SELECT public.mcp_pair_start(''nothex'', ''p'', ''X'', NULL, NULL, NULL)', '22023', 'a malformed key hash is rejected');
SELECT t.raises(format('SELECT public.mcp_pair_start(%L, ''p'', ''X'', NULL, NULL, NULL)', t.h('keyA')),
                '23505', 'a hash that is already a live key cannot be paired again');
SELECT t.ok((public.mcp_pair_status('nothex') ->> 'status') = 'unknown', 'status of a malformed hash is unknown');
SELECT t.ok((public.mcp_pair_status(t.h('never-seen')) ->> 'status') = 'unknown', 'status of an unknown hash is unknown');

RESET ROLE;
SET LOCAL ROLE authenticated;  -- authenticated but with no JWT sub => auth.uid() is null
SELECT t.raises('SELECT public.mcp_pair_preview(''ABCDEFGH'')', '28000', 'preview requires a signed-in user');
SELECT t.raises('SELECT public.mcp_pair_approve(''ABCDEFGH'', NULL)', '28000', 'approve requires a signed-in user');

-- ------------------------------------------------------------------------------
-- 6. Quota and key replacement (migration 05 still governs)
-- ------------------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name) VALUES
    ('bbbbbbbb-0000-4000-8000-000000000002', t.h('bob1'), 'p', 'Bob One'),
    ('bbbbbbbb-0000-4000-8000-000000000002', t.h('bob2'), 'p', 'Bob Two');
-- bob now has bob1, bob2 (+ keyBob is still only a pending pairing)

SELECT t.as_user('bbbbbbbb-0000-4000-8000-000000000002');
SELECT t.ok((public.mcp_pair_approve(t.code('bob'), 'Bob Three') ->> 'ok')::boolean,
            'third key pairs fine');

SELECT t.as_cli();
SELECT t.save('bob4', public.mcp_pair_start(t.h('keyBob4'), 'p', 'BOB-PC-2', 'Windows', '1.4.0', NULL));
SELECT t.as_user('bbbbbbbb-0000-4000-8000-000000000002');
SELECT t.raises(format('SELECT public.mcp_pair_approve(%L, ''Bob Four'')', t.code('bob4')),
                'P0001', 'a 4th active key is refused by the quota trigger');

-- replacing an existing key frees the slot in the same transaction
SELECT t.as_cli();
SELECT t.save('bob_replace', public.mcp_pair_start(t.h('keyBobNew'), 'p', 'BOB-PC', 'Windows', '1.4.0', t.h('bob1')));
SELECT t.as_user('bbbbbbbb-0000-4000-8000-000000000002');
SELECT t.ok((public.mcp_pair_approve(t.code('bob_replace'), 'Bob One') ->> 'ok')::boolean,
            're-pairing the same machine swaps its key instead of hitting the limit');
RESET ROLE;
SELECT t.ok((SELECT revoked_at IS NOT NULL FROM public.api_keys WHERE key_hash = t.h('bob1')), 'the replaced key is revoked');
SELECT t.ok((SELECT revoked_at IS NULL FROM public.api_keys WHERE key_hash = t.h('keyBobNew')), 'the new key is active');
SELECT t.ok((SELECT count(*) FROM public.api_keys
              WHERE user_id = 'bbbbbbbb-0000-4000-8000-000000000002' AND revoked_at IS NULL) = 3,
            'bob is still within the 3-key quota');

-- a user cannot replace someone else's key
SELECT t.as_cli();
SELECT t.save('evil', public.mcp_pair_start(t.h('keyEvil'), 'p', 'EVIL-PC', 'Linux', '1.4.0', t.h('keyA')));
SELECT t.as_user('dddddddd-0000-4000-8000-000000000004');
-- carol has 0 keys, so the insert itself succeeds; what must NOT happen is alice's key being revoked
SELECT t.ok((public.mcp_pair_approve(t.code('evil'), 'Evil') ->> 'ok')::boolean, 'carol pairs her own machine');
RESET ROLE;
SELECT t.ok((SELECT revoked_at IS NULL FROM public.api_keys WHERE key_hash = t.h('keyA')),
            'another user''s key is NOT revoked by a crafted replaces_key_hash');

-- ------------------------------------------------------------------------------
-- 7. revoke_self
-- ------------------------------------------------------------------------------
SELECT t.as_cli();
SELECT t.ok((public.mcp_revoke_self(t.h('keyA')) ->> 'revoked')::boolean, 'a key can revoke itself');
SELECT t.ok(NOT (public.mcp_revoke_self(t.h('keyA')) ->> 'revoked')::boolean, 'revoking twice is a no-op');
SELECT t.raises(format('SELECT public.mcp_cloud_status(%L)', t.h('keyA')), '28000', 'the revoked key no longer authenticates');

-- ------------------------------------------------------------------------------
-- 8. Input sanitising
-- ------------------------------------------------------------------------------
SELECT t.as_cli();
SELECT t.save('dirty', public.mcp_pair_start(t.h('keyDirty'), 'p', E'BAD<script>\nname' || repeat('x', 200), 'Win<>dows', '9.9.9;rm -rf', NULL));
SELECT t.as_user('dddddddd-0000-4000-8000-000000000004');
RESET ROLE;
SELECT t.ok((SELECT label !~ '[<>]' AND length(label) <= 60 FROM private.mcp_pairings WHERE key_hash = t.h('keyDirty')),
            'the machine label is stripped of unsafe characters and bounded');
SELECT t.ok((SELECT client_version = '9.9.9rm-rf' FROM private.mcp_pairings WHERE key_hash = t.h('keyDirty')),
            'the client version is sanitised');

-- lowercase / dashed codes entered by hand still match
SELECT t.as_cli();
SELECT t.save('case', public.mcp_pair_start(t.h('keyCase'), 'p', 'CASE-PC', 'Linux', '1.4.0', NULL));
SELECT t.as_user('dddddddd-0000-4000-8000-000000000004');
SELECT t.ok(
    (SELECT public.mcp_pair_preview(
        lower(substr(t.code('case'), 1, 4)) || '-' ||
        lower(substr(t.code('case'), 5, 4))) ->> 'label') = 'CASE-PC',
    'a hand-typed code is accepted lowercase and with a dash');

ROLLBACK;

\echo 'ALL QR PAIRING TESTS PASSED'
