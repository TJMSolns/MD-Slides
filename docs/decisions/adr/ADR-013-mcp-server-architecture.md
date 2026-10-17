# ADR-013: MCP Server Architecture — File-in/File-out over Stateful Session

**Status:** Accepted
**Date:** 2026-05-17
**Deciders:** Tony Moores
**Related:** MS-011 (MCP capability surface spike)

## Context

mdslides is a Scala 3 / Mill / Cats Effect CLI tool that renders markdown presentation decks to HTML. As AI-agent workflows mature (Claude Code, Chora agents, demo-generation pipelines), there is demand to expose mdslides operations as Model Context Protocol (MCP) tools so agents can render presentations programmatically without spawning shell processes.

The spike (MS-011) evaluates two candidate architectures:

**Architecture A — File-in/file-out:** The MCP server wraps the existing CLI pipeline. Each tool invocation is stateless: it accepts file paths, performs the operation, and returns results. The agent is responsible for creating and managing the markdown source file (using Claude Code file tools or equivalent).

**Architecture B — Stateful session:** The MCP server maintains a `SlideDeck` in memory per session. Agents call tools like `create_deck`, `add_slide`, `set_slot_content` to build decks incrementally, then call `render` when ready. Requires session lifecycle management, in-memory domain manipulation, and cleanup.

**Near-term use cases driving the decision:**
1. **Chora pitch decks** — Agent generates marketing narrative as markdown, renders to HTML. High value.
2. **Demo slide generation** — Agent generates demo presentations (feature tours, product walkthroughs) from structured prompts. High value.
3. **ArcLight customer reports** — Agent formats order/engagement data as a presentation. Medium value (requires data access outside scope).

**Harvest pre-gate assessment:** No prior MCP server patterns exist in the TJMSolns org portfolio. ArcLight and SCKB do not yet expose MCP surfaces. This ADR establishes the org-first pattern.

## Decision

We will implement a **file-in/file-out MCP server** (Architecture A).

The MCP server wraps the existing mdslides rendering pipeline as a set of stateless tools. Agents generate or edit the markdown source using their own file tools (e.g., Claude Code's `Write`/`Edit`), then invoke mdslides MCP tools to validate and render.

### Rationale

1. **Compositional simplicity.** Agents already have capable file tools. Duplicating markdown editing inside the MCP server layer creates redundant complexity with no benefit. The agent's file tools + mdslides render tools compose cleanly.

2. **Matches existing mental model.** mdslides is a file-in → file-out pipeline today. The MCP server exposes exactly this contract. No new concepts for developers to learn.

3. **Stateless = no cleanup burden.** A stateful server requires session creation, incremental state management, timeout handling, and cleanup. All of this complexity disappears with stateless tools.

4. **Domain layer stays in Scala.** The domain model (`SlideDeck`, `Template`, `Slide`, validation) remains the single source of truth. A stateful MCP server would need to re-expose domain mutations through JSON APIs — a maintenance burden.

5. **Faster path to value.** File-in/file-out requires wrapping 2–3 existing operations. Stateful would require re-exposing the entire domain mutation surface.

### Rejected: Stateful Session (Architecture B)

Stateful session was evaluated and rejected because:
- It requires the agent to learn a new "deck building" API when it already knows how to write markdown
- Session lifecycle management (create/destroy, idle timeouts) adds operational complexity
- The domain model would need to be re-exposed as JSON-RPC mutations — a growing maintenance burden as the domain evolves
- Near-term use cases (pitch decks, demo generation) are all "generate once" workflows, not interactive editing sessions

## Capability Surface

The mdslides MCP server exposes four tools, grouped by tier:

### Tier 1 — Core (implement first)

| Tool | Signature | Returns | Use case |
|------|-----------|---------|----------|
| `render_deck` | `input_path: String, output_dir: String, theme?: String, no_copy_images?: Boolean` | `RenderResult` | Render markdown → HTML output directory |
| `validate_deck` | `input_path: String` | `ValidationResult` | Validate markdown without rendering; returns errors + warnings + slide count |

**`RenderResult`:**
```json
{
  "success": true,
  "output_dir": "/path/to/output",
  "files": ["index.html", "speaker.html"],
  "slide_count": 12,
  "warnings": [],
  "errors": []
}
```

**`ValidationResult`:**
```json
{
  "valid": true,
  "slide_count": 12,
  "errors": [],
  "warnings": ["Slide 4: body slot content exceeds density guideline"]
}
```

### Tier 2 — Discovery (implement second)

| Tool | Signature | Returns | Use case |
|------|-----------|---------|----------|
| `list_themes` | `themes_dir?: String` | `List[String]` | List available built-in and directory-based themes |
| `get_deck_info` | `input_path: String` | `DeckInfo` | Inspect deck without rendering: slide count, templates used, images referenced |

**`DeckInfo`:**
```json
{
  "slide_count": 12,
  "templates_used": ["title", "two-column", "body"],
  "images_referenced": ["images/logo.png"],
  "has_mermaid_diagrams": false
}
```

### Not in scope

- `preview_slide` — requires a browser/headless renderer; out of scope
- `export_as_pdf` — mdslides does not produce PDF; out of scope
- Stateful mutation tools (`add_slide`, `set_slot_content`, etc.) — rejected; see Decision above

## Implementation Architecture

**New module:** `mcp` in the Mill monorepo.

Dependency chain: `mcp` → `infrastructure` → `domain`. The `mcp` module has no compile-time dependency on `cli`.

**Protocol:** MCP stdio transport (JSON-RPC 2.0 over stdin/stdout). Standard MCP handshake + `tools/list` + `tools/call`.

**Tech stack:** Scala 3, Cats Effect (consistent with existing modules). No ZIO — mdslides predates org ZIO default and this module follows the existing effect system.

**MCP library:** No established Scala MCP library exists. Implement a lightweight stdio transport layer directly. The MCP protocol at this layer is simple: parse JSON-RPC from stdin, dispatch to tool handlers, write JSON response to stdout. Estimated: ~200 LoC for protocol layer.

**Invocation:** `mill mcp.run` (or a companion assembly target for standalone use).

```
claude code (Write markdown) → mdslides MCP: render_deck(path, out_dir) → HTML files
                              → mdslides MCP: validate_deck(path)       → errors/warnings
```

## Consequences

### Positive
- Agents can render presentations without shell access
- Composable with Claude Code file tools (the natural pairing)
- Stateless tools are easy to test in isolation
- Implementation reuses the existing rendering pipeline with no changes

### Negative
- Agents must manage markdown source files themselves (not a limitation in practice — Claude Code handles this)
- No streaming output — `render_deck` blocks until rendering is complete (acceptable for presentation-scale workloads)

### Neutral
- The `mcp` module adds a fourth Mill module; build time impact is negligible

## Implementation Order

1. Tier 1: `render_deck` + `validate_deck` (covers all near-term use cases)
2. MCP protocol layer (stdio transport, JSON-RPC dispatch)
3. Integration test: end-to-end render via MCP protocol
4. Tier 2: `list_themes` + `get_deck_info` (after Tier 1 is validated in production use)

**Pre-scaffold gate:** Sequence diagrams for `render_deck` happy path and primary error path must exist before implementation begins.

## Related Documents

- [ADR-006: Rendering Architecture](ADR-006-rendering-architecture.md)
- [ADR-007: Pure Functional Domain](ADR-007-pure-functional-domain.md)
- MS-011 (mdslides WORK-QUEUE — MCP capability surface spike)

---

**Decision Date:** 2026-05-17
**Next Review:** After Tier 1 implementation is complete and used in at least one production workflow
