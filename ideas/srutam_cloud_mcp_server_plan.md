# Srutam Cloud Sync & MCP Agent Server Plan

## Executive Summary
This document outlines the end-to-end architecture and implementation plan for **Srutam Cloud Sync** and the **Srutam Model Context Protocol (MCP) Server**. 

The goal is to allow users to capture spoken thoughts, meeting notes, and project ideas on their Android device, automatically analyze them with AI, sync the structured insights to a private cloud, and seamlessly expose them to AI coding agents in any IDE (Antigravity IDE, Cursor, Claude Desktop, Windsurf, Cline, Zed) through an authenticated local MCP server.

---

## 1. System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Srutam Android App                       │
│  - Local-first Room DB (Works 100% offline)                 │
│  - On-Device / Cloud Gemini AI (Transcripts & Insights)     │
│  - 1-Tap Google Sign-In (Credential Manager) / Magic Link   │
│  - API Key Generator (Personal Access Tokens)               │
│  - Note Privacy Toggle ("Exclude from MCP")                 │
└──────────────────────────────┬──────────────────────────────┘
                               │
            Instant Push on AI Finish / Auto-Pull on Resume
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   Supabase Cloud Backend                    │
│  - Postgres Database with Row-Level Security (RLS)          │
│  - pgvector Extension (Semantic Vector Search)              │
│  - Tables: users, api_keys, notes, action_items, agent_logs │
│  - Secure API Key Validation RPC (`verify_srutam_api_key`)  │
└──────────────────────────────▲──────────────────────────────┘
                               │
            Authenticated via `SRUTAM_API_KEY` (Bearer / RPC)
                               │
┌──────────────────────────────┴──────────────────────────────┐
│            @srutam/mcp-server (stdio NPM Package)           │
│  - TypeScript MCP Server (`@modelcontextprotocol/sdk`)      │
│  - Tools: search_notes, list_recent_notes, get_note_detail, │
│           list_action_items, update_action_item,            │
│           append_agent_work_log                             │
└──────────────────────────────▲──────────────────────────────┘
                               │
                       stdio Transport
                               │
┌──────────────────────────────┴──────────────────────────────┐
│                 AI Agents & Developer IDEs                  │
│  - Antigravity IDE (`mcp_config.json`)                      │
│  - Cursor IDE (`~/.cursor/mcp.json`)                        │
│  - Claude Desktop (`claude_desktop_config.json`)            │
│  - Windsurf / Zed / VS Code (Cline / Roo Code)              │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Core User Journey

1. **Idea Capture on the Go**:
   - User speaks into Srutam during a commute, walk, or brainstorming session.
   - Srutam transcribes the recording and generates structured insights: Title, Summary, Key Points, Action Items, and WIIFM (What's In It For Me).
2. **Instant Text Sync**:
   - The structured text, action items, and an embedding vector are pushed to Supabase immediately upon AI completion.
   - Audio files remain local on the device (zero wasted bandwidth/cloud storage costs).
   - Notes are accessible to MCP agents by default, with an optional "Private (Exclude from MCP)" toggle per note.
3. **One-Time Developer Setup**:
   - In Srutam Android Settings -> **Developer & MCP**, the user taps **Generate API Key** (e.g. `srtm_live_9a8f...`).
   - The user adds the one-time configuration snippet to their IDE's MCP config:
     ```json
     {
       "mcpServers": {
         "srutam": {
           "command": "npx",
           "args": ["-y", "@srutam/mcp-server"],
           "env": {
             "SRUTAM_API_KEY": "srtm_live_xxxxxxxxxxxxxxxx"
           }
         }
       }
     }
     ```
4. **Agent Action in Projects**:
   - In any new or existing repository, the user tells their agent:
     > *"Check Srutam for my brainstormed idea on user authentication from yesterday, and scaffold the database schema and endpoints."*
   - The agent executes `search_notes` or `get_recent_notes`, reads the transcript and key points, and begins writing code.
   - When the agent finishes implementing the task, it calls `update_action_item` (marks it done) and `append_agent_work_log` (*"Implemented in PR #42"*).
   - The next time the user opens Srutam on Android, the task is marked completed with the agent's note attached.

---

## 3. Database Schema & Security (Supabase)

### PostgreSQL Schema
```sql
-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. API Keys Table
CREATE TABLE public.api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    key_hash TEXT NOT NULL UNIQUE,
    key_prefix TEXT NOT NULL, -- e.g. "srtm_live_9a8f"
    name TEXT NOT NULL DEFAULT 'Developer Key',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

-- 2. Notes Table
CREATE TABLE public.notes (
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
    timestamp TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_private BOOLEAN NOT NULL DEFAULT false,
    embedding VECTOR(768), -- Gemini / text-embedding-004
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Action Items Table
CREATE TABLE public.action_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id UUID NOT NULL REFERENCES public.notes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    completed_by TEXT, -- "user" or "agent:Cursor"
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4. Agent Work Logs Table
CREATE TABLE public.agent_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id UUID NOT NULL REFERENCES public.notes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    agent_name TEXT NOT NULL, -- e.g. "Antigravity", "Cursor"
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Row Level Security (RLS)
- Every table has RLS enabled:
  ```sql
  ALTER TABLE public.notes ENABLE ROW LEVEL SECURITY;
  ALTER TABLE public.action_items ENABLE ROW LEVEL SECURITY;
  ALTER TABLE public.api_keys ENABLE ROW LEVEL SECURITY;
  ALTER TABLE public.agent_logs ENABLE ROW LEVEL SECURITY;
  ```
- Direct user access: `auth.uid() = user_id`.
- MCP Server verification RPC function hashes the incoming `SRUTAM_API_KEY`, verifies it is active and not revoked, sets the authenticated user context, and filters out `WHERE is_private = true`.

---

## 4. MCP Server Specification (`@srutam/mcp-server`)

### Location in Repository
`mcp-server/` in this repository (monorepo structure with TypeScript, automated build, and publishing setup).

### Transport & Packaging
- Standard `stdio` protocol via `@modelcontextprotocol/sdk`.
- Published as `@srutam/mcp-server` on npm (runnable instantly with `npx -y @srutam/mcp-server`).
- Requires single environment variable: `SRUTAM_API_KEY`.

### Exposed Tool Suite
1. **`search_notes`**:
   - **Arguments**: `query` (string), `limit` (number, default: 5)
   - **Description**: Performs semantic and keyword search across notes, summaries, and transcripts.
2. **`list_recent_notes`**:
   - **Arguments**: `limit` (number, default: 10), `include_action_items` (boolean, default: true)
   - **Description**: Retrieves recent notes in chronological order with extracted key points.
3. **`get_note_detail`**:
   - **Arguments**: `note_id` (string)
   - **Description**: Returns the full transcript, summary, key points, WIIFM, action items, and agent logs for a specific note.
4. **`list_action_items`**:
   - **Arguments**: `status` ("pending" | "completed" | "all"), `limit` (number, default: 20)
   - **Description**: Aggregates actionable items across all ideas.
5. **`update_action_item`**:
   - **Arguments**: `action_item_id` (string), `completed` (boolean), `agent_name` (optional string)
   - **Description**: Marks an action item completed and attributes completion to the agent.
6. **`append_agent_work_log`**:
   - **Arguments**: `note_id` (string), `summary` (string), `agent_name` (string)
   - **Description**: Records an implementation trail or design note back onto the user's voice note.

---

## 5. Android Application Integration

### Local-First Architecture
- Srutam remains **100% functional offline without an account**. Users can record, transcribe, and review notes with zero network connectivity.
- Cloud Sync and MCP access are opt-in features configured under **Settings**.

### Authentication Flow
- **1-Tap Google Sign-In**: Powered by modern Android `androidx.credentials:credentials` (Credential Manager).
- **Email Magic Link / OTP**: Fallback for users without Google Play Services or preferring email auth.
- **Session Management**: Supabase GoTrue session persisted in encrypted storage.

### UI & UX Enhancements
1. **Settings Screen -> Developer & MCP Section**:
   - Account status card (showing user email or "Sign In to Enable Cloud Sync & MCP").
   - "API Keys for AI Agents" list showing active keys, creation dates, and revoke buttons.
   - "Create API Key" modal with key name input (e.g. "Macbook Cursor") and 1-tap "Copy to Clipboard" / "Share to Email".
   - Setup Guide snippet generator with ready-to-paste JSON configs for Cursor, Claude Desktop, and Antigravity.
2. **Note Detail Screen**:
   - "Private Note (Exclude from MCP)" toggle switch.
   - Agent Work Logs badge showing agent contributions (e.g., "Agent Cursor completed 2 tasks").
3. **Background Sync Service**:
   - Triggered automatically when AI transcription/analysis finishes.
   - Auto-pulls agent changes on app resume and swipe-to-refresh.

---

## 6. Implementation Milestones

- **Milestone 1: Backend & Schema Setup**: Deploy Supabase Postgres tables, RLS policies, `pgvector`, and API key verification functions.
- **Milestone 2: MCP Server Development**: Implement `@srutam/mcp-server` in `mcp-server/` with all 6 tools, unit tests, and stdio transport.
- **Milestone 3: Android Supabase & Auth Integration**: Add Supabase Kotlin SDK and Android Credential Manager for 1-tap sign-in.
- **Milestone 4: Android API Key Management UI**: Implement key generation, listing, revocation, and MCP setup guide in Settings.
- **Milestone 5: Bidirectional Sync Engine**: Implement instant push on note creation, note privacy toggle, and remote action item sync.
- **Milestone 6: Verification & End-to-End Testing**: Test full loop: Phone voice recording -> Supabase cloud -> IDE agent query -> Agent marks task complete -> Phone reflects updated state.
