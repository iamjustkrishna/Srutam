-- ==============================================================================
-- QR pairing: connect a computer without ever showing or pasting a key
-- File: supabase/migrations/20261002_06_mcp_qr_pairing.sql
--
-- MODEL: the computer generates its own API key and keeps it. Only sha256(key) is
-- ever sent. The phone, after the user confirms, registers that hash under their
-- account. The secret never travels - not in the QR, not through the phone, not
-- through this database.
--
--   CLI  (role anon)         : mcp_pair_start -> code, then polls mcp_pair_status
--   Phone (role authenticated): mcp_pair_preview(code) -> mcp_pair_approve(code, name)
--
-- The QR carries only `srutam://pair?v=1&c=<code>` - a single-use, 5-minute pointer
-- to a pending row. Someone who photographs it can only attach THEIR account to the
-- computer, which the CLI's own [Y/n] confirmation (it prints the masked approving
-- account) is there to catch.
--
-- Additive and idempotent: nothing existing changes.
-- ==============================================================================

BEGIN;

CREATE SCHEMA IF NOT EXISTS private;
REVOKE ALL ON SCHEMA private FROM PUBLIC, anon, authenticated;

-- ------------------------------------------------------------------------------
-- 1. Tables (in `private`, so PostgREST never exposes them and no role can read them)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS private.mcp_pairings (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code               text NOT NULL UNIQUE,
    key_hash           text NOT NULL UNIQUE,
    key_prefix         text NOT NULL,
    label              text NOT NULL,
    platform           text,
    client_version     text,
    replaces_key_hash  text,
    status             text NOT NULL DEFAULT 'pending'
                       CHECK (status IN ('pending', 'approved', 'cancelled')),
    approved_by        uuid REFERENCES auth.users(id) ON DELETE CASCADE,
    key_name           text,
    created_at         timestamptz NOT NULL DEFAULT now(),
    expires_at         timestamptz NOT NULL,
    approved_at        timestamptz
);

CREATE INDEX IF NOT EXISTS idx_mcp_pairings_expires ON private.mcp_pairings (expires_at);

-- Brute-force accounting for code guessing, per authenticated user.
CREATE TABLE IF NOT EXISTS private.mcp_pair_attempts (
    user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_mcp_pair_attempts_user_at ON private.mcp_pair_attempts (user_id, at);

ALTER TABLE private.mcp_pairings ENABLE ROW LEVEL SECURITY;
ALTER TABLE private.mcp_pair_attempts ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON private.mcp_pairings, private.mcp_pair_attempts FROM PUBLIC, anon, authenticated;

-- ------------------------------------------------------------------------------
-- 2. Helpers (private: unreachable through the API)
-- ------------------------------------------------------------------------------

-- Crockford base32 without I, L, O and U, so a human can retype the code from the
-- terminal without ambiguity. 8 characters = 40 bits.
CREATE OR REPLACE FUNCTION private.generate_pair_code()
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    alphabet constant text := '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
    raw      bytea;
    out_code text := '';
    i        int;
BEGIN
    FOR attempt IN 1..10 LOOP
        -- gen_random_uuid() is a pg_catalog built-in (always on the search path, even when it is
        -- empty). pgcrypto's gen_random_bytes is deliberately avoided: on Supabase it lives in the
        -- `extensions` schema, so it would not resolve under SET search_path = ''.
        raw := decode(replace(gen_random_uuid()::text, '-', ''), 'hex');
        out_code := '';
        FOR i IN 0..7 LOOP
            out_code := out_code || substr(alphabet, (get_byte(raw, i) % 32) + 1, 1);
        END LOOP;
        -- Collisions are vanishingly unlikely but must never silently reuse a live code.
        IF NOT EXISTS (SELECT 1 FROM private.mcp_pairings p WHERE p.code = out_code) THEN
            RETURN out_code;
        END IF;
    END LOOP;
    RAISE EXCEPTION 'could not allocate a pairing code' USING ERRCODE = '53000';
END;
$$;

-- Masks an email for display in the terminal: the CLI must be able to show WHICH
-- account approved, without learning the address.
CREATE OR REPLACE FUNCTION private.mask_email(p_email text)
RETURNS text
LANGUAGE sql
IMMUTABLE
SET search_path = ''
AS $$
    SELECT CASE
        WHEN p_email IS NULL OR position('@' in p_email) = 0 THEN 'your account'
        ELSE left(split_part(p_email, '@', 1), 1) || '***@' || split_part(p_email, '@', 2)
    END;
$$;

CREATE OR REPLACE FUNCTION private.sanitize_label(p_label text, p_fallback text)
RETURNS text
LANGUAGE sql
IMMUTABLE
SET search_path = ''
AS $$
    SELECT coalesce(
        nullif(trim(left(regexp_replace(coalesce(p_label, ''), '[^A-Za-z0-9 ._@/()-]', '', 'g'), 60)), ''),
        p_fallback
    );
$$;

REVOKE ALL ON FUNCTION private.generate_pair_code(), private.mask_email(text), private.sanitize_label(text, text)
    FROM PUBLIC, anon, authenticated;

-- ------------------------------------------------------------------------------
-- 3. CLI side (role anon). These never reveal anything about an account.
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.mcp_pair_start(
    p_key_hash          text,
    p_key_prefix        text,
    p_label             text,
    p_platform          text DEFAULT NULL,
    p_client_version    text DEFAULT NULL,
    p_replaces_key_hash text DEFAULT NULL
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_code    text;
    v_expires timestamptz := now() + interval '5 minutes';
BEGIN
    IF p_key_hash IS NULL OR p_key_hash !~ '^[0-9a-f]{64}$' THEN
        RAISE EXCEPTION 'invalid key hash' USING ERRCODE = '22023';
    END IF;
    IF p_replaces_key_hash IS NOT NULL AND p_replaces_key_hash !~ '^[0-9a-f]{64}$' THEN
        RAISE EXCEPTION 'invalid replaces hash' USING ERRCODE = '22023';
    END IF;

    -- A hash that is already a live key must never be re-pairable.
    IF EXISTS (SELECT 1 FROM public.api_keys k WHERE k.key_hash = p_key_hash) THEN
        RAISE EXCEPTION 'key already registered' USING ERRCODE = '23505';
    END IF;

    -- Housekeeping: drop anything long dead, then bound the pending pool.
    DELETE FROM private.mcp_pairings p WHERE p.created_at < now() - interval '1 hour';
    IF (SELECT count(*) FROM private.mcp_pairings p
         WHERE p.status = 'pending' AND p.expires_at > now()) >= 500 THEN
        RAISE EXCEPTION 'pairing temporarily unavailable, try again shortly' USING ERRCODE = '53000';
    END IF;

    -- Re-running `init` on the same machine supersedes its own earlier attempt.
    DELETE FROM private.mcp_pairings p WHERE p.key_hash = p_key_hash AND p.status = 'pending';

    v_code := private.generate_pair_code();

    INSERT INTO private.mcp_pairings (
        code, key_hash, key_prefix, label, platform, client_version, replaces_key_hash, expires_at
    ) VALUES (
        v_code,
        p_key_hash,
        left(coalesce(p_key_prefix, ''), 32),
        private.sanitize_label(p_label, 'A computer'),
        private.sanitize_label(p_platform, 'unknown'),
        left(regexp_replace(coalesce(p_client_version, ''), '[^0-9A-Za-z.+-]', '', 'g'), 20),
        p_replaces_key_hash,
        v_expires
    );

    RETURN jsonb_build_object('code', v_code, 'expiresAt', v_expires);
END;
$$;

CREATE OR REPLACE FUNCTION public.mcp_pair_status(p_key_hash text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_row private.mcp_pairings%ROWTYPE;
BEGIN
    IF p_key_hash IS NULL OR p_key_hash !~ '^[0-9a-f]{64}$' THEN
        RETURN jsonb_build_object('status', 'unknown');
    END IF;

    SELECT * INTO v_row FROM private.mcp_pairings p WHERE p.key_hash = p_key_hash;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('status', 'unknown');
    END IF;

    IF v_row.status = 'pending' AND v_row.expires_at <= now() THEN
        RETURN jsonb_build_object('status', 'expired', 'expiresAt', v_row.expires_at);
    END IF;

    RETURN jsonb_build_object(
        'status',      v_row.status,
        'expiresAt',   v_row.expires_at,
        'keyName',     v_row.key_name,
        -- Masked on purpose: enough for the user to recognise their account, useless to an attacker.
        'accountHint', CASE
            WHEN v_row.status = 'approved' THEN
                (SELECT private.mask_email(u.email) FROM auth.users u WHERE u.id = v_row.approved_by)
            ELSE NULL
        END
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.mcp_pair_cancel(p_key_hash text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_count int;
BEGIN
    IF p_key_hash IS NULL OR p_key_hash !~ '^[0-9a-f]{64}$' THEN
        RETURN jsonb_build_object('cancelled', false);
    END IF;

    UPDATE private.mcp_pairings p
       SET status = 'cancelled'
     WHERE p.key_hash = p_key_hash AND p.status = 'pending';
    GET DIAGNOSTICS v_count = ROW_COUNT;

    RETURN jsonb_build_object('cancelled', v_count > 0);
END;
$$;

-- A key revoking itself: used when the user answers [n] on the terminal, and by `logout`.
CREATE OR REPLACE FUNCTION public.mcp_revoke_self(p_key_hash text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_count int;
BEGIN
    IF p_key_hash IS NULL OR p_key_hash !~ '^[0-9a-f]{64}$' THEN
        RETURN jsonb_build_object('revoked', false);
    END IF;

    UPDATE public.api_keys k
       SET revoked_at = now()
     WHERE k.key_hash = p_key_hash AND k.revoked_at IS NULL;
    GET DIAGNOSTICS v_count = ROW_COUNT;

    RETURN jsonb_build_object('revoked', v_count > 0);
END;
$$;

-- ------------------------------------------------------------------------------
-- 4. Phone side (role authenticated). Identity always comes from auth.uid().
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.mcp_pair_preview(p_code text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user     uuid := auth.uid();
    v_code     text := upper(regexp_replace(coalesce(p_code, ''), '[^0-9A-Za-z]', '', 'g'));
    v_failures int;
    v_oldest   timestamptz;
    v_row      private.mcp_pairings%ROWTYPE;
BEGIN
    IF v_user IS NULL THEN
        RAISE EXCEPTION 'unauthorized' USING ERRCODE = '28000';
    END IF;

    DELETE FROM private.mcp_pair_attempts a WHERE a.at < now() - interval '1 hour';

    SELECT count(*), min(a.at) INTO v_failures, v_oldest
    FROM private.mcp_pair_attempts a
    WHERE a.user_id = v_user AND a.at > now() - interval '10 minutes';

    IF v_failures >= 10 THEN
        RETURN jsonb_build_object(
            'ok', false,
            'error', 'rate_limited',
            'retryAfterSeconds', greatest(1, ceil(extract(epoch from (v_oldest + interval '10 minutes' - now())))::int)
        );
    END IF;

    SELECT * INTO v_row
    FROM private.mcp_pairings p
    WHERE p.code = v_code AND p.status = 'pending' AND p.expires_at > now();

    IF NOT FOUND THEN
        -- Deliberately a normal return, not RAISE: an exception would roll back this INSERT
        -- and the brute-force counter would never advance.
        INSERT INTO private.mcp_pair_attempts (user_id) VALUES (v_user);
        RETURN jsonb_build_object('ok', false, 'error', 'invalid');
    END IF;

    RETURN jsonb_build_object(
        'ok',            true,
        'label',         v_row.label,
        'platform',      v_row.platform,
        'clientVersion', v_row.client_version,
        'keyPrefix',     v_row.key_prefix,
        'expiresAt',     v_row.expires_at
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.mcp_pair_approve(p_code text, p_name text DEFAULT NULL)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user     uuid := auth.uid();
    v_code     text := upper(regexp_replace(coalesce(p_code, ''), '[^0-9A-Za-z]', '', 'g'));
    v_failures int;
    v_oldest   timestamptz;
    v_row      private.mcp_pairings%ROWTYPE;
    v_name     text;
BEGIN
    IF v_user IS NULL THEN
        RAISE EXCEPTION 'unauthorized' USING ERRCODE = '28000';
    END IF;

    SELECT count(*), min(a.at) INTO v_failures, v_oldest
    FROM private.mcp_pair_attempts a
    WHERE a.user_id = v_user AND a.at > now() - interval '10 minutes';

    IF v_failures >= 10 THEN
        RETURN jsonb_build_object(
            'ok', false,
            'error', 'rate_limited',
            'retryAfterSeconds', greatest(1, ceil(extract(epoch from (v_oldest + interval '10 minutes' - now())))::int)
        );
    END IF;

    -- Lock the row so a double-tap cannot create two keys from one code.
    SELECT * INTO v_row
    FROM private.mcp_pairings p
    WHERE p.code = v_code AND p.status = 'pending' AND p.expires_at > now()
    FOR UPDATE;

    IF NOT FOUND THEN
        INSERT INTO private.mcp_pair_attempts (user_id) VALUES (v_user);
        RETURN jsonb_build_object('ok', false, 'error', 'invalid');
    END IF;

    v_name := private.sanitize_label(coalesce(p_name, v_row.label), 'Agent Key');

    -- Replacing this machine's previous key frees a quota slot before the insert below.
    IF v_row.replaces_key_hash IS NOT NULL THEN
        UPDATE public.api_keys k
           SET revoked_at = now()
         WHERE k.key_hash = v_row.replaces_key_hash
           AND k.user_id = v_user
           AND k.revoked_at IS NULL;
    END IF;

    -- The migration-05 quota trigger and unique-active-name index still apply, so
    -- KEY_LIMIT_REACHED (P0001) and 23505 propagate as exceptions and roll this back.
    INSERT INTO public.api_keys (user_id, key_hash, key_prefix, name)
    VALUES (v_user, v_row.key_hash, v_row.key_prefix, v_name);

    UPDATE private.mcp_pairings p
       SET status = 'approved', approved_by = v_user, approved_at = now(), key_name = v_name
     WHERE p.id = v_row.id;

    RETURN jsonb_build_object('ok', true, 'approved', true, 'keyName', v_name);
END;
$$;

-- ------------------------------------------------------------------------------
-- 5. Grants: default deny, then the narrowest possible allow per role.
--    The CLI (anon) must never reach preview/approve; the phone (authenticated)
--    must never reach start/status/cancel.
-- ------------------------------------------------------------------------------
REVOKE ALL ON FUNCTION public.mcp_pair_start(text, text, text, text, text, text) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.mcp_pair_status(text)                              FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.mcp_pair_cancel(text)                              FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.mcp_revoke_self(text)                              FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.mcp_pair_preview(text)                             FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.mcp_pair_approve(text, text)                       FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION public.mcp_pair_start(text, text, text, text, text, text) TO anon;
GRANT EXECUTE ON FUNCTION public.mcp_pair_status(text)                              TO anon;
GRANT EXECUTE ON FUNCTION public.mcp_pair_cancel(text)                              TO anon;
GRANT EXECUTE ON FUNCTION public.mcp_revoke_self(text)                              TO anon;
GRANT EXECUTE ON FUNCTION public.mcp_pair_preview(text)                             TO authenticated;
GRANT EXECUTE ON FUNCTION public.mcp_pair_approve(text, text)                       TO authenticated;

COMMIT;
