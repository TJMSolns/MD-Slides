Feature: Template Configuration
  As a theme author
  I want to configure presentation defaults per template type
  So that slides automatically match my branded backgrounds and design system
  Without requiring content authors to specify presentation details in every slide

  Background:
    Given a theme with the following default colors:
      | element  | color   |
      | heading  | #002C74 |
      | body     | #333333 |
    And the theme has template backgrounds configured for:
      | template       | background                              |
      | title          | backgrounds/retisio-title-page.png      |
      | section-title  | backgrounds/retisio-section-title-page.png |
      | diagram        | backgrounds/retisio-diagram-page.png    |
      | closing        | backgrounds/retisio-end-page.png        |

  # ============================================================================
  # Rule 1: Configuration Hierarchy
  # ============================================================================

  Scenario: Theme template config overrides theme default
    Given the theme has default heading color "#002C74"
    And the theme has template configuration:
      | template | property       | value   |
      | title    | colors.heading | #FFFFFF |
    When a title slide is rendered
    Then the title slide heading color should be "#FFFFFF"
    And other template slides should have heading color "#002C74"

  Scenario: Global config overrides theme template config
    Given the theme has template configuration:
      | template | property | value                     |
      | title    | footer   | {{company}} Confidential  |
    And the global config has:
      | property | value         |
      | footer   | Public Domain |
    When a title slide is rendered
    Then the slide footer should display "Public Domain"

  Scenario: Slide frontmatter overrides everything
    Given the theme has template configuration:
      | template | property | value                     |
      | title    | footer   | {{company}} Confidential  |
    And the global config has:
      | property | value                            |
      | footer   | {{pageNumber}}/{{totalPages}}    |
    And a title slide with frontmatter:
      """
      ---
      footer: "Custom Footer"
      ---
      """
    When the slide is rendered
    Then the slide footer should display "Custom Footer"

  Scenario: Fallback chain uses theme template config when no overrides
    Given the theme has template configuration:
      | template | property       | value   |
      | diagram  | colors.heading | #FFFFFF |
    And there is no global config for heading color
    And a diagram slide with no frontmatter for heading color
    When the slide is rendered
    Then the slide heading color should be "#FFFFFF"

  # ============================================================================
  # Rule 2: Vertical Alignment
  # ============================================================================

  Scenario: Bottom-aligned title slide for branded theme
    Given the theme has template configuration:
      | template | property      | value  |
      | title    | verticalAlign | bottom |
    And the title template background has branded graphics at top
    When a title slide is rendered
    Then the slide content should be positioned at the bottom

  Scenario: Center-aligned content by default
    Given no template configuration for vertical alignment
    When a content slide is rendered
    Then the slide content should be centered vertically

  Scenario: Slide frontmatter overrides template alignment
    Given the theme has template configuration:
      | template | property      | value  |
      | title    | verticalAlign | bottom |
    And a title slide with frontmatter:
      """
      ---
      vertical-align: top
      ---
      """
    When the slide is rendered
    Then the slide content should be positioned at the top

  # ============================================================================
  # Rule 3: Layout Variants
  # ============================================================================

  Scenario: Section-title rendered as two-column
    Given the theme has template configuration:
      | template      | property | value      |
      | section-title | layout   | two-column |
    And the section-title template has column configuration:
      """
      {
        "leftColumn": { "width": "40%" },
        "rightColumn": { "width": "60%" }
      }
      """
    When a section-title slide is rendered
    Then the slide should use two-column layout
    And the left column should be 40% wide
    And the right column should be 60% wide

  Scenario: Default single-column layout when not specified
    Given no template configuration for layout
    When a content slide is rendered
    Then the slide should use single-column layout

  Scenario: Validation error for two-column without column config
    Given a theme with template configuration:
      | template      | property | value      |
      | section-title | layout   | two-column |
    But no column configuration is specified
    When the theme is validated
    Then validation should fail with error "Two-column layout requires columnConfig"

  # ============================================================================
  # Rule 4: Color Overrides
  # ============================================================================

  Scenario: White headings on diagram slides only
    Given the theme has default heading color "#002C74"
    And the theme has template configuration:
      | template | property       | value   |
      | diagram  | colors.heading | #FFFFFF |
    When a diagram slide is rendered
    Then the slide heading color should be "#FFFFFF"
    And content slides should have heading color "#002C74"

  Scenario: Multiple color overrides for title template
    Given the theme has template configuration:
      | template | property        | value   |
      | title    | colors.heading  | #FFFFFF |
      | title    | colors.subtitle | #CCCCCC |
      | title    | colors.author   | #FFFFFF |
    When a title slide is rendered
    Then the slide heading color should be "#FFFFFF"
    And the slide subtitle color should be "#CCCCCC"
    And the slide author color should be "#FFFFFF"

  Scenario: Partial color override inherits theme defaults
    Given the theme has default colors:
      | element | color   |
      | heading | #002C74 |
      | body    | #333333 |
    And the theme has template configuration:
      | template | property       | value   |
      | closing  | colors.heading | #FFFFFF |
    When a closing slide is rendered
    Then the slide heading color should be "#FFFFFF"
    And the slide body color should be "#333333"

  # ============================================================================
  # Rule 5: Header/Footer Templates
  # ============================================================================

  Scenario: Branded footer on title slides with placeholders
    Given the theme has template configuration:
      | template | property | value                             |
      | title    | footer   | {{company}} Confidential \| {{date}} |
    And the company is "Retisio"
    And the current date is "2026-01-02"
    When a title slide is rendered
    Then the slide footer should display "Retisio Confidential | 2026-01-02"

  Scenario: Page numbers on content slides
    Given the theme has template configuration:
      | template | property | value                         |
      | content  | footer   | {{pageNumber}}/{{totalPages}} |
    And the presentation has 10 slides
    When slide 5 is rendered
    Then the slide footer should display "5/10"

  Scenario: Timer in header
    Given the theme has template configuration:
      | template | property | value              |
      | diagram  | header   | Elapsed: {{timer}} |
    And the presentation has been running for 5 minutes
    When a diagram slide is rendered
    Then the slide header should display "Elapsed: 00:05:00"

  Scenario: Multi-element footer with positioned spans
    Given the theme has template configuration:
      | template | property | value                                                                                              |
      | content  | footer   | <span class='footer-left'>{{timer}}</span><span class='footer-right'>{{pageNumber}}/{{totalPages}}</span> |
    And the presentation has been running for 3 minutes
    And the presentation has 10 slides
    When slide 5 is rendered
    Then the footer should have timer "00:03:00" on the left
    And the footer should have page number "5/10" on the right

  Scenario: No header or footer when not configured
    Given no template configuration for header or footer
    And no global config for header or footer
    And a content slide with no frontmatter for header or footer
    When the slide is rendered
    Then the slide should have no header
    And the slide should have no footer

  # ============================================================================
  # Rule 6: Column Configuration
  # ============================================================================

  Scenario: Two-column layout with custom widths
    Given the theme has template configuration:
      | template      | property | value      |
      | section-title | layout   | two-column |
    And the section-title template has column configuration:
      """
      {
        "leftColumn": { "width": "40%" },
        "rightColumn": { "width": "60%" }
      }
      """
    When a section-title slide is rendered
    Then the left column should be 40% wide
    And the right column should be 60% wide

  Scenario: Two-column layout with per-column color overrides
    Given the theme has template configuration:
      | template      | property | value      |
      | section-title | layout   | two-column |
    And the section-title template has column configuration:
      """
      {
        "leftColumn": {
          "width": "40%",
          "colors": { "heading": "#FFFFFF" }
        },
        "rightColumn": {
          "width": "60%"
        }
      }
      """
    And the theme default heading color is "#002C74"
    When a section-title slide is rendered
    Then the left column heading color should be "#FFFFFF"
    And the right column heading color should be "#002C74"

  Scenario: Default column widths when not specified
    Given the theme has template configuration:
      | template      | property | value      |
      | section-title | layout   | two-column |
    And the section-title template has column configuration without widths
    When a section-title slide is rendered
    Then the left column should be 50% wide
    And the right column should be 50% wide

  # ============================================================================
  # Rule 7: Validation
  # ============================================================================

  Scenario: Invalid template name fails validation
    Given a theme with template configuration:
      | template        | property      | value  |
      | custom-template | verticalAlign | bottom |
    But the template "custom-template" does not exist
    When the theme is validated
    Then validation should fail with error "Unknown template: custom-template"

  Scenario: Invalid CSS color fails validation
    Given a theme with template configuration:
      | template | property       | value        |
      | title    | colors.heading | not-a-color  |
    When the theme is validated
    Then validation should fail with error "Invalid CSS color: not-a-color"

  Scenario: Invalid vertical alignment value fails validation
    Given a theme with template configuration:
      | template | property      | value  |
      | title    | verticalAlign | middle |
    When the theme is validated
    Then validation should fail with error "Invalid vertical alignment: middle. Valid values: top, center, bottom"

  Scenario: Column widths exceeding 100% generates warning
    Given a theme with template configuration:
      | template      | property | value      |
      | section-title | layout   | two-column |
    And the section-title template has column configuration:
      """
      {
        "leftColumn": { "width": "70%" },
        "rightColumn": { "width": "50%" }
      }
      """
    When the theme is validated
    Then validation should warn "Column widths exceed 100%"
    But validation should succeed
