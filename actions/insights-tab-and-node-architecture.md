# Srutam Insights Tab and Connected Thought Architecture

## Purpose

This document defines how Srutam should evolve from an audio-note viewer into a low-effort review system.

The user should be able to:

1. Capture a voice note in under two seconds.
2. Let Srutam transcribe it locally.
3. Receive useful AI organization without manually tagging or sorting anything.
4. Revisit only the parts that deserve attention: things to do, ideas worth remembering, decisions already made, and themes that keep recurring.

The Insights experience must reduce work after capture. It must not become another inbox that the user has to maintain.

## Product decision

Keep an Insights destination, but define it as a passive review surface rather than an analytics dashboard or a second notes library.

The three primary destinations should have distinct jobs:

| Destination | User question | Responsibility |
|---|---|---|
| Notes | What did I record? | Browse, play, search, rename, delete, and open recordings. |
| Insights | What deserves my attention? | Surface explicit actions, ideas, decisions, and recurring themes. |
| Ask | What do my notes say about this? | Interactive cross-note questions and retrieval. |

The existing Studio recording bar remains global and unchanged. Insights should not copy or redesign the recording controls.

## Insights screen structure

### Header

Use the existing shared Srutam top bar:

- Title: `Srutam Insights`
- Subtitle: `Audio transcribed on this device`
- Shared squircle Settings button
- Optional refresh/reprocess action only when needed

Privacy wording must be precise. Audio transcription is local, but optional cloud analysis may receive transcript text. A better supporting message is:

> Audio stays on this device. Optional AI analysis uses your transcript.

Do not imply that summaries and chat are also fully on-device unless they actually are.

### Default view: All insights

The first view should be an overview rather than forcing the user into one empty category.

Recommended order:

1. **Needs attention**
   - Open action items only.
   - Show the source note and date.
   - Show a compact `Completed` or `Archive` row below the open items.
   - If there are no actions, show a positive state such as: `Nothing you need to do from your notes yet.`

2. **Recent decisions**
   - A chronological log grouped by month.
   - No checkbox, progress percentage, or task language.
   - Each entry opens the source note.

3. **Themes you keep returning to**
   - Display only when a theme appears in at least three distinct notes and passes the confidence rules.
   - Example: `You have mentioned a walking-friendly recording flow in 3 notes.`
   - Expand to show the supporting notes inline.
   - Provide a dismiss/not-related action to protect trust.

4. **Recent ideas**
   - Show distilled ideas that are not actions or decisions.
   - Include source note and date.
   - Show recurrence only when there is evidence from other notes.

The overview may be implemented as sections in one scrollable screen. It should not require the user to explore a graph or open several screens to understand what Srutam found.

### Category filter capsule

Use a segmented capsule below the header:

`All  |  Next Steps  |  Ideas  |  Decisions`

The capsule is a filter, not four identical list screens.

#### Next Steps

- Open actions first.
- Checkbox for completion.
- Completed actions collapse into an Archive section.
- `Clear Completed` means archive, never delete.
- Preserve source note, evidence, and completion history.

#### Ideas

- No checkbox.
- Idea text, source note, and date.
- Optional recurrence pill such as `3 notes` or `Seen 3x`.
- Inline `Related (n)` accordion.
- Related items show the actual supporting text, not just a title.
- Allow `Not related` or `Dismiss pattern` to prevent bad matches from repeating.

#### Decisions

- Plain chronological timeline grouped by month.
- Small marker or timeline dot.
- Decision text, source note, and date.
- Optional rationale only when explicitly extracted.
- No completion state and no recurrence pressure.

## What the AI should produce per note

The current summary/key-points/action-items/WIIFM pipeline should become a structured, versioned analysis contract.

```json
{
  "schemaVersion": 1,
  "title": "Short topic title",
  "summary": "Grounded two-sentence summary.",
  "whatsInItForMe": "Concrete personal value, or an honest no-clear-benefit result.",
  "keyPoints": [
    {
      "text": "Factual takeaway from this note",
      "evidence": "Short supporting quote"
    }
  ],
  "actionItems": [
    {
      "text": "Explicit task or commitment",
      "isExplicit": true,
      "dueDate": null,
      "evidence": "Short supporting quote"
    }
  ],
  "ideas": [
    {
      "text": "A distinct possibility, proposal, or thought",
      "evidence": "Short supporting quote"
    }
  ],
  "decisions": [
    {
      "text": "An explicit conclusion or choice",
      "rationale": null,
      "evidence": "Short supporting quote"
    }
  ]
}
```

### Extraction rules

- Never invent action items.
- Only create a decision when the speaker reached or stated a conclusion.
- Do not turn a vague possibility into a decision.
- Do not duplicate the same sentence as a key point, idea, action, and decision.
- Allow empty arrays.
- WIIFM must be grounded in the note. `No clear personal benefit stated` is valid.
- Preserve evidence for every extracted item where possible.
- Store the prompt/model/schema version for future reprocessing.

The distinction must remain clear:

| Output | Meaning |
|---|---|
| Summary | What the note was about. |
| WIIFM | Why this note may matter to the user. |
| Key point | Important factual takeaway from this note. |
| Idea | A possibility or thought worth remembering. |
| Decision | A conclusion or choice that was made. |
| Action item | Something explicitly stated that should be done. |

## Local Android architecture

### Capture and persistence

When recording stops:

1. Finalize the audio file.
2. Insert a `Recording` row immediately.
3. Mark the row `CAPTURED`.
4. Enqueue processing work with the recording ID.

The recording service must not depend on a Compose screen remaining alive. The current service database-save method is only a placeholder and must become a real repository/database write.

### Processing pipeline

Use durable background work, preferably WorkManager, rather than performing the whole pipeline inside a ViewModel:

```text
Recording finished
  -> Room Recording(CAPTURED)
  -> LocalTranscriptionWorker
  -> Room transcript(TRANSCRIBED)
  -> CloudAnalysisWorker when user permits and network exists
  -> Room structured insight rows(READY)
  -> Local keyword/related index update
  -> Compose flows refresh automatically
```

Recommended statuses:

```text
CAPTURED
TRANSCRIBING
TRANSCRIBED
WAITING_FOR_NETWORK
ANALYZING
READY
ERROR
```

Transcription should work without a network connection. Cloud analysis should be opt-in or clearly disclosed because transcript text may leave the device even though raw audio does not.

### Room model

Do not keep global actions, ideas, and decisions only as JSON arrays inside `Recording`. They need to be queryable independently and eventually synced to the Cloud/MCP backend.

Recommended local tables:

```text
recordings
- id
- audioFilePath
- timestamp
- duration
- title/name
- transcript
- summary
- whatsInItForMe
- aiStatus
- analysisVersion
- createdAt
- updatedAt

insight_items
- id
- recordingId
- kind: ACTION / IDEA / DECISION
- text
- evidence
- rationale
- dueDate
- sourceOrder
- createdAt
- status: OPEN / COMPLETED / ARCHIVED
- completedAt
- archivedAt
- recurrenceGroupId

related_insights
- id
- insightId
- relatedInsightId
- similarityScore
- relationshipType
- createdAt
- dismissed
```

Use a foreign key from `insight_items.recordingId` to `recordings.id` with cascade deletion. The existing `ActionItem` class is currently a UI/data-transfer model; it should become a proper Room entity or be replaced by the shared entity above.

### Repository and ViewModel responsibilities

The repository should own:

- Recording creation after capture.
- Processing state transitions.
- Insight insertion/replacement for an analysis version.
- Action completion and archive operations.
- Queries for overview, actions, ideas, decisions, and related items.

The ViewModel should expose screen-ready flows such as:

```text
insightsOverview
openActions
archivedActions
recentIdeas
recentDecisions
recurringThemes
```

Compose should render state and send user intents. It should not parse JSON, run AI, calculate similarity, or directly rename files.

## The 2D connected-node view

The node view can be useful, but only if it has a job beyond looking interesting.

### What the nodes represent

Possible node types:

- Recording: a source note.
- Idea: an extracted thought.
- Decision: an extracted conclusion.
- Action: an extracted task.
- Theme: a recurring cluster of similar ideas.

Edges can represent:

- `RELATED_TO`: semantically similar ideas.
- `FROM_NOTE`: an insight belongs to a recording.
- `LED_TO`: a decision produced an action.
- `REPEATS`: multiple notes express the same theme.

### Where it helps

The graph is most useful for deliberate review sessions, not everyday capture. It can help the user:

1. See how an idea evolved across multiple recordings.
2. Understand which decisions produced follow-up actions.
3. Trace an action back to the original thought and evidence.
4. Discover that several notes are about the same project or concern.
5. Give an AI agent a visual explanation of the relationship between a note, decision, and task.

Example:

```text
Idea: local transcription
        |
        | led to
        v
Decision: use Sherpa-ONNX
        |
        | created
        v
Action: benchmark latency on mid-range phones
```

That is useful because the graph explains a chain of reasoning. A graph that only says “these sentences are similar” is decoration.

### When not to show it

Do not show the graph as the default Insights screen because:

- It requires interpretation.
- It becomes noisy with many notes.
- A privacy-conscious, low-effort user may never explore it.
- It duplicates the value of related-note accordions.
- Incorrect edges are more confusing than missing edges.

Make it a secondary `Explore connections` action from a recurring theme, idea, or decision. The entry point should already explain why the graph exists:

> Explore how this decision connects to 4 ideas and 2 actions.

### Graph interaction requirements

If implemented, every node and edge must be actionable:

- Tap a node to open the source note or insight detail.
- Tap an edge to show the relationship and similarity reason.
- Filter by Actions, Ideas, Decisions, or Themes.
- Show a legend.
- Support reset/recenter.
- Avoid requiring users to drag nodes just to understand the graph.
- Provide a list view fallback for accessibility and small screens.

Use a simple Compose `Canvas`/layout-based visualization first. A real 3D scene, physics engine, or immersive spatial view is unnecessary for this product.

## Recurrence and similarity strategy

Do not make recurrence detection a launch blocker for the Insights tab.

### First version

Use conservative local matching:

- Normalize text.
- Remove generic stop words.
- Compare important terms with BM25/Jaccard-style overlap.
- Require matches from distinct recordings.
- Store computed relationships when a new note arrives.

Use language such as `Related to 2 notes` until the system is proven reliable.

### Later embedding version

When an on-device embedding model is available:

1. Embed each idea and decision after extraction.
2. Compare only against existing insights.
3. Cache pair scores.
4. Cluster only when similarity and evidence are strong.
5. Require at least three separate notes before creating a prominent recurring-theme banner.

The proposed `0.75` cosine threshold is a starting experiment, not a product truth. Build a small manually labeled test set and optimize for precision. A false claim that the user repeatedly thinks about something will erode trust faster than a missed connection.

Add user feedback controls:

- `Not the same idea`
- `Dismiss theme`
- `Show related notes`

## Cloud and MCP compatibility

The Srutam Cloud/MCP plan should consume the same structured insight model rather than inventing a second representation.

The cloud layer can sync:

- Note metadata, transcript, summary, WIIFM, and key points.
- Action items and their completion/archive state.
- Ideas and decisions.
- Recurrence groups or related-note references.
- Agent work logs.

Raw audio should remain local by default.

The MCP server can then expose meaningful tools:

- Search notes and insights.
- List open actions.
- Get decisions for a project or date range.
- Show recurring themes.
- Update an action item.
- Append an agent work log.

Important sync rules:

- Local IDs and remote IDs must be mapped explicitly.
- Sync must be idempotent.
- Agent completion must record `completedBy` and timestamp.
- A private/excluded note must not leak its insights through search, related-note results, or MCP responses.
- Conflict resolution must preserve the user's local completion/archive decision unless the user explicitly chooses otherwise.

The Android app should remain useful without an account. Cloud Sync and MCP should be opt-in features.

## Implementation sequence

### Phase 1: Correct the current lifecycle

- Insert recordings into Room when recording stops.
- Ensure files and database rows stay synchronized.
- Move processing out of UI-only button callbacks.
- Add durable status transitions and retry handling.
- Fix the public-storage/private-storage messaging mismatch.

### Phase 2: Stabilize AI analysis

- Replace the current response model with the versioned structured schema.
- Add ideas and decisions.
- Require explicit action extraction.
- Store evidence for extracted items.
- Store analysis model/prompt version.
- Keep transcript processing local and cloud analysis transparent.

### Phase 3: Build Insights V1

- Replace the current Actions screen with the Insights overview.
- Add `All`, `Next Steps`, `Ideas`, and `Decisions` filters.
- Add proper database-backed action completion.
- Add manual archive for completed actions.
- Add useful empty states.

### Phase 4: Add related content

- Implement conservative BM25 related-note matching.
- Add inline related accordions.
- Add recurrence counts only with enough evidence.
- Add dismiss/not-related feedback.

### Phase 5: Add optional connected view

- Add `Explore connections` from a theme or insight.
- Render a small 2D graph with list fallback.
- Make edges explainable and tappable.
- Evaluate usage before promoting it into the primary navigation.

### Phase 6: Integrate Cloud/MCP

- Reuse the local insight entities for cloud sync.
- Add note privacy controls.
- Add conflict-safe action synchronization.
- Add MCP search, detail, actions, and work-log tools.
- Test the full phone-to-agent-to-phone loop.

## Definition of done

Insights is ready for a first release when:

- A completed recording appears in Room without opening the app again.
- Local transcription works offline.
- AI analysis never fabricates action items.
- Ideas and decisions are stored as queryable rows.
- The default Insights screen is useful when there are zero actions.
- Completed actions can be archived without data loss.
- Related-note results show their evidence and can be dismissed.
- The graph, if present, explains relationships and is not required for normal use.
- Cloud/MCP sync can reuse the same structured records without exposing private notes.

## First engineering task list

- [ ] Add `keyIdeas` and `decisions` to the analysis contract.
- [ ] Introduce `InsightItem` Room entity and migration strategy.
- [ ] Make the recording service insert a database row on stop.
- [ ] Add WorkManager transcription/analysis chain.
- [ ] Replace `AppPreferences` action completion storage with Room fields.
- [ ] Build `InsightsOverviewScreen` from Room flows.
- [ ] Implement distinct Next Steps, Ideas, and Decisions composables.
- [ ] Add empty states and manual archive behavior.
- [ ] Add evidence text to every extracted insight.
- [ ] Implement conservative related-note matching.
- [ ] Add privacy/exclusion fields required by the Cloud/MCP plan.
- [ ] Add MCP sync only after the local insight lifecycle is stable.
- [ ] Prototype the 2D graph as a secondary exploration surface, not the default tab.
