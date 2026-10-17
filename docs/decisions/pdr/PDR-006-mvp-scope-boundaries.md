# PDR-006: v1.0 MVP Scope Boundaries

**Status**: Accepted
**Date**: 2024-12-20
**Decider**: Tony Moores (Product Owner)
**Consulted**: Tony Moores (Architect, Team)
**Related Ceremony**: Backlog Bucketing Session, All Phase 2 Ceremonies

---

## Context

MDSlides has a large feature surface area. Without clear boundaries, scope creep is inevitable:

**Potential Features**:
- Core pipeline (parsing, validation, rendering, CLI)
- Advanced templates (code, image, quote, data, two-column)
- PDF export
- Speaker view (dual-screen presenter mode)
- Slide transitions/animations
- Mobile presenter app
- Real-time collaboration
- Version control integration
- Plugin system
- Cloud hosting
- AI-assisted content generation

**Business Question**: What is the MINIMUM viable product for v1.0?

**Constraints**:
- **Timeline**: 6-8 weeks development (Phase 3 TDD implementation)
- **Team Size**: 1 developer (Tony Moores)
- **Goal**: Validate core value proposition (Markdown → HTML slides with density validation)
- **Risk**: Feature creep delays launch, prevents user feedback

---

## Decision

**v1.0 MVP Scope**: 12 User Stories (Core Pipeline Only)

### IN SCOPE ✅

**Epic 1: Slide Parsing** (3 stories)
- US-001: Title Slide Template
- US-002: Content Slide Template
- US-003: Slide Separation (--- separator)
- US-004: Speaker Notes Parsing (parse-only, no rendering)

**Epic 2: Validation Framework** (5 stories)
- US-011: Structure Validation (template binding, required slots)
- US-012: Density Validation (12 lines, 150 words, 80 chars)
- US-013: Content Validation (template constraints)
- US-015: Error Collection (NonEmptyList pattern)
- US-009: Theme Validation (WCAG AA, font minimums)

**Epic 3: Theme System** (1 story)
- US-008: Theme Loading (JSON themes, CSS generation)

**Epic 4: Rendering** (1 story)
- US-016: HTML Rendering (Scalatags, standalone HTML, keyboard navigation)

**Epic 5: CLI Interface** (1 story)
- US-019: Generate Slides via CLI (mdslides input.md output.html)

**Total**: 12 stories, ~220 KB ceremony documentation

### OUT OF SCOPE ❌ (Deferred to v1.1+)

**v1.1 (4-6 weeks after v1.0)**:
- US-005: Code Slide Template (syntax highlighting)
- US-006: Image Slide Template (responsive images, alt text)
- US-034: Speaker Notes Rendering (speaker view, timer, next slide preview)
- US-020: Watch Mode (auto-regenerate on file change)
- US-021: PDF Export (via headless Chrome)

**v1.2 (3-4 months after v1.0)**:
- US-007: Quote Slide Template
- US-023: Two-Column Layout Template
- US-024: Data/Table Slide Template
- US-025: Custom Template Support (user-defined templates)
- US-026: Slide Transitions (CSS animations)

**v2.0 (6-12 months after v1.0)**:
- US-030: Plugin System (extend MDSlides functionality)
- US-031: Mobile Presenter App (iOS/Android remote control)
- US-032: Real-Time Collaboration (Google Docs-style co-editing)
- US-033: Version Control Integration (Git diff for slides)
- US-035: Cloud Hosting (publish slides to mdslides.io)

**Never (Out of Mission)**:
- WYSIWYG editor (MDSlides is Markdown-first)
- PowerPoint import (too complex, low ROI)
- Video embedding (browser native support sufficient)

---

## Consequences

### Positive

1. **Fast Time-to-Market**: 12 stories achievable in 6-8 weeks (vs. 6 months for full feature set)
2. **User Feedback Early**: Can validate value proposition before building advanced features
3. **Reduced Risk**: Small MVP = less code to maintain, fewer bugs
4. **Clear Roadmap**: Deferred features create clear v1.1, v1.2 roadmap
5. **Focus**: Team can focus on quality (no rushing to add features)

### Negative

1. **Feature Requests**: Users will immediately ask for PDF export, speaker view
2. **Competitive Gap**: Marp has features MDSlides v1.0 lacks (code slides, PDF)
3. **Incomplete Story**: Some workflows require v1.1 features (e.g., code presentations)

### Mitigations

1. **Clear Roadmap**: Document v1.1 features in BACKLOG-V3.md (set expectations)
2. **Fast Follow**: Commit to v1.1 release within 4-6 weeks of v1.0
3. **Communication**: Blog post explaining MVP strategy, roadmap visibility

---

## Alternatives Considered

### Alternative A: Larger MVP (20 Stories)
**Scope**: Core pipeline + code slides + image slides + PDF export + speaker view
**Why Rejected**:
- 3-4 months development (too long without user feedback)
- High risk (building features nobody wants)
- Complexity (more bugs, harder to maintain)

### Alternative B: Smaller MVP (8 Stories)
**Scope**: Core pipeline only (no themes, no speaker notes parsing)
**Why Rejected**:
- Too minimal (themes are core differentiator)
- Speaker notes parsing is trivial (no rendering cost)
- Themes enable corporate adoption (critical for growth)

### Alternative C: Feature Parity with Marp
**Scope**: Match Marp feature-for-feature before launch
**Why Rejected**:
- Defeats purpose of MVP (validate unique value first)
- Marp took years to reach current state
- MDSlides differentiator is density validation (not feature count)

### Alternative D: No Roadmap (Reactive)
**Approach**: Launch v1.0, add features based on user requests only
**Why Rejected**:
- No vision (just copying competitors)
- Reactive roadmap = inconsistent product
- Some features take months (can't wait for requests)

---

## Implementation Strategy

### Phase 3 TDD Implementation Order

**Week 1-2**: Parsing (US-001, 002, 003, 004)
- Foundation stories (everything depends on parsing)
- Simplest to implement (well-understood from ceremonies)

**Week 3-4**: Validation (US-011, 012, 013, 015, 009)
- Core value proposition (density validation)
- Most complex logic (needs careful TDD)

**Week 5**: Themes + Rendering (US-008, 016)
- Brings everything together (visual output)
- Validates entire pipeline end-to-end

**Week 6**: CLI (US-019)
- User-facing interface (glues all modules together)
- Integration testing

**Week 7-8**: Polish, Documentation, Release
- Bug fixes
- Performance optimization
- User guide, API docs
- Release preparation

---

## Success Criteria (v1.0)

### Functional Criteria
- ✅ Parse title + content slides from Markdown
- ✅ Validate structure, density, content
- ✅ Load themes (JSON), validate WCAG AA compliance
- ✅ Render standalone HTML (CSS/JS inlined)
- ✅ CLI generates slides with clear error messages
- ✅ Exit code 0 (success) / 1 (error)

### Non-Functional Criteria
- ✅ Rendering < 500ms for 50-slide deck
- ✅ HTML file < 100 KB/slide
- ✅ Browser compatibility (Chrome, Firefox, Safari, Edge)
- ✅ Keyboard navigation (→, ←, Space, Home, End)
- ✅ Domain layer pure (no side effects, testable)

### Quality Criteria
- ✅ Domain layer ≥ 90% line coverage
- ✅ All BDD scenarios passing
- ✅ Zero critical bugs
- ✅ Documentation complete (user guide, API docs, governance)

---

## v1.1 Readiness Criteria

**Before starting v1.1 development**:
1. v1.0 released and stable (no critical bugs)
2. User feedback collected (surveys, GitHub issues, interviews)
3. v1.1 features prioritized based on feedback
4. Ceremonies complete for v1.1 stories (Three Amigos, Example Mapping)

**v1.1 Timeline**: 4-6 weeks after v1.0 release

---

## Feature Comparison Matrix

| Feature | MDSlides v1.0 | MDSlides v1.1 | Marp | PowerPoint |
|---------|---------------|---------------|------|------------|
| **Core Pipeline** |
| Markdown parsing | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| Structure validation | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| Density validation | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| Standalone HTML | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| Keyboard navigation | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Templates** |
| Title slide | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| Content slide | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| Code slide | ❌ v1.1 | ✅ Yes | ✅ Yes | ✅ Yes |
| Image slide | ❌ v1.1 | ✅ Yes | ✅ Yes | ✅ Yes |
| Custom templates | ❌ v1.2 | ❌ v1.2 | ✅ Yes | ✅ Yes |
| **Themes** |
| JSON themes | ✅ Yes | ✅ Yes | ✅ CSS | ✅ Yes |
| Theme validation | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| WCAG AA enforcement | ✅ Yes | ✅ Yes | ❌ No | ⚠️ Opt-in |
| **Advanced Features** |
| Speaker notes parsing | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| Speaker notes rendering | ❌ v1.1 | ✅ Yes | ✅ Yes | ✅ Yes |
| PDF export | ❌ v1.1 | ✅ Yes | ✅ Yes | ✅ Yes |
| Watch mode | ❌ v1.1 | ✅ Yes | ✅ Yes | ❌ No |
| Slide transitions | ❌ v1.2 | ❌ v1.2 | ✅ Yes | ✅ Yes |
| Mobile app | ❌ v2.0 | ❌ v2.0 | ❌ No | ✅ Yes |

**v1.0 Unique Value**:
- Only tool with density validation (Marp doesn't validate)
- Only tool with theme validation (WCAG AA enforcement)
- Only tool with structure + content validation

---

## Marketing Message

### v1.0 Launch Message

> **MDSlides v1.0: Markdown Slides with Validation**
>
> Stop creating unreadable slides. MDSlides validates density (12 lines, 150 words) and accessibility (WCAG AA) before rendering.
>
> **What's Included**:
> - Markdown → HTML slide generation
> - Density validation (prevent "wall of text" slides)
> - Theme system with WCAG AA enforcement
> - Standalone HTML (offline-capable)
> - CLI tool (`mdslides slides.md output.html`)
>
> **Coming in v1.1** (4-6 weeks):
> - Code slides with syntax highlighting
> - Image slides with alt text validation
> - Speaker view (timer, next slide preview)
> - PDF export
> - Watch mode (auto-regenerate)

### Target Users (v1.0)

**Primary**:
- Corporate presenters (need WCAG compliance)
- Conference speakers (need readable slides)
- Technical writers (Markdown-native workflow)

**Secondary** (wait for v1.1):
- Developers (need code slides - coming in v1.1)
- Educators (need speaker notes rendering - coming in v1.1)
- Consultants (need PDF export - coming in v1.1)

---

## Related Ceremonies

- **Backlog Bucketing Session**: Split 50+ stories into v1.0 (12), v1.1 (5), v1.2 (4), v2.0 (4)
- **All Phase 2 Ceremonies**: 12 v1.0 stories have completed Three Amigos + Example Mapping

---

## Related Governance

- **ADR-001**: Technology Stack Selection (Scala 3.3.1, Scalatags, Flexmark, Decline)
- **ADR-002**: Validation Pipeline Architecture (Structure → Density + Content)
- **PDR-001**: Density Validation Limits (12 lines, 150 words, 80 chars)
- **PDR-002**: Speaker Notes v1.0 Scope (parse-only, rendering deferred)

---

**Decision Owner**: Tony Moores (Product Owner)
**Business Impact**: Critical (defines launch timeline, user expectations)
**User Impact**: High (sets feature availability, roadmap visibility)
**Timeline Impact**: High (6-8 weeks for v1.0 vs. 3-4 months for larger scope)
**Reversibility**: Low (once launched, can't "un-launch" features, but can add)
