# Srutam Agent Operating System

This project is orchestrated using the global Antigravity Universal Agent System.

## Specialist Universal Agents
The following global specialist agents are active and bound to this workspace:
- **`state-keeper`**: Keeps `.agents/state/` factual and updated with milestone progress.
- **`qa-release-agent`**: Executes builds, tests, checks logs, and verifies APKs/binaries.
- **`mobile-ui-agent`**: Builds and polishes UI screens, animations, and edge-to-edge insets.
- **`ai-integration-agent`**: Implements LLM inference, prompts, and streaming pipelines.
- **`store-assets-agent`**: Produces store graphics (1024x500 banner, screenshots, icon).
- **`growth-marketing-agent`**: ASO copywriting, keywords, and app store listings.
- **`dev-broadcaster-agent`**: Formats git milestones into changelogs and social posts.

## Operating Protocol
- Keep project memory up to date in `.agents/state/current.md` and `.agents/state/decisions.md`.
- Build & verify before declaring milestones complete.


## Marketing & Social Updates (/marketing Trigger)
- **Strictly On-Demand**: Do NOT generate marketing copy, social posts, or video scripts during regular coding, bugfixing, or UI turns. Keep engineering turns focused entirely on the code and tests.
- **When Triggered by `/marketing` or `/growth`**:
  1. Invoke the `growth-marketing-agent`.
  2. Inspect the latest changes, git commits, or milestone progress in `.agents/state/current.md`.
  3. Formulate 1 punchy X/Twitter post (< 280 chars) and 1 short-form demo script following `builder-voice-and-style.md` (strict zero em dashes, casual, authentic builder voice).
  4. Append to `.agents/state/social-drafts.md` and present to the user for review.
