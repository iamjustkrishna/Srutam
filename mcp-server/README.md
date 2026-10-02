# srutam-mcp

Official **Model Context Protocol (MCP)** server for [Srutam](https://srutam.space): Pure Voice, Crystallized Thought.

Capture ideas, architectural brainstorms, and task lists on your phone using Srutam, and seamlessly search, read, and execute them directly inside your favorite AI coding agents (**Claude Code**, **Codex**, **Gemini CLI**, **Cursor**, **VS Code**, **Windsurf**, **OpenCode**, **Zed**, **Kiro** and more).

**Requires Node.js 22 or newer.**

---

## ⚡ Quick Start (30 Seconds)

Run the interactive setup wizard in your terminal:

```bash
npx -y srutam-mcp init
```

The wizard will:
1. Reuse your saved key if there is one, or ask for a new one (Srutam app → **Settings** → **Cloud Sync & Developer Brain (MCP)** → **MCP Agent Keys** → **New Key**). Input is hidden.
2. Validate the key against Srutam Cloud and save it to `~/.srutam/config.json` (owner-only permissions on macOS/Linux).
3. Show every supported assistant, mark the ones it finds on your machine, and add a `srutam` entry to the ones you pick (default: all detected). **Your API key is never written into those files.** Existing settings, comments and formatting are preserved, and a file the wizard cannot parse is left untouched.

Supported: **Claude Code, OpenAI Codex, Gemini CLI, Cursor, VS Code / GitHub Copilot, Windsurf, OpenCode, Claude Desktop, Zed, Kiro, Qwen Code, Crush, Amp, GitHub Copilot CLI, Cline, Antigravity**, plus printable snippets for Goose, Continue and JetBrains.

Non-interactive, for scripts and dotfiles:

```bash
npx -y srutam-mcp init --client codex,claude-code,gemini --yes   # uses the saved key; "all" = every detected client
npx -y srutam-mcp print-config codex                             # print one client's snippet (add --windows for the Windows launcher)
```

---

## 💻 CLI Commands

```bash
# Interactive dashboard (shows active key prefix, synced notes count, and health)
npx srutam-mcp

# Start stdio MCP server for IDEs (this is what IDE configs run)
npx srutam-mcp serve

# Test cloud connectivity
npx srutam-mcp status

# Reconfigure or link another assistant
npx srutam-mcp init

# Print a config snippet
npx srutam-mcp print-config codex

# Disconnect and clear saved credentials
npx srutam-mcp logout

# View command line options
npx srutam-mcp --help
```

---

## 🛠 Manual Configuration

`srutam-mcp init` is the recommended path. To configure by hand, first save your key once with `npx srutam-mcp init` (or create `~/.srutam/config.json` containing `{ "apiKey": "srtm_live_..." }`), then add the launch entry for your assistant. **No key belongs in these files.** `npx srutam-mcp print-config <client>` prints the exact snippet.

On **Windows**, most clients cannot start `npx` directly: use `"command": "cmd"` with `"args": ["/c", "npx", "-y", "srutam-mcp@1", "serve"]` (`print-config --windows` does this for you).

| Client | Where | Format |
| :--- | :--- | :--- |
| **Claude Code** | run `claude mcp add --scope user srutam -- npx -y srutam-mcp@1 serve` | CLI (stored in `~/.claude.json`) |
| **OpenAI Codex** | `~/.codex/config.toml` (`codex mcp add srutam -- npx -y srutam-mcp@1 serve` also works) | TOML `[mcp_servers.srutam]` |
| **Gemini CLI**, **Qwen Code**, **Kiro**, **Cursor**, **Windsurf**, **Claude Desktop**, **Cline**, **Antigravity***, **Copilot CLI*** | `~/.gemini/settings.json`, `~/.qwen/settings.json`, `~/.kiro/settings/mcp.json`, `~/.cursor/mcp.json`, `~/.codeium/windsurf/mcp_config.json`, `claude_desktop_config.json`, Cline's `cline_mcp_settings.json`, ... | JSON `mcpServers` |
| **VS Code / GitHub Copilot** | `.vscode/mcp.json` or *MCP: Open User Configuration* | JSON `servers` with `"type": "stdio"` |
| **OpenCode** | `~/.config/opencode/opencode.json` | JSON `mcp` with `"type": "local"` |
| **Zed** | `settings.json` | JSON `context_servers` |
| **Crush** | `~/.config/crush/crush.json` | JSON `mcp` with `"type": "stdio"` |
| **Amp*** | `~/.config/amp/settings.json` | JSON `amp.mcpServers` |
| **Goose**, **Continue**, **JetBrains** | see `print-config goose` etc. | snippet only |

* format not verified against the real application; check its docs if the server does not load.

The standard entry looks like this:

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

Tip: the first start downloads the package via `npx`, which can exceed a client's default startup timeout. The Codex entry sets `startup_timeout_sec = 60`; for Claude Code set `MCP_TIMEOUT=60000` if the first start fails.

### When the client cannot see `~/.srutam/config.json`

WSL, dev containers and remote SSH sessions run the server in a different environment than the one where you ran `init`. Pass the key through the environment for that entry instead:

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
| `list_insights` | Lists ideas and decisions (not tasks) captured in voice notes, filtered by `idea` or `decision`. |
| `list_reminders` | Lists meetings, deadlines, and calls captured in voice notes. Read-only. |

Notes you mark **private** in the app are never visible to agents: not in search, not in details, and not through their action items, insights, or reminders.

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
