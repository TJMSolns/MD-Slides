# MD-Slides feature list

Every user-facing feature, where the feature tour (`examples/feature-tour.md`) demonstrates it, where the docs
describe it, and how it is verified. The feature tour is the tutorial, the acceptance test and the live demo
(https://www.tjm.solutions/mdslides/demo/).

**Rule:** a feature change is not done until its row here, its tour slide and its docs page are updated in the same
change. A release is not done until every row has been verified against the released artifacts.

Docs pages are under https://www.tjm.solutions/mdslides/docs/. "Browser check" is the automated run of
`scripts/verify-mdslides-demo.mjs` in the tjm-solutions-site repo against the rendered demo; "Phone" is a manual
pass through the tour's **Try it** steps on a real phone.

## Writing slides

| Feature | Tour slide | Docs | Verified by |
|---|---|---|---|
| Templates: title, content, section-title, two-column, diagram, closing | Templates section | writing-slides | Unit (`TemplateV2Spec`, `MarkdownParserSpec`); render of tour |
| Inline formatting, lists, tables (bold, code and links inside cells) | Inline formatting; Lists; Tables | writing-slides | Unit (`FormattedContentSpec`, `HTMLRendererTableSpec`, `FlexmarkAdapterTableSpec`); render of tour |
| Code blocks with syntax highlighting | Code blocks: syntax highlighting; Python and Bash | writing-slides | Unit; browser check |
| Long code listings shrink (over 20 lines) | Long code listings | writing-slides | Browser check; phone |
| Images (copied) and data URLs; alt text required | Images | writing-slides, validation | Unit (`MarkdownParserImageAltSpec`, `HTMLRendererImageSpec`, `ImagePathRoundTripSpec`: every documented path form resolves in the output); release run of a deck with `./images/`, `images/` and bare file names |
| Mermaid diagrams (pre-rendered with `mmdc`), captions | diagram template; Mermaid diagram: live render | writing-slides | Unit (`MarkdownParserCaptionSpec`, `MermaidDiagramSpec`); browser check |
| Speaker notes | Speaker notes | speaker-view | Unit |
| Per-slide background | Per-slide background image | writing-slides | Render of tour |
| Header/footer tokens: pageNumber, totalPages, timer, title, date | Per-slide header and footer: live demo | writing-slides | Unit (`HTMLRendererSessionSpec`); browser check |
| Vertical alignment | Vertical alignment | writing-slides | Unit (`VerticalAlignmentSpec`) |

## Presenting

| Feature | Tour slide | Docs | Verified by |
|---|---|---|---|
| Keyboard navigation and clickers (arrows, Space, Enter, Page Up/Down, Home/End) | Keyboard shortcuts | navigation | Unit (`HTMLRendererMobileSpec`); browser check |
| Touch: swipe, tap zones, on-screen buttons | Touch, clickers and fullscreen | navigation | Unit; phone |
| Fullscreen (F, button) | Touch, clickers and fullscreen; Fullscreen and browser tips | navigation | Unit; phone |
| Fit to the screen (wide tables, code, diagrams, whole slide) | Fit to the screen: a deliberately wide slide | navigation | Browser check (phone and laptop sizes); phone |
| Deep links `#N` | Links to a slide | navigation | Browser check |
| Goto (G, type a number, list, tap the counter) | Break mode, goto and history | navigation | Browser check; phone |
| History (P / N) | Break mode, goto and history | navigation | Unit (`NavigationHistorySpec`); browser check |
| Break mode (B) and break-screen image | Break mode, goto and history | navigation, configuration | Unit (`BreakScreenAssetSpec`, `BreakModeSpec`); browser check |
| Timer (T, R) | Speaker view: layout, sync and timer | navigation | Unit (`timer/`); browser check |
| Speaker view, synced both ways | Speaker view: layout, sync and timer | speaker-view | Browser check |
| Session log (L) and `report` | Session log: how did the talk go? | distribution | Unit (`SessionAnalysisSpec`, `SessionLogParserSpec`, `SessionReportSpec`); browser check + `report` |
| Print / Save as PDF (one page per slide) | Distributing your presentation | distribution | Browser check (PDF page count) |

## Command line

| Feature | Tour slide | Docs | Verified by |
|---|---|---|---|
| `render` (simple and explicit forms, all flags) | render: markdown to HTML; render: all flags | intro, installation | Unit (`CLIArgumentsSpec`, `PathResolverSpec`); release run |
| `display`, smart default | Smart default: no subcommand needed | distribution | Release run |
| `config` | config command | configuration | Release run |
| `--help`, `--version` | render: all flags | installation | Release run |
| MCP server (`render_deck`, `validate_deck`, `list_themes`, `get_deck_info`) | MCP server: decks from an AI assistant | README | Unit (`McpServerSpec`) |

## Themes and configuration

| Feature | Tour slide | Docs | Verified by |
|---|---|---|---|
| Built-in themes light, dark, corporate (WCAG 2.1 AA) | Built-in themes | themes | Unit (`BuiltInThemeContrastSpec`); render of tour in each |
| Theme folders (`themes/NAME/theme.json`, `paths.themesDir`) | Custom themes: a theme folder | themes | Unit (`ThemeLoaderSpec`); release run |
| Theme background colour/image, `templateBackgrounds` | Theme backgrounds: colour and image | themes | Release run |
| `templateConfig` (header, footer, verticalAlign, colors per template) | Per-template configuration in themes | themes | Unit (`TemplateConfigurationSpec`); release run |
| Project and global config, priority order | Four-layer configuration | configuration | Unit (`ConfigLoaderSpec`, `ConfigurationSpec`) |

## Validation

| Feature | Tour slide | Docs | Verified by |
|---|---|---|---|
| Errors stop the render (structure, alt text, missing images), all reported in one pass before anything is written | What gets validated; Validation output | validation | Unit (`DeckCheckSpec`, `TwoColumnSlideSpec`); release run of a deck with problems on several slides |
| Warnings (density, theme contrast) | What gets validated; Validation output | validation | Unit (`SlideSpec`, density properties) |
| Accessibility report (`--accessibility-report`) | render: all flags | validation | Release run |

## Known gaps

- `--watch` (live reload) is parsed but not implemented; it says so when used.
