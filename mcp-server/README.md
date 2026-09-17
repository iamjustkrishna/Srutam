# srutam-mcp

Official **Model Context Protocol (MCP)** server for [Srutam](https://srutam.space) — Pure Voice, Crystallized Thought.

Capture ideas, architectural brainstorms, and task lists on your phone using Srutam, and seamlessly search, read, and execute them directly inside your favorite AI coding agents (**Cursor**, **Antigravity**, **Claude Desktop**, **Windsurf**, **Cline**).

---

## ⚡ Quick Start (30 Seconds)

Run the interactive setup wizard in your terminal:

```bash
npx -y srutam-mcp init
```

The wizard will:
1. Prompt for your Srutam API key (obtained from Srutam Mobile App $\rightarrow$ **Settings** $\rightarrow$ **Developer & MCP**).
2. Validate the key against Srutam Cloud.
3. Automatically configure **Cursor**, **Antigravity**, or **Claude Desktop** on your machine.

---

## 💻 CLI Commands

You can run `srutam-mcp` directly in your terminal for status checks and maintenance:

```bash
# Interactive dashboard (shows active key, synced notes count, and health)
npx srutam-mcp

# Test cloud connectivity
npx srutam-mcp status

# Reconfigure or link a new IDE
npx srutam-mcp init

# Disconnect and clear saved credentials
npx srutam-mcp logout

# View command line options
npx srutam-mcp --help
```

---

## 🛠 Manual Configuration

If you prefer to configure manually, add `srutam-mcp` to your IDE's MCP config:

### A. Cursor IDE (`~/.cursor/mcp.json`)
```json
{
  "mcpServers": {
    "srutam": {
      "command": "npx",
      "args": ["-y", "srutam-mcp"],
      "env": {
        "SRUTAM_API_KEY": "srtm_live_your_key_here"
      }
    }
  }
}
```

### B. Antigravity IDE (`mcp_config.json`)
```json
{
  "mcpServers": {
    "srutam": {
      "command": "npx",
      "args": ["-y", "srutam-mcp"],
      "env": {
        "SRUTAM_API_KEY": "srtm_live_your_key_here"
      }
    }
  }
}
```

### C. Claude Desktop (`claude_desktop_config.json`)
```json
{
  "mcpServers": {
    "srutam": {
      "command": "npx",
      "args": ["-y", "srutam-mcp"],
      "env": {
        "SRUTAM_API_KEY": "srtm_live_your_key_here"
      }
    }
  }
}
```

---

## 🧰 Exposed MCP Tools

| Tool | Description |
| :--- | :--- |
| `search_notes` | Semantic and keyword search across your voice notes, summaries, and transcripts. |
| `list_recent_notes` | Retrieves chronological recent voice notes with executive summaries and key points. |
| `get_note_detail` | Retrieves full verbatim transcript, structured summary, WIIFM, and action items for a note. |
| `list_action_items` | Lists all actionable tasks and next steps, filtered by `pending` or `completed`. |
| `update_action_item` | Marks tasks as completed and records agent attribution. Syncs back to your phone. |
| `append_agent_work_log` | Attaches implementation notes or git commits directly onto a voice memo. |

---

## 🛡 Performance & Concurrency

- **In-Memory TTL Caching**: Key authentication is cached for 60 seconds and read queries for 30 seconds to minimize database roundtrips.
- **Rate Limiting**: Protected with an in-process token-bucket rate limiter (60 requests/minute per key) to guard against runaway agent loops.
- **Strict Bounding**: Parameter boundaries ensure compact payloads (`limit` capped at 25 for notes, 50 for tasks).

---

## 📄 License

MIT © [Krishna](https://github.com/iamjustkrishna)
