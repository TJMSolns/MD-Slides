Feature: Two-Column Layout Template
  As a presentation author
  I want to create slides with two-column layouts
  So that I can display code alongside explanations, compare before/after states, or show diagrams side-by-side

  Background:
    Given I am authoring a presentation in markdown

  # Feature 1: Template Recognition
  Scenario: Two-column template recognized via frontmatter
    Given I write a slide with frontmatter:
      """
      ---
      template: two-column
      ---
      """
    When I parse the markdown
    Then the slide is recognized as template "two-column"

  # Feature 2: Column Delimiter Parsing
  Scenario: Valid two-column slide with column delimiter
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      ## Left Column

      This is the left column content.

      ---column---

      ## Right Column

      This is the right column content.
      """
    When I parse the markdown
    Then the left column contains "## Left Column\n\nThis is the left column content."
    And the right column contains "## Right Column\n\nThis is the right column content."

  Scenario: Exactly one column delimiter required
    Given I write a slide with TWO "---column---" delimiters
    When I parse the markdown
    Then a validation error is raised: "Two-column template allows only one '---column---' separator"

  Scenario: No column delimiter raises error
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      Content without column delimiter
      """
    When I parse the markdown
    Then a validation error is raised: "Two-column template requires exactly one '---column---' separator"

  # Feature 3: Non-Empty Columns Validation
  Scenario: Both columns must be non-empty
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      ## Left Column Content

      ---column---

      ## Right Column Content
      """
    When I parse the markdown
    Then both columns are non-empty
    And the slide is valid

  Scenario: Empty left column raises error
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      ---column---

      ## Right Column
      """
    When I parse the markdown
    Then a validation error is raised: "Both columns must contain content"
    And the error message specifies "Left column: 0 lines (EMPTY)"

  Scenario: Empty right column raises error
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      ## Left Column

      ---column---

      """
    When I parse the markdown
    Then a validation error is raised: "Both columns must contain content"
    And the error message specifies "Right column: 0 lines (EMPTY)"

  # Feature 4: Content Types Support
  Scenario Outline: Columns support all content types
    Given the left column contains <left_content_type>
    And the right column contains <right_content_type>
    When I render the slide
    Then both columns render correctly

    Examples:
      | left_content_type | right_content_type |
      | text paragraphs   | text paragraphs    |
      | code block        | text explanation   |
      | image             | text description   |
      | bulleted list     | numbered list      |
      | mermaid diagram   | text explanation   |
      | mixed content     | mixed content      |

  Scenario: Left column with code, right column with explanation
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      ## Implementation

      ```scala
      def renderSlide(slide: Slide): Html =
        div(
          cls := "slide",
          renderSlots(slide.slots)
        )
      ```

      ---column---

      ## Explanation

      This function renders a slide by:
      1. Creating a div container
      2. Adding the "slide" CSS class
      3. Rendering all slot content
      """
    When I render the slide
    Then the left column contains syntax-highlighted Scala code
    And the right column contains formatted explanation text
    And both columns are vertically centered

  Scenario: Left column with image, right column with description
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      ![Architecture diagram with 4 layers](./arch.png)

      ---column---

      ## System Architecture

      The diagram shows:
      - **Client Layer**: Web and mobile apps
      - **API Gateway**: Request routing
      - **Service Layer**: Business logic
      - **Data Layer**: PostgreSQL
      """
    When I render the slide
    Then the left column displays the image scaled to fit column width (max 50vw)
    And the right column displays the formatted description

  Scenario: Side-by-side mermaid diagrams
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      ## Development Flow

      ```mermaid
      graph TD
        A[Plan] --> B[Code]
        B --> C[Test]
        C --> D[Deploy]
      ```

      ---column---

      ## Deployment Pipeline

      ```mermaid
      graph TD
        A[Commit] --> B[CI]
        B --> C[Staging]
        C --> D[Production]
      ```
      """
    When I render the slide
    Then the left column contains a pre-rendered SVG mermaid diagram
    And the right column contains a pre-rendered SVG mermaid diagram
    And both diagrams are scaled to fit column width (max 50vw each)

  # Feature 5: Equal Width Split (50/50)
  Scenario: Columns have equal 50/50 width
    When I render a two-column slide
    Then the CSS Grid is configured as:
      """
      display: grid;
      grid-template-columns: 1fr 1fr;
      """
    And the left column occupies 50% of viewport width
    And the right column occupies 50% of viewport width

  # Feature 6: Column Gap
  Scenario: Fixed 2rem gap between columns
    When I render a two-column slide
    Then the CSS Grid has:
      """
      gap: 2rem;
      """
    And there is 32px spacing between left and right columns (at 16px base font)

  # Feature 7: Default Alignment
  Scenario: Horizontal alignment defaults to left
    When I render a two-column slide
    Then both columns have text-align: left
    And all content within columns is left-aligned

  Scenario: Vertical alignment defaults to center
    When I render a two-column slide
    Then both columns have:
      """
      display: flex;
      flex-direction: column;
      justify-content: center;
      """
    And column content is vertically centered within available space

  # Feature 8: Column Density Validation
  Scenario: Each column validated against 70vh height limit
    Given the left column contains 50 lines of text (estimated 75vh)
    When I validate the slide
    Then a density error is raised: "Left column content exceeds vertical limit"
    And the error shows "Estimated height: 75vh (max: 70vh)"

  Scenario: Right column exceeds height limit
    Given the right column contains a very large mermaid diagram (estimated 80vh)
    When I validate the slide
    Then a density error is raised: "Right column content exceeds vertical limit"
    And suggestions are provided:
      """
      - Split content across multiple slides
      - Reduce diagram size
      - Use smaller font or line spacing
      """

  Scenario: Both columns within height limit
    Given the left column content is estimated at 50vh
    And the right column content is estimated at 60vh
    When I validate the slide
    Then no density error is raised
    And the slide is valid

  # Feature 9: Accessibility (Reading Order)
  Scenario: Left column read before right column by screen readers
    When I render a two-column slide
    Then the HTML structure is:
      """
      <div class="two-column-layout" role="region" aria-label="Two-column slide">
        <section class="column column-left" aria-label="Left column">
          <!-- Left column content -->
        </section>
        <section class="column column-right" aria-label="Right column">
          <!-- Right column content -->
        </section>
      </div>
      """
    And the left `<section>` appears before the right `<section>` in DOM order
    And screen readers read left column completely before right column

  # Feature 10: Image Scaling
  Scenario: Images in columns scaled to fit column width
    Given the left column contains an image
    When I render the slide
    Then the image has CSS:
      """
      max-width: 100%;  /* Column width, which is 50vw */
      height: auto;
      """
    And the image does not exceed the column boundary

  Scenario: Large image in narrow column
    Given the left column contains a 1920px wide image
    When I render the slide with 1920px viewport
    Then the image is scaled to fit 960px column width (50% of viewport)

  # Feature 11: Mermaid Diagram Scaling
  Scenario: Mermaid diagrams scaled to column width
    Given the left column contains a mermaid diagram
    When I render the slide
    Then the mermaid SVG is pre-rendered
    And the SVG has max-width: 50vw (to fit column)
    And the SVG maintains aspect ratio

  # Feature 12: Mixed Content in Single Column
  Scenario: Column with heading, text, code, and list
    Given the left column contains:
      """
      ## Features

      MDSlides provides:

      ```scala
      val features = List("Markdown", "Themes", "Diagrams")
      ```

      - Easy authoring
      - Beautiful output
      - Accessible
      """
    When I render the slide
    Then all content types render within the left column
    And the content is vertically stacked in order

  # Feature 13: Before/After Comparison Use Case
  Scenario: Before/After comparison slide
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      ## Before

      Old approach with limitations:
      - Manual HTML
      - No validation
      - Inconsistent styling

      ---column---

      ## After

      New approach with benefits:
      - Markdown simplicity
      - WCAG validation
      - Theme system
      """
    When I render the slide
    Then the left column shows "Before" content
    And the right column shows "After" content
    And both columns are vertically centered and left-aligned

  # Feature 14: Code + Explanation Use Case
  Scenario: Code on left, explanation on right
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      ```scala
      case class PresentationTimer(
        state: TimerState,
        startTimestamp: Long,
        totalPausedDuration: Long
      ):
        def elapsedSeconds(): Long = ???
      ```

      ---column---

      ## Timer Domain Model

      - **state**: Current timer state
      - **startTimestamp**: Session start
      - **totalPausedDuration**: Break time
      - **elapsedSeconds()**: Query method
      """
    When I render the slide
    Then the left column shows highlighted Scala code
    And the right column shows formatted explanation

  # Feature 15: Responsive Considerations
  Scenario: Two-column layout in presentation mode
    When I render a two-column slide at 1920x1080 resolution
    Then both columns are displayed side-by-side
    And the layout uses CSS Grid

  Scenario: Two-column layout maintains structure in print mode
    When I print the presentation
    Then two-column slides maintain side-by-side layout
    And the page break does not split columns

  # Feature 16: Integration with Themes
  Scenario: Two-column layout uses theme colors
    Given the theme is "dark"
    When I render a two-column slide
    Then the left and right columns use the dark theme colors
    And text color contrasts with background (>= 4.5:1 ratio)

  # Feature 17: Integration with Headers/Footers
  Scenario: Two-column slide with footer
    Given I write a two-column slide
    And the template defines a footer with timer and page numbers
    When I render the slide
    Then the two-column layout occupies the main content area
    And the footer is displayed below the columns
    And the footer overlays the slide (fixed position)

  # Feature 18: Slide Transition Compatibility
  Scenario: Two-column slide with fade transition
    Given I write a two-column slide with transition "fade"
    When I navigate to this slide
    Then the slide fades in smoothly
    And both columns animate together

  # Feature 19: Empty Heading Scenarios
  Scenario: Columns without headings
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      Just plain text in the left column.

      ---column---

      And plain text in the right column too.
      """
    When I render the slide
    Then both columns render the plain text
    And no error occurs due to missing headings

  # Feature 20: Edge Cases
  Scenario: Minimal two-column slide
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      Left

      ---column---

      Right
      """
    When I render the slide
    Then the left column contains "Left"
    And the right column contains "Right"
    And both columns are valid (non-empty)

  Scenario: Very long content in one column
    Given the left column contains 500 words
    And the right column contains 50 words
    When I validate the slide
    Then the left column may trigger a density warning
    But the right column passes validation

  Scenario: Special characters in columns
    Given the left column contains "© 2025 TJM Solutions"
    And the right column contains "Q&A Session"
    When I render the slide
    Then special characters are correctly HTML-encoded

  # Feature 21: Parser Whitespace Handling
  Scenario: Whitespace around column delimiter is trimmed
    Given I write a slide:
      """
      ---
      template: two-column
      ---

      Left content


      ---column---


      Right content
      """
    When I parse the markdown
    Then the left column is trimmed to "Left content"
    And the right column is trimmed to "Right content"
    And extra blank lines are removed

  # Feature 22: Column Content Isolation
  Scenario: Left column parsing does not affect right column
    Given the left column contains a code block with backticks
    And the right column also contains a code block
    When I parse the markdown
    Then both code blocks are correctly parsed
    And the column delimiter does not interfere with code blocks

  Scenario: Markdown inside columns is independently parsed
    Given the left column contains a bulleted list
    And the right column contains a numbered list
    When I render the slide
    Then the left column shows bullets
    And the right column shows numbers
    And each column's markdown is correctly formatted

  # Feature 23: Validation Error Messages
  Scenario: Clear error for missing delimiter
    Given I write a two-column slide without "---column---"
    When I validate the slide
    Then the error message is:
      """
      ✗ Slide 5: Two-column template requires exactly one '---column---' separator

        To fix: Add '---column---' between left and right content
      """

  Scenario: Clear error for empty column
    Given the left column is empty
    When I validate the slide
    Then the error message is:
      """
      ✗ Slide 5: Two-column template requires both columns to have content
        Left column: 0 lines (EMPTY)
        Right column: 45 lines

        Fix: Add content to left column or change to single-column template
      """

  # Feature 24: Column Delimiter Case Sensitivity
  Scenario: Column delimiter is case-sensitive
    Given I use "---COLUMN---" (uppercase)
    When I parse the markdown
    Then it is NOT recognized as a column delimiter
    And a validation error is raised

  # Feature 25: Nested Content Support
  Scenario: Nested lists in columns
    Given the left column contains:
      """
      - Parent item
        - Child item
          - Grandchild item
      """
    When I render the slide
    Then the nested list is correctly indented
    And the hierarchy is preserved

  Scenario: Code block with nested syntax
    Given the left column contains:
      """
      ```scala
      case class Outer(
        inner: Inner,
        nested: List[Nested]
      )
      ```
      """
    When I render the slide
    Then the code block is syntax-highlighted correctly
    And nested structures are properly formatted
