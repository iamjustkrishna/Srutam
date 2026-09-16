-- ==============================================================================
-- Srutam Cloud & MCP Agent Server Schema
-- Supports multi-tenant voice note sync, API key auth for IDE agents,
-- and hybrid keyword/semantic search over spoken insights.
-- ==============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- 1. API Keys Table (Personal Access Tokens generated in Android Settings)
CREATE TABLE IF NOT EXISTS public.api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    key_hash TEXT NOT NULL UNIQUE,
    key_prefix TEXT NOT NULL, -- e.g. "srtm_live_9a8f" for user recognition
    name TEXT NOT NULL DEFAULT 'Developer Key',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_api_keys_hash ON public.api_keys(key_hash);
CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON public.api_keys(user_id);

-- 2. Notes Table (Structured voice insights synced from Android Room DB)
CREATE TABLE IF NOT EXISTS public.notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    client_recording_id BIGINT,
    title TEXT NOT NULL,
    transcript TEXT,
    summary TEXT,
    key_points JSONB DEFAULT '[]'::jsonb,
    wiifm TEXT,
    ai_status TEXT NOT NULL DEFAULT 'COMPLETED',
    duration_ms BIGINT DEFAULT 0,
    "timestamp" TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_private BOOLEAN NOT NULL DEFAULT false, -- If true, filtered out from MCP queries
    embedding VECTOR(768), -- Gemini text-embedding-004 vector
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notes_user_id ON public.notes(user_id);
CREATE INDEX IF NOT EXISTS idx_notes_timestamp ON public.notes("timestamp" DESC);
CREATE INDEX IF NOT EXISTS idx_notes_is_private ON public.notes(is_private);

-- Full-text search index over title, summary, and transcript
CREATE INDEX IF NOT EXISTS idx_notes_fts ON public.notes USING GIN (
    to_tsvector('english', coalesce(title, '') || ' ' || coalesce(summary, '') || ' ' || coalesce(transcript, ''))
);

-- 3. Action Items Table (Tasks extracted by AI from voice memos)
CREATE TABLE IF NOT EXISTS public.action_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id UUID NOT NULL REFERENCES public.notes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    completed_by TEXT, -- e.g. "user" or "agent:Cursor"
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_action_items_user_id ON public.action_items(user_id);
CREATE INDEX IF NOT EXISTS idx_action_items_note_id ON public.action_items(note_id);
CREATE INDEX IF NOT EXISTS idx_action_items_status ON public.action_items(is_completed);

-- 4. Agent Work Logs Table (Audit trail left by coding agents on a note)
CREATE TABLE IF NOT EXISTS public.agent_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id UUID NOT NULL REFERENCES public.notes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    agent_name TEXT NOT NULL, -- e.g. "Antigravity", "Cursor", "Claude"
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_agent_logs_note_id ON public.agent_logs(note_id);
CREATE INDEX IF NOT EXISTS idx_agent_logs_user_id ON public.agent_logs(user_id);

-- ==============================================================================
-- Row Level Security (RLS) Policies
-- ==============================================================================

ALTER TABLE public.api_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.action_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.agent_logs ENABLE ROW LEVEL SECURITY;

-- User Authenticated via JWT (Android App via Supabase GoTrue)
CREATE POLICY "Users can manage their own API keys"
    ON public.api_keys FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can manage their own notes"
    ON public.notes FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can manage their own action items"
    ON public.action_items FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can manage their own agent logs"
    ON public.agent_logs FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ==============================================================================
-- Secure RPC Functions for MCP Server
-- ==============================================================================

-- Verify API Key and return the owner's user_id if valid & active
CREATE OR REPLACE FUNCTION public.verify_srutam_api_key(raw_key_hash TEXT)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    found_user_id UUID;
BEGIN
    SELECT user_id INTO found_user_id
    FROM public.api_keys
    WHERE key_hash = raw_key_hash
      AND revoked_at IS NULL;

    IF found_user_id IS NOT NULL THEN
        -- Update last_used_at timestamp
        UPDATE public.api_keys
        SET last_used_at = now()
        WHERE key_hash = raw_key_hash;
    END IF;

    RETURN found_user_id;
END;
$$;

-- Search notes for an authenticated MCP agent (respects is_private = false)
CREATE OR REPLACE FUNCTION public.mcp_search_notes(
    p_user_id UUID,
    p_query TEXT,
    p_limit INT DEFAULT 5
)
RETURNS TABLE (
    id UUID,
    title TEXT,
    summary TEXT,
    transcript TEXT,
    key_points JSONB,
    wiifm TEXT,
    "timestamp" TIMESTAMPTZ,
    similarity_rank REAL
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    IF p_query IS NULL OR trim(p_query) = '' THEN
        RETURN QUERY
        SELECT 
            n.id,
            n.title,
            n.summary,
            n.transcript,
            n.key_points,
            n.wiifm,
            n."timestamp",
            1.0::REAL AS similarity_rank
        FROM public.notes n
        WHERE n.user_id = p_user_id
          AND n.is_private = false
        ORDER BY n."timestamp" DESC
        LIMIT p_limit;
    ELSE
        RETURN QUERY
        SELECT 
            n.id,
            n.title,
            n.summary,
            n.transcript,
            n.key_points,
            n.wiifm,
            n."timestamp",
            ts_rank_cd(
                to_tsvector('english', coalesce(n.title, '') || ' ' || coalesce(n.summary, '') || ' ' || coalesce(n.transcript, '')),
                plainto_tsquery('english', p_query)
            ) AS similarity_rank
        FROM public.notes n
        WHERE n.user_id = p_user_id
          AND n.is_private = false
          AND (
              to_tsvector('english', coalesce(n.title, '') || ' ' || coalesce(n.summary, '') || ' ' || coalesce(n.transcript, '')) @@ plainto_tsquery('english', p_query)
              OR n.title ILIKE '%' || p_query || '%'
              OR n.summary ILIKE '%' || p_query || '%'
          )
        ORDER BY similarity_rank DESC, n."timestamp" DESC
        LIMIT p_limit;
    END IF;
END;
$$;
