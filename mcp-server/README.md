# srutam-mcp

Official **Model Context Protocol (MCP)** server for [Srutam](https://srutam.space): Pure Voice, Crystallized Thought.

Capture ideas, architectural brainstorms, and task lists on your phone using Srutam, and seamlessly search, read, and execute them directly inside your favorite AI coding agents (**OpenCode**, **Cursor**, **Windsurf**, **Zed**, **Claude Desktop**, **Antigravity**, **Cline**).

**Requires Node.js 22 or newer.**

---

## ⚡ Quick Start (30 Seconds)

Run the interactive setup wizard in your terminal:

```bash
npx -y srutam-mcp init
```

The wizard will:
1. Prompt for your Srutam API key (Srutam app → **Settings** → **Cloud Sync & Developer Brain (MCP)** → **MCP Agent Keys** → **New Key**). Input is hidden.
2. Validate the key against Srutam Cloud.
3. Save it to `~/.srutam/config.json` (owner-only permissions on macOS/Linux).
4. Add a `srutam` entry to the IDE configs it finds on your machine (Cursor, Windsurf, OpenCode, Claude Desktop, Antigravity, Zed, Cline). **Your API key is never written into those files.** Existing settings, comments and formatting are preserved, and a file the wizard cannot parse is left untouched.

---

## 💻 CLI Commands

```bash
# Interactive dashboard (shows active key prefix, synced notes count, and health)
npx srutam-mcp

# Start stdio MCP server for IDEs (this is what IDE configs run)
npx srutam-mcp serve

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

`srutam-mcp init` is the recommended path. To configure by hand, first save your key once with `npx srutam-mcp init` (or create `~/.srutam/config.json` containing `{ "apiKey": "srtm_live_..." }`), then add the launch entry below. **No key belongs in the IDE file.**

On **Windows**, use `"command": "cmd"` with `"args": ["/c", "npx", "-y", "srutam-mcp@1", "serve"]` (most clients cannot start `npx` directly).

### Standard `mcpServers` clients

Cursor (`~/.cursor/mcp.json`), Windsurf (`~/.codeium/windsurf/mcp_config.json`), Claude Desktop (`claude_desktop_config.json`), Antigravity, and Cline / Roo Code (`cline_mcp_settings.json`) all use:

```json
{
  "mcpServers": {
    "srutam": {
      "command": "npx",
      "args": ["-y", "srutam-mcp@1", "serve"]
    }
  }
}
```

### OpenCode (`~/.config/opencode/opencode.json`)

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "srutam": {
      "type": "local",
      "command": ["npx", "-y", "srutam-mcp@1", "serve"],
      "enabled": true
    }
  }
}
```

### Zed Editor (`settings.json`)

```json
{
  "context_servers": {
    "srutam": {
      "command": {
        "path": "npx",
        "args": ["-y", "srutam-mcp@1", "serve"]
      }
    }
  }
}
```

### Continue.dev (`~/.continue/config.json`)

```json
{
  "experimental": {
    "modelContextProtocolServers": [
      {
        "transport": {
          "type": "stdio",
          "command": "npx",
          "args": ["-y", "srutam-mcp@1", "serve"]
        }
      }
    ]
  }
}
```

### When the IDE cannot see `~/.srutam/config.json`

WSL, dev containers and remote SSH sessions run the server in a different environment than the one where you ran `init`. In that case, pass the key through the environment for that entry instead:

```json
"env": { "SRUTAM_API_KEY": "srtm_live_your_key_here" }
```

Treat any file containing a key as a secret: do not commit it, and revoke the key in the app if it leaks.

---

## 🧰 Exposed MCP Tools

| Tool | Description |
| :--- | :--- |
| `search_notes` | Keyword search across your voice notes' titles, summaries, and transcripts. |
| `list_recent_notes` | Retrieves chronological recent voice notes with executive summaries and key points. |
| `get_note_detail` | Retrieves full verbatim transcript, structured summary, WIIFM, and action items for a note. |
| `list_action_items` | Lists all actionable tasks and next steps, filtered by `pending` or `completed`. |
| `update_action_item` | Marks tasks as completed and records agent attribution (`agent:<name>`). Syncs back to your phone. |
| `append_agent_work_log` | Attaches implementation notes or git commits directly onto a voice memo. |

Notes you mark **private** in the app are never visible to agents: not in search, not in details, and not through their action items.

---

## 🛡 Security Model

- **Authorization is enforced by the database, not the client.** Every call carries only the SHA-256 hash of your API key; Srutam Cloud resolves the owner itself. Knowing someone's user id gives no access. Revoking a key in the app takes effect on the very next call.
- **No secrets in IDE config files.** The key lives in `~/.srutam/config.json` only.
- **No `.env` auto-loading.** The server ignores `.env` files in the project you have open, so a hostile repository cannot redirect it. `SUPABASE_URL` must be an `https://*.supabase.co` address (self-hosters opt in with `SRUTAM_ALLOW_CUSTOM_URL=1`).
- **Voice-note text is treated as untrusted.** Transcripts capture anything spoken near your microphone, and summaries and tasks are generated from them. Tool output fences all note-derived text in an explicit "untrusted data" block so agents don't follow instructions embedded in it. Review anything an agent proposes to run on the strength of a note.
- **Least privilege.** Agents can read notes, complete/reopen tasks, and append work logs. They cannot delete or edit notes.
- **Runaway-loop guard.** An in-process limiter (60 calls/minute) and a 30-second read cache keep a looping agent from hammering the backend. This is a convenience, not a security boundary.
- Parameters are bounded (`limit` ≤ 25 for notes, ≤ 50 for tasks) and IDs must be UUIDs.

---

## ⬆️ Upgrading to 1.3.0

1.3.0 changes how the server authenticates to Srutam Cloud (see Security Model). **Versions 1.2.x stop working once the matching cloud migration is applied**, so update with `npx -y srutam-mcp@latest init`. That also rewrites your IDE entries without the key and with the explicit `serve` argument. Existing entries that still contain `SRUTAM_API_KEY` are replaced.

---

## 📄 License

MIT © [Krishna](https://github.com/iamjustkrishna)
