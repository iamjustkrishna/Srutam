package space.iamjustkrishna.srutam.ui.screens

/**
 * Single source of truth for the "Agent MCP Config Snippet" card.
 * Snippets contain the launch entry only: the API key is stored by `npx srutam-mcp init` in ~/.srutam/config.json
 * and must not be pasted into client config files.
 *
 * Formats last checked against public docs on 2026-09-30. Entries flagged unverified were not confirmed
 * against the real client; the card shows a warning for them.
 */
enum class McpClientGroup(val label: String) {
    TERMINAL("Terminal agents"),
    EDITOR("Editors & IDEs"),
    DESKTOP("Desktop apps")
}

data class McpClient(
    val id: String,
    val label: String,
    val group: McpClientGroup,
    val pathHint: String,
    val snippetLanguage: String,
    val snippet: (windows: Boolean) -> String,
    val cliCommand: ((windows: Boolean) -> String)? = null,
    val note: String? = null,
    val unverified: Boolean = false
)

private const val PKG = "srutam-mcp@1"

/** Windows GUI/CLI clients usually spawn without a shell, so `npx` (a .cmd shim) must run through cmd.exe. */
private fun launcher(windows: Boolean): Pair<String, List<String>> {
    val args = listOf("-y", PKG, "serve")
    return if (windows) "cmd" to (listOf("/c", "npx") + args) else "npx" to args
}

private fun jsonArray(items: List<String>) = items.joinToString(", ") { "\"$it\"" }

private fun standardServers(windows: Boolean): String {
    val (cmd, args) = launcher(windows)
    return """
{
  "mcpServers": {
    "srutam": {
      "command": "$cmd",
      "args": [${jsonArray(args)}]
    }
  }
}""".trimIndent()
}

private fun cliLine(windows: Boolean): String {
    val (cmd, args) = launcher(windows)
    return (listOf(cmd) + args).joinToString(" ")
}

object McpClients {

    val all: List<McpClient> = listOf(
        // ---- Terminal agents -------------------------------------------------
        McpClient(
            id = "claude-code", label = "Claude Code", group = McpClientGroup.TERMINAL,
            pathHint = "Run once in a terminal (stored in ~/.claude.json)",
            snippetLanguage = "shell",
            snippet = { w -> "claude mcp add --scope user srutam -- ${cliLine(w)}" },
            note = "Use --scope project instead to share it via .mcp.json. Set MCP_TIMEOUT=60000 if the first start times out."
        ),
        McpClient(
            id = "codex", label = "Codex", group = McpClientGroup.TERMINAL,
            pathHint = "~/.codex/config.toml (shared by the Codex CLI and IDE extension)",
            snippetLanguage = "toml",
            snippet = { w ->
                val (cmd, args) = launcher(w)
                "[mcp_servers.srutam]\ncommand = \"$cmd\"\nargs = [${jsonArray(args)}]\nstartup_timeout_sec = 60"
            },
            cliCommand = { w -> "codex mcp add srutam -- ${cliLine(w)}" },
            note = "startup_timeout_sec keeps the first npx download from looking like a failed server."
        ),
        McpClient(
            id = "gemini-cli", label = "Gemini CLI", group = McpClientGroup.TERMINAL,
            pathHint = "~/.gemini/settings.json (or .gemini/settings.json per project)",
            snippetLanguage = "json", snippet = ::standardServers
        ),
        McpClient(
            id = "opencode", label = "OpenCode", group = McpClientGroup.TERMINAL,
            pathHint = "~/.config/opencode/opencode.json",
            snippetLanguage = "json",
            snippet = { w ->
                val (cmd, args) = launcher(w)
                """
{
  "${'$'}schema": "https://opencode.ai/config.json",
  "mcp": {
    "srutam": {
      "type": "local",
      "command": [${jsonArray(listOf(cmd) + args)}],
      "enabled": true
    }
  }
}""".trimIndent()
            }
        ),
        McpClient(
            id = "qwen-code", label = "Qwen Code", group = McpClientGroup.TERMINAL,
            pathHint = "~/.qwen/settings.json (or .qwen/settings.json per project)",
            snippetLanguage = "json", snippet = ::standardServers
        ),
        McpClient(
            id = "crush", label = "Crush", group = McpClientGroup.TERMINAL,
            pathHint = "~/.config/crush/crush.json (Windows: %LOCALAPPDATA%\\crush\\crush.json)",
            snippetLanguage = "json",
            snippet = { w ->
                val (cmd, args) = launcher(w)
                """
{
  "mcp": {
    "srutam": {
      "type": "stdio",
      "command": "$cmd",
      "args": [${jsonArray(args)}]
    }
  }
}""".trimIndent()
            }
        ),
        McpClient(
            id = "amp", label = "Amp", group = McpClientGroup.TERMINAL,
            pathHint = "~/.config/amp/settings.json",
            snippetLanguage = "json",
            snippet = { w ->
                val (cmd, args) = launcher(w)
                """
{
  "amp.mcpServers": {
    "srutam": {
      "command": "$cmd",
      "args": [${jsonArray(args)}]
    }
  }
}""".trimIndent()
            },
            unverified = true
        ),
        McpClient(
            id = "copilot-cli", label = "Copilot CLI", group = McpClientGroup.TERMINAL,
            pathHint = "~/.copilot/mcp-config.json",
            snippetLanguage = "json", snippet = ::standardServers,
            note = "Copilot CLI may also want a \"type\": \"local\" field and a \"tools\": [\"*\"] list; check its docs.",
            unverified = true
        ),
        McpClient(
            id = "goose", label = "Goose", group = McpClientGroup.TERMINAL,
            pathHint = "~/.config/goose/config.yaml (or run: goose configure)",
            snippetLanguage = "yaml",
            snippet = { w ->
                val (cmd, args) = launcher(w)
                "extensions:\n  srutam:\n    name: srutam\n    type: stdio\n    cmd: $cmd\n    args: [${args.joinToString(", ")}]\n    enabled: true\n    timeout: 300"
            },
            unverified = true
        ),

        // ---- Editors & IDEs --------------------------------------------------
        McpClient(
            id = "cursor", label = "Cursor", group = McpClientGroup.EDITOR,
            pathHint = "~/.cursor/mcp.json or Settings > Features > MCP",
            snippetLanguage = "json", snippet = ::standardServers
        ),
        McpClient(
            id = "windsurf", label = "Windsurf", group = McpClientGroup.EDITOR,
            pathHint = "~/.codeium/windsurf/mcp_config.json",
            snippetLanguage = "json", snippet = ::standardServers
        ),
        McpClient(
            id = "vscode", label = "VS Code", group = McpClientGroup.EDITOR,
            pathHint = ".vscode/mcp.json, or \"MCP: Open User Configuration\" in the command palette",
            snippetLanguage = "json",
            snippet = { w ->
                val (cmd, args) = launcher(w)
                """
{
  "servers": {
    "srutam": {
      "type": "stdio",
      "command": "$cmd",
      "args": [${jsonArray(args)}]
    }
  }
}""".trimIndent()
            },
            note = "VS Code uses \"servers\", not \"mcpServers\"."
        ),
        McpClient(
            id = "zed", label = "Zed", group = McpClientGroup.EDITOR,
            pathHint = "settings.json (under context_servers)",
            snippetLanguage = "json",
            snippet = { w ->
                val (cmd, args) = launcher(w)
                """
{
  "context_servers": {
    "srutam": {
      "command": {
        "path": "$cmd",
        "args": [${jsonArray(args)}]
      }
    }
  }
}""".trimIndent()
            }
        ),
        McpClient(
            id = "kiro", label = "Kiro", group = McpClientGroup.EDITOR,
            pathHint = "~/.kiro/settings/mcp.json (or .kiro/settings/mcp.json per workspace)",
            snippetLanguage = "json", snippet = ::standardServers
        ),
        McpClient(
            id = "cline", label = "Cline", group = McpClientGroup.EDITOR,
            pathHint = "VS Code globalStorage/saoudrizwan.claude-dev/settings/cline_mcp_settings.json",
            snippetLanguage = "json", snippet = ::standardServers,
            note = "Or use Cline's MCP Servers panel > Configure."
        ),
        McpClient(
            id = "antigravity", label = "Antigravity", group = McpClientGroup.EDITOR,
            pathHint = "~/.gemini/config/mcp_config.json",
            snippetLanguage = "json", snippet = ::standardServers,
            unverified = true
        ),
        McpClient(
            id = "jetbrains", label = "JetBrains", group = McpClientGroup.EDITOR,
            pathHint = "Settings > Tools > AI Assistant > Model Context Protocol (paste as JSON)",
            snippetLanguage = "json", snippet = ::standardServers,
            unverified = true
        ),

        // ---- Desktop apps ----------------------------------------------------
        McpClient(
            id = "claude-desktop", label = "Claude Desktop", group = McpClientGroup.DESKTOP,
            pathHint = "claude_desktop_config.json (Settings > Developer > Edit Config)",
            snippetLanguage = "json", snippet = ::standardServers
        )
    )

    fun byId(id: String): McpClient = all.firstOrNull { it.id == id } ?: all.first()
}
