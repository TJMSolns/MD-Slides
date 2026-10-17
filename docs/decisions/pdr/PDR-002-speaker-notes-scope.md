# PDR-002: Speaker Notes v1.0 Scope

**Status**: Accepted
**Date**: 2024-12-20
**Decider**: Tony Moores (Product Owner)
**Consulted**: Tony Moores (Architect, Team)
**Related Ceremony**: US-004 (Speaker Notes Parsing)

---

## Context

Speaker notes are a common feature in presentation tools:
- **PowerPoint**: Notes pane below each slide
- **Google Slides**: Speaker notes panel
- **Marp**: Supports notes via HTML comments

**User Need**: Authors want to include speaker notes (talking points, reminders, references) that don't appear on slides.

**Business Question**: Should v1.0 support speaker notes? If so, how much functionality?

**Constraints**:
- v1.0 timeline: 6-8 weeks
- Must prioritize core pipeline (parse → validate → render → CLI)
- Speaker notes rendering requires separate UI (speaker view vs. audience view)
- Mobile presenter apps (iOS, Android) not in scope for v1.0

---

## Decision

**v1.0 Scope**: **Parse-only** (no rendering)

**What v1.0 WILL do**:
- Parse speaker notes from Markdown (HTML comment syntax)
- Store notes in `Slide` domain model (`notes: Option[String]`)
- Include notes in internal data model (for future use)

**What v1.0 WILL NOT do**:
- Render notes in HTML output (only slides visible)
- Provide "speaker view" (dual-screen mode)
- Export notes to PDF or separate document
- Mobile presenter app integration

**Markdown Syntax** (v1.0):
```markdown
# Slide Title

Slide content here.

<!-- NOTES
These are speaker notes.
- Talking point 1
- Talking point 2
-->

---

# Next Slide
```

**Domain Model**:
```scala
case class Slide(
  id: Int,
  template: String,
  slots: Map[String, String],
  notes: Option[String]  // Added in v1.0, rendered in v1.1
)
```

**Deferred to v1.1** (US-034):
- Render notes in speaker view (separate HTML page)
- Dual-screen mode (audience view + speaker view)
- Timer integration (elapsed/remaining time)
- Next slide preview in speaker view

---

## Consequences

### Positive

1. **Future-Proof**: Notes stored in model, ready for v1.1 rendering
2. **MVP Focus**: v1.0 delivers core value (slide generation) faster
3. **User Feedback**: Can validate notes syntax before building complex UI
4. **No Risk**: Parse-only is low-risk (no UI complexity)
5. **Compatibility**: Notes syntax compatible with Marp (easier migration)

### Negative

1. **Incomplete Feature**: Users expect notes rendering (not just parsing)
2. **Workaround Needed**: Users must use external tools (PowerPoint, Google Slides) for speaker view
3. **Competitive Gap**: Marp renders notes (MDSlides doesn't in v1.0)

### Mitigations

1. **Clear Roadmap**: Document v1.1 speaker view in BACKLOG-V3.md
2. **User Communication**: Explain parse-only scope in release notes
3. **Fast Follow**: Prioritize v1.1 speaker view (within 4 weeks of v1.0 release)

---

## Alternatives Considered

### Alternative A: No Speaker Notes in v1.0
**Rationale**: Skip notes entirely, add in v2.0
**Why Rejected**:
- Parsing is trivial (HTML comments already parsed by Flexmark)
- Missing from domain model requires breaking change later
- Users will ask for it immediately after v1.0 release

### Alternative B: Full Speaker View in v1.0
**Scope**: Parse + render notes in speaker view UI
**Why Rejected**:
- Adds 2-3 weeks to v1.0 timeline (significant delay)
- Complex UI (dual-screen mode, timer, next slide preview)
- Not critical for MVP (slides themselves are critical)
- Risk of scope creep (mobile apps, etc.)

### Alternative C: Notes in Slide Footer
**Approach**: Render notes at bottom of each slide (visible to audience)
**Why Rejected**:
- Defeats purpose of speaker notes (should be private)
- Clutters slides (bad UX)
- Not standard behavior (PowerPoint, Marp don't do this)

### Alternative D: Export Notes to Separate File
**Approach**: Generate `notes.txt` or `notes.md` alongside HTML
**Why Rejected**:
- Still doesn't solve speaker view problem (authors need on-screen view)
- Extra file to manage (not standalone HTML anymore)
- Could be v1.2 feature (but not v1.0 priority)

---

## Implementation Notes

### Parsing Speaker Notes

```scala
import com.vladsch.flexmark.ast.HtmlCommentBlock

def parseSlide(nodes: List[Node]): Slide = {
  val notes = nodes.collectFirst {
    case comment: HtmlCommentBlock if comment.getChars.toString.trim.startsWith("NOTES") =>
      extractNotes(comment.getChars.toString)
  }

  Slide(
    id = slideId,
    template = inferTemplate(nodes),
    slots = extractSlots(nodes),
    notes = notes  // Optional
  )
}

def extractNotes(commentText: String): String = {
  // Remove "<!-- NOTES" prefix and "-->" suffix
  commentText
    .stripPrefix("<!--")
    .stripSuffix("-->")
    .stripPrefix("NOTES")
    .trim
}
```

### Validation (v1.0)

**No validation for speaker notes** (they're optional, freeform text):
- No length limits (authors can write as much as needed)
- No markdown rendering validation (notes stored as raw text)
- No required/optional checks (notes always optional)

### Storage in Domain Model

```scala
// v1.0: Notes stored but not rendered
case class Slide(
  id: Int,
  template: String,
  slots: Map[String, String],
  notes: Option[String]  // Present in v1.0, unused in rendering
)

// v1.1: Notes rendered in speaker view
def renderSpeakerView(deck: SlideDeck, theme: Theme): Html = {
  // Will access slide.notes in v1.1
}
```

### CLI Behavior (v1.0)

```bash
# Generate HTML (slides only, notes not rendered)
mdslides slides.md output.html

# Validate (includes parsing notes, but no note-specific validation)
mdslides slides.md --validate-only
✓ Validation passed (12 slides, 5 with speaker notes)

# Notes present but not rendered
cat output.html  # No notes visible in HTML
```

---

## User Experience

### v1.0 (Parse-Only)

**User writes**:
```markdown
# Key Metrics

- Revenue: $1.2M
- Growth: 15% YoY

<!-- NOTES
Emphasize growth rate (competitors at 10%).
Pause after revenue number for impact.
If asked about Q4 projections, defer to CFO.
-->
```

**MDSlides behavior**:
1. Parses notes successfully (stored in `Slide` model)
2. Validates markdown (no errors)
3. Generates HTML (slides only, notes not visible)
4. User must use external tool for speaker view (PowerPoint import, printed notes, etc.)

**User feedback expected**:
- "Where are my notes?" → Document in FAQ: notes rendering in v1.1
- "Can I export notes?" → Roadmap item for v1.2
- "Is this compatible with Marp?" → Yes, same syntax

### v1.1 (Rendering)

**Additional functionality** (US-034):
```bash
# Generate speaker view (separate HTML file)
mdslides slides.md --speaker-view speaker.html

# Dual-screen mode (browser feature)
open output.html speaker.html
# Drag speaker.html to second monitor
```

**Speaker view features**:
- Current slide notes displayed
- Next slide preview
- Timer (elapsed/remaining)
- Slide navigation controls

---

## Roadmap Integration

### v1.0 MVP (Current)
- ✅ Parse speaker notes (US-004)
- ✅ Store in domain model
- ❌ No rendering (deferred)

### v1.1 (Next Release - 4 weeks after v1.0)
- ⏭️ US-034: Render Speaker Notes
  - Generate speaker view HTML
  - Dual-screen support (CSS media queries)
  - Timer integration
  - Next slide preview

### v1.2 (Future)
- Export notes to PDF
- Export notes to Markdown file
- Notes search/filter

### v2.0 (Long-Term)
- Mobile presenter app (iOS, Android)
- Remote control (phone as clicker)
- Audience Q&A integration

---

## Competitive Analysis

| Feature | MDSlides v1.0 | MDSlides v1.1 | Marp | PowerPoint |
|---------|---------------|---------------|------|------------|
| Parse notes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| Render notes | ❌ No | ✅ Yes | ✅ Yes | ✅ Yes |
| Speaker view | ❌ No | ✅ Yes | ✅ Yes | ✅ Yes |
| Dual-screen | ❌ No | ✅ Yes | ✌️ Manual | ✅ Yes |
| Timer | ❌ No | ✅ Yes | ❌ No | ✅ Yes |
| Mobile app | ❌ No | ❌ No | ❌ No | ✅ Yes |

**v1.0 Positioning**: "Core slide generation with notes support coming in v1.1"
**v1.1 Positioning**: "Full speaker view with timer and dual-screen support"

---

## Communication Plan

### Release Notes (v1.0)

**Speaker Notes (Parse-Only)**:
- MDSlides v1.0 parses speaker notes using HTML comment syntax (`<!-- NOTES ... -->`)
- Notes are stored but not rendered in HTML output
- **Coming in v1.1**: Speaker view with timer, next slide preview, and dual-screen support
- **Workaround**: Use external tools (PowerPoint, Google Slides) for speaker view in v1.0

### Documentation

**User Guide Section: "Speaker Notes (v1.0)"**:

> Speaker notes allow you to include talking points and reminders that don't appear on slides.
>
> **Syntax**:
> ```markdown
> # Slide Title
>
> Slide content here.
>
> <!-- NOTES
> Your speaker notes here.
> - Talking point 1
> - Talking point 2
> -->
> ```
>
> **v1.0 Limitation**: Notes are parsed but not rendered. Speaker view coming in v1.1 (target: January 2025).
>
> **Workaround**: Export slides to PowerPoint or print notes separately.

### FAQ

**Q: Where are my speaker notes in the HTML?**
A: v1.0 parses notes but doesn't render them. Speaker view UI coming in v1.1 (4 weeks after v1.0 release).

**Q: Can I use MDSlides for presentations now?**
A: Yes! Slides render perfectly. For speaker notes, use external tools (PowerPoint import, printed notes) until v1.1.

**Q: Is the notes syntax stable?**
A: Yes. The `<!-- NOTES ... -->` syntax is final and compatible with Marp.

---

## Related Ceremonies

- **US-004**: Speaker Notes Parsing (Three Amigos + Example Mapping)
- **US-034**: Speaker Notes Rendering (deferred to v1.1)
- **Backlog Planning**: Scope boundaries discussion

---

## Related Governance

- **PDR-006**: v1.0 MVP Scope Boundaries (this decision part of scope discussion)
- **ADR-004**: Slide Separator Design (notes parsed as part of slide)
- **POL-001**: Ubiquitous Language (use "Speaker Notes" not "Presenter Notes" or "Notes")

---

**Decision Owner**: Tony Moores (Product Owner)
**Business Impact**: Low (parse-only doesn't block MVP)
**User Impact**: Medium (users expect rendering, but workarounds exist)
**Timeline Impact**: High (saves 2-3 weeks by deferring rendering to v1.1)
**Reversibility**: High (adding rendering in v1.1 is non-breaking change)
