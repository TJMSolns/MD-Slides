Feature: Header and Footer Enhancements
  As a presentation author
  I want customizable headers and footers on my slides
  So that I can display branding, timer, and page numbers consistently across slides

  Background:
    Given a markdown presentation "my-talk.md" with 42 slides

  # Feature 1: Footer - Multiple Elements (Up to 3 Positions)
  Scenario: Footer with timer, custom text, and page numbers
    Given the template defines footer configuration:
      """
      footer:
        bottom-left:
          content: "{{timer}}"
        bottom-center:
          content: "Internal Only - Do not Distribute"
        bottom-right:
          content: "{{pageNumber}} / {{totalPages}}"
      """
    When I render the presentation
    Then slide 15 footer contains 3 elements:
      | position      | content                             |
      | bottom-left   | <div class="presentation-timer">00:00:00</div> |
      | bottom-center | Internal Only - Do not Distribute   |
      | bottom-right  | 15 / 42                             |

  Scenario: Footer with only timer (single element)
    Given the template defines footer configuration:
      """
      footer:
        bottom-left:
          content: "{{timer}}"
      """
    When I render the presentation
    Then slide 10 footer contains 1 element at bottom-left
    And the footer shows the presentation timer
    And bottom-center and bottom-right are empty

  Scenario: Footer with only page numbers (single element)
    Given the template defines footer configuration:
      """
      footer:
        bottom-right:
          content: "Slide {{pageNumber}} of {{totalPages}}"
      """
    When I render the presentation
    Then slide 20 footer contains "Slide 20 of 42" at bottom-right

  # Feature 2: Placeholder Resolution
  Scenario Outline: Page number placeholder resolution
    Given a presentation with 42 slides
    And the footer content is "<content_template>"
    When I render slide <slide_index>
    Then the footer displays "<resolved_content>"

    Examples:
      | slide_index | content_template                          | resolved_content       |
      | 0           | Slide {{pageNumber}} of {{totalPages}}    | Slide 1 of 42          |
      | 14          | Slide {{pageNumber}} of {{totalPages}}    | Slide 15 of 42         |
      | 41          | Slide {{pageNumber}} of {{totalPages}}    | Slide 42 of 42         |
      | 10          | {{pageNumber}} / {{totalPages}}           | 11 / 42                |
      | 5           | Page {{pageNumber}}                       | Page 6                 |

  Scenario: Timer placeholder inserts dynamic div
    Given the footer content is "{{timer}}"
    When I render slide 10
    Then the footer HTML contains:
      """
      <div class="presentation-timer">00:00:00</div>
      """
    And the timer div has id "presentation-timer"
    And the timer is updated by JavaScript every second

  Scenario: Title placeholder resolution
    Given the presentation frontmatter contains:
      """
      title: MDSlides Tutorial
      """
    And the footer content is "{{title}} - {{pageNumber}}"
    When I render slide 5
    Then the footer displays "MDSlides Tutorial - 6"

  Scenario: Multiple placeholders in one footer element
    Given the footer content is "{{title}} | Slide {{pageNumber}} of {{totalPages}} | {{timer}}"
    When I render slide 10
    Then the footer displays:
      """
      MDSlides Tutorial | Slide 11 of 42 | <div class="presentation-timer">00:00:00</div>
      """

  # Feature 3: Template-Specific Defaults
  Scenario: Title template has default header/footer
    Given slide 0 uses template "title"
    And the title template defines:
      """
      header:
        position: top-right
        content: "{{title}}"
      footer:
        position: bottom-right
        content: "Slide {{pageNumber}} of {{totalPages}}"
      """
    When I render slide 0
    Then the header at top-right shows "MDSlides Tutorial"
    And the footer at bottom-right shows "Slide 1 of 42"

  Scenario: Content template has timer in footer
    Given slide 5 uses template "content"
    And the content template defines:
      """
      footer:
        bottom-left:
          content: "{{timer}}"
        bottom-right:
          content: "{{pageNumber}}"
      """
    When I render slide 5
    Then the footer at bottom-left shows the timer
    And the footer at bottom-right shows "6"

  Scenario: Section-title template has centered footer
    Given slide 10 uses template "section-title"
    And the section-title template defines:
      """
      header:
        position: top-center
        content: "{{title}}"
      footer:
        position: bottom-center
        content: "{{pageNumber}} / {{totalPages}}"
      """
    When I render slide 10
    Then the header at top-center shows "MDSlides Tutorial"
    And the footer at bottom-center shows "11 / 42"

  # Feature 4: Custom CSS Classes
  Scenario: Footer element with custom CSS class
    Given the footer configuration is:
      """
      footer:
        bottom-left:
          content: "{{timer}}"
          cssClass: "custom-timer-style"
      """
    When I render slide 10
    Then the footer HTML contains:
      """
      <div class="footer-bottom-left custom-timer-style">
        <div class="presentation-timer">00:00:00</div>
      </div>
      """

  Scenario: Multiple footer elements with different CSS classes
    Given the footer configuration is:
      """
      footer:
        bottom-left:
          content: "{{timer}}"
          cssClass: "timer-style"
        bottom-center:
          content: "Confidential"
          cssClass: "watermark-style"
        bottom-right:
          content: "{{pageNumber}}"
          cssClass: "page-num-style"
      """
    When I render slide 10
    Then the bottom-left footer has classes "footer-bottom-left timer-style"
    And the bottom-center footer has classes "footer-bottom-center watermark-style"
    And the bottom-right footer has classes "footer-bottom-right page-num-style"

  # Feature 5: Header Positioning
  Scenario Outline: Header positions
    Given the header configuration is:
      """
      header:
        position: <position>
        content: "Header Text"
      """
    When I render slide 10
    Then the header is positioned at <position>
    And the header contains "Header Text"

    Examples:
      | position    |
      | top-left    |
      | top-center  |
      | top-right   |

  # Feature 6: Overlay Positioning (Fixed, Not Content Flow)
  Scenario: Footer overlays slide content
    Given the footer configuration defines bottom-left and bottom-right elements
    When I render slide 10
    Then the footer has CSS:
      """
      position: fixed;
      bottom: 0;
      z-index: 100;
      """
    And the slide content has bottom margin to avoid overlap

  Scenario: Header overlays slide content
    Given the header configuration defines top-center
    When I render slide 10
    Then the header has CSS:
      """
      position: fixed;
      top: 0;
      z-index: 100;
      """
    And the slide content has top margin to avoid overlap

  # Feature 7: Static vs. Dynamic Placeholders
  Scenario: Static placeholders resolved once at render time
    Given the footer content is "{{pageNumber}} / {{totalPages}} | {{title}}"
    When I render slide 10
    Then the footer is resolved to "11 / 42 | MDSlides Tutorial"
    And the resolved content is static HTML (not updated by JavaScript)

  Scenario: Timer placeholder is dynamic
    Given the footer content is "{{timer}}"
    When I render slide 10
    Then the footer contains a div with class "presentation-timer"
    And JavaScript updates the timer content every 1 second
    And the timer div content changes from "00:00:00" to "00:00:01" to "00:00:02"

  # Feature 8: Page Number 1-Indexed for Display
  Scenario: Page numbers are 1-indexed in footer
    Given I am viewing slide 0 (0-indexed internally)
    And the footer content is "{{pageNumber}}"
    Then the footer displays "1" (not "0")

    When I navigate to slide 41 (0-indexed)
    Then the footer displays "42"

  # Feature 9: Empty Footer Elements
  Scenario: Footer position with empty content is not rendered
    Given the footer configuration is:
      """
      footer:
        bottom-left:
          content: "{{timer}}"
        bottom-center:
          content: ""
        bottom-right:
          content: "{{pageNumber}}"
      """
    When I render slide 10
    Then the footer contains bottom-left and bottom-right elements
    But bottom-center is not rendered (no empty div)

  # Feature 10: Header/Footer Per-Slide Override (Frontmatter)
  Scenario: Slide-level frontmatter overrides template footer
    Given slide 5 uses template "content"
    And the template defines footer "{{timer}}"
    But slide 5 frontmatter contains:
      """
      footer-text: "Special Notice for This Slide"
      """
    When I render slide 5
    Then the footer displays "Special Notice for This Slide"
    And the template default is overridden

  Scenario: Slide-level frontmatter hides header
    Given slide 10 uses template "title"
    And the template defines a header
    But slide 10 frontmatter contains:
      """
      hideHeader: true
      """
    When I render slide 10
    Then no header is rendered
    And the slide content uses the full vertical space

  Scenario: Slide-level frontmatter hides footer
    Given slide 15 uses template "content"
    But slide 15 frontmatter contains:
      """
      hideFooter: true
      """
    When I render slide 15
    Then no footer is rendered

  # Feature 11: Accessibility
  Scenario: Footer has semantic HTML
    When I render a slide with footer
    Then the footer uses `<footer>` element
    And the footer has `role="contentinfo"` for screen readers

  Scenario: Timer div is not announced by screen readers
    Given the footer contains "{{timer}}"
    When I render slide 10
    Then the timer div has `aria-live="off"`
    And screen readers do not announce timer updates

  Scenario: Footer elements have sufficient contrast
    Given the theme is "dark"
    When I render slide 10 with footer
    Then the footer text color has >= 4.5:1 contrast ratio with background

  # Feature 12: Theme-Specific Styling
  Scenario: Light theme footer styling
    Given the theme is "light"
    When I render slide 10
    Then the footer has:
      """
      background: rgba(0, 0, 0, 0.05);
      border-top: 1px solid rgba(0, 0, 0, 0.1);
      color: #000000;
      """

  Scenario: Dark theme footer styling
    Given the theme is "dark"
    When I render slide 10
    Then the footer has:
      """
      background: rgba(255, 255, 255, 0.05);
      border-top: 1px solid rgba(255, 255, 255, 0.1);
      color: #FFFFFF;
      """

  # Feature 13: Timer Synchronization with Presentation Timer
  Scenario: Footer timer updates match PresentationTimer
    Given the footer contains "{{timer}}"
    And the PresentationTimer is running
    When the PresentationTimer reaches "00:15:30"
    Then the footer timer div displays "00:15:30"
    And the footer updates every second in sync with the timer

  Scenario: Footer timer pauses when PresentationTimer pauses
    Given the footer contains "{{timer}}"
    And the PresentationTimer is paused at "00:10:00"
    When I activate break mode
    Then the footer timer remains at "00:10:00"
    And the footer does not update while paused

  # Feature 14: Grid Layout for Footer
  Scenario: Footer uses CSS Grid for 3-column layout
    Given the footer has bottom-left, bottom-center, and bottom-right elements
    When I render slide 10
    Then the footer has CSS:
      """
      display: grid;
      grid-template-columns: 1fr 2fr 1fr;
      """
    And the center column is wider for longer text

  # Feature 15: Default Footer (No Configuration)
  Scenario: No footer configuration uses built-in default
    Given the template does not define header or footer
    When I render slide 10
    Then the footer shows page numbers at bottom-right: "11 / 42"
    And no header is rendered
    And this is the built-in default

  Scenario: Empty footer configuration hides footer
    Given the template defines:
      """
      footer: null
      """
    When I render slide 10
    Then no footer is rendered

  # Feature 16: Complex Footer Content
  Scenario: Footer with HTML entities
    Given the footer content is "© 2025 TJM Solutions"
    When I render slide 10
    Then the footer correctly displays the copyright symbol

  Scenario: Footer with special characters
    Given the footer content is "Q&A | Contact: info@example.com"
    When I render slide 10
    Then the footer displays "Q&A | Contact: info@example.com"
    And special characters are HTML-escaped

  # Feature 17: Responsive Considerations
  Scenario: Footer wraps text if too long
    Given the footer bottom-center content is a very long string (100 characters)
    When I render slide 10
    Then the footer center column wraps the text
    Or truncates with ellipsis to fit

  # Feature 18: Edge Cases
  Scenario: Footer on first slide
    When I render slide 0
    Then the footer displays "1 / 42" for page numbers
    And the timer shows "00:00:00"

  Scenario: Footer on last slide
    When I render slide 41
    Then the footer displays "42 / 42" for page numbers

  Scenario: Presentation with only 1 slide
    Given a presentation with 1 slide
    When I render slide 0
    Then the footer displays "1 / 1"

  # Feature 19: CLI Argument Override
  Scenario: CLI --footer-text overrides template default
    Given the template defines footer "{{timer}}"
    When I render with "--footer-text 'Confidential - Q4 2025'"
    Then all slides show "Confidential - Q4 2025" in the footer
    And the template default is overridden globally

  Scenario: CLI --header-position changes header placement
    Given the template defines header at top-center
    When I render with "--header-position top-right"
    Then all slides show the header at top-right

  # Feature 20: Invariant Validation
  Scenario: Page number never exceeds total slides
    When I render any slide
    Then {{pageNumber}} is always <= {{totalPages}}
    And page numbers are always positive integers

  Scenario: Timer placeholder always inserts valid HTML
    When I render a slide with {{timer}} placeholder
    Then the resulting HTML is valid
    And the timer div has all required attributes
