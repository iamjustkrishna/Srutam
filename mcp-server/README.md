# @srutam/mcp-server

Official **Model Context Protocol (MCP)** server for [Srutam](https://srutam.space) — Pure Voice, Crystallized Thought.

Capture fleeting ideas, architectural brainstorms, and task lists on your phone using Srutam, and seamlessly fetch, search, and implement them directly inside your favorite AI coding agents (Cursor, Antigravity IDE, Claude Desktop, Windsurf, Cline).

---

## ⚡ Quick Setup

### 1. Generate an API Key in Srutam
1. Open the **Srutam** Android app.
2. Go to **Settings** $\rightarrow$ **Developer & MCP Brain**.
3. Tap **Generate API Key** (e.g., `srtm_live_9a8f4c...`).
4. Copy the key.

### 2. Configure Your AI Agent / IDE

#### A. Antigravity IDE
Add the following to your project's `.gemini/antigravity-ide/mcp_config.json` (or workspace MCP settings):
```json
{
  "mcpServers": {
    "srutam": {
      "command": "npx",
      "args": ["-y", "@srutam/mcp-server"],
      "env": {
        "SRUTAM_API_KEY": "srtm_live_your_key_here"
      }
    }
  }
}
```

#### B. Cursor IDE
Add to `~/.cursor/mcp.json`:
```json
{
  "mcpServers": {
    "srutam": {
      "command": "npx",
      "args": ["-y", "@srutam/mcp-server"],
      "env": {
        "SRUTAM_API_KEY": "srtm_live_your_key_here"
      }
    }
  }
}
```

#### C. Claude Desktop
Add to `claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "srutam": {
      "command": "npx",
      "args": ["-y", "@srutam/mcp-server"],
      "env": {
        "SRUTAM_API_KEY": "srtm_live_your_key_here"
      }
    }
  }
}
```

---

## 🛠 Exposed MCP Tools

| Tool | Description |
| :--- | :--- |
| `search_notes` | Semantic and keyword search across your voice notes, summaries, and transcripts. |
| `list_recent_notes` | Retrieves chronological recent voice notes with executive summaries and key points. |
| `get_note_detail` | Retrieves full verbatim transcript, structured summary, WIIFM, and action items. |
| `list_action_items` | Lists all actionable tasks and next steps, filtered by `pending` or `completed`. |
| `update_action_item` | Marks tasks as completed and records agent attribution. Syncs back to your phone. |
| `append_agent_work_log` | Attaches implementation notes or git commits directly onto a voice memo. |

---

## 💻 Local Development

```bash
# Install dependencies
npm install

# Build TypeScript to dist/
npm run build

# Run locally via stdio
node dist/index.js
```
