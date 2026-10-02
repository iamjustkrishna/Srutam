-- ==============================================================================
-- Srutam MCP RPC Suite Migration
-- File: supabase/migrations/20260917_02_mcp_rpc_suite.sql
-- Description: Complete suite of SECURITY DEFINER RPC functions for Model
--              Context Protocol (MCP) servers accessing notes, tasks, and logs.
-- ==============================================================================

-- 1. Get Note Detail with Action Items and Agent Logs in a single roundtrip
CREATE OR REPLACE FUNCTION public.mcp_get_note_detail(
    p_user_id UUID,
    p_note_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    note_rec JSONB;
    actions_rec JSONB;
    logs_rec JSONB;
BEGIN
    -- Verify note exists, belongs to this user, and is not private
    SELECT to_jsonb(n) INTO note_rec
    FROM public.notes n
    WHERE n.id = p_note_id
      AND n.user_id = p_user_id
      AND n.is_private = false;

    IF note_rec IS NULL THEN
        RETURN NULL;
    END IF;

    -- Fetch action items associated with this note
    SELECT coalesce(jsonb_agg(to_jsonb(a) ORDER BY a.created_at ASC), '[]'::jsonb) INTO actions_rec
    FROM public.action_items a
    WHERE a.note_id = p_note_id;

    -- Fetch agent work logs associated with this note
    SELECT coalesce(jsonb_agg(to_jsonb(l) ORDER BY l.created_at ASC), '[]'::jsonb) INTO logs_rec
    FROM public.agent_logs l
    WHERE l.note_id = p_note_id;

    RETURN jsonb_build_object(
        'note', note_rec,
        'actionItems', actions_rec,
        'agentLogs', logs_rec
    );
END;
$$;

-- 2. List Action Items across all notes for a user (with status filter)
CREATE OR REPLACE FUNCTION public.mcp_list_action_items(
    p_user_id UUID,
    p_status TEXT DEFAULT 'pending',
    p_limit INT DEFAULT 20
)
RETURNS TABLE (
    id UUID,
    note_id UUID,
    description TEXT,
    is_completed BOOLEAN,
    completed_by TEXT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        a.id,
        a.note_id,
        a.description,
        a.is_completed,
        a.completed_by,
        a.completed_at,
        a.created_at
    FROM public.action_items a
    WHERE a.user_id = p_user_id
      AND (
          p_status = 'all'
          OR (p_status = 'pending' AND a.is_completed = false)
          OR (p_status = 'completed' AND a.is_completed = true)
      )
    ORDER BY a.created_at DESC
    LIMIT p_limit;
END;
$$;

-- 3. Update Action Item Status (Complete or Uncomplete)
CREATE OR REPLACE FUNCTION public.mcp_update_action_item(
    p_user_id UUID,
    p_item_id UUID,
    p_completed BOOLEAN,
    p_agent_name TEXT DEFAULT 'AI Agent'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    updated_row JSONB;
BEGIN
    UPDATE public.action_items
    SET 
        is_completed = p_completed,
        completed_by = CASE WHEN p_completed THEN 'agent:' || p_agent_name ELSE NULL END,
        completed_at = CASE WHEN p_completed THEN now() ELSE NULL END
    WHERE id = p_item_id
      AND user_id = p_user_id
    RETURNING to_jsonb(action_items.*) INTO updated_row;

    RETURN updated_row;
END;
$$;

-- 4. Append Agent Work Log (Audit Trail)
CREATE OR REPLACE FUNCTION public.mcp_append_agent_log(
    p_user_id UUID,
    p_note_id UUID,
    p_agent_name TEXT,
    p_message TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    inserted_row JSONB;
BEGIN
    -- Ensure note belongs to user and is not private
    IF NOT EXISTS (
        SELECT 1 FROM public.notes
        WHERE id = p_note_id AND user_id = p_user_id AND is_private = false
    ) THEN
        RAISE EXCEPTION 'Note not found or access denied';
    END IF;

    INSERT INTO public.agent_logs (note_id, user_id, agent_name, message, created_at)
    VALUES (p_note_id, p_user_id, p_agent_name, p_message, now())
    RETURNING to_jsonb(agent_logs.*) INTO inserted_row;

    RETURN inserted_row;
END;
$$;
