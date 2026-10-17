Feature: Goto Function Popup
  As a presentation author
  I want to use a goto popup to jump to any slide by number
  So that I can quickly navigate to specific slides during Q&A or non-linear presentations

  Background:
    Given a rendered presentation with 42 slides
    And the presentation is displayed in a browser
    And I am on slide 10

  # Feature 1: Goto Popup Opening
  Scenario: Open goto popup with G key
    When I press the "G" key
    Then the Goto popup is displayed
    And the popup overlays the current slide
    And the input field is focused and empty
    And the PresentationTimer is paused

  Scenario: Goto popup cannot open during break mode
    Given BreakMode is active
    When I press the "G" key
    Then the Goto popup does NOT open
    And a message is displayed: "Goto is disabled during break mode"
    And BreakMode remains active
    And the PresentationTimer remains paused

  # Feature 2: Input Validation
  Scenario: Valid slide number (in range)
    Given the Goto popup is open
    When I type "25"
    Then the input is validated in real-time
    And no error message is shown
    And the target slide index is resolved to 24 (0-indexed)

  Scenario: Invalid input - number out of range (too high)
    Given the Goto popup is open
    When I type "50"
    Then an error message is displayed: "Slide number must be between 1 and 42"
    And the input field is highlighted as invalid
    And the target slide index is None

  Scenario: Invalid input - number out of range (zero)
    Given the Goto popup is open
    When I type "0"
    Then an error message is displayed: "Slide number must be between 1 and 42"
    And the input field is highlighted as invalid

  Scenario: Invalid input - negative number
    Given the Goto popup is open
    When I type "-5"
    Then an error message is displayed: "Slide number must be a positive integer"
    And the input field is highlighted as invalid

  Scenario: Invalid input - non-numeric characters
    Given the Goto popup is open
    When I type "abc"
    Then an error message is displayed: "Slide number must be a positive integer"
    And the input field is highlighted as invalid

  Scenario: Invalid input - decimal number
    Given the Goto popup is open
    When I type "15.5"
    Then an error message is displayed: "Slide number must be a positive integer"
    And the input field is highlighted as invalid

  Scenario: Invalid input - empty string
    Given the Goto popup is open
    And the input field is empty
    When I attempt to confirm navigation
    Then an error message is displayed: "Please enter a slide number"
    And navigation does not occur

  # Feature 3: Popup Confirmation (ENTER key)
  Scenario: Confirm valid input with ENTER key
    Given the Goto popup is open
    And I type "25"
    When I press the ENTER key
    Then I navigate to slide 24 (0-indexed from input "25")
    And the Goto popup closes
    And the PresentationTimer resumes
    And the NavigationHistory is updated with slide 10 added to visit stack
    And the forward stack is cleared

  Scenario: Cannot confirm invalid input
    Given the Goto popup is open
    And I type "99" (invalid, out of range)
    And an error message is shown
    When I press the ENTER key
    Then navigation does NOT occur
    And I remain on slide 10
    And the Goto popup remains open
    And the error message persists

  # Feature 4: Popup Cancellation (ESC key)
  Scenario: Cancel goto with ESC key
    Given the Goto popup is open
    And I have typed "25" in the input field
    When I press the ESC key
    Then the Goto popup closes
    And I remain on slide 10 (no navigation)
    And the PresentationTimer resumes
    And the NavigationHistory is unchanged

  Scenario: Cancel goto with empty input
    Given the Goto popup is open
    And the input field is empty
    When I press the ESC key
    Then the Goto popup closes
    And I remain on slide 10
    And the PresentationTimer resumes

  # Feature 5: Timer Pause/Resume
  Scenario: Timer pauses when goto popup opens
    Given the PresentationTimer is running at "00:10:30"
    When I press "G" to open the Goto popup
    Then the PresentationTimer state is Paused
    And the timer display shows "00:10:30" (frozen)
    And the timer does not increment while the popup is open

  Scenario: Timer resumes when goto popup closes (confirm)
    Given the Goto popup is open
    And the PresentationTimer is paused at "00:10:30"
    When I type "25" and press ENTER
    Then the Goto popup closes
    And the PresentationTimer state is Running
    And the timer resumes from "00:10:30"

  Scenario: Timer resumes when goto popup closes (cancel)
    Given the Goto popup is open
    And the PresentationTimer is paused at "00:10:30"
    When I press ESC
    Then the Goto popup closes
    And the PresentationTimer state is Running
    And the timer resumes from "00:10:30"

  # Feature 6: 1-Indexed UI to 0-Indexed Internal Conversion
  Scenario Outline: Slide number conversion from UI to internal index
    Given the Goto popup is open
    When I type "<ui_input>"
    Then the target slide index is resolved to <internal_index>

    Examples:
      | ui_input | internal_index |
      | 1        | 0              |
      | 2        | 1              |
      | 10       | 9              |
      | 25       | 24             |
      | 42       | 41             |

  Scenario: First slide navigation (input "1")
    Given the Goto popup is open
    And I type "1"
    When I press ENTER
    Then I navigate to slide 0 (first slide)
    And the Goto popup closes

  Scenario: Last slide navigation (input "42")
    Given the Goto popup is open
    And I type "42"
    When I press ENTER
    Then I navigate to slide 41 (last slide)
    And the Goto popup closes

  # Feature 7: Real-Time Validation Feedback
  Scenario: Validation clears when correcting invalid input
    Given the Goto popup is open
    And I have typed "99" (invalid)
    And an error message is displayed
    When I clear the input and type "25"
    Then the error message disappears
    And the input field is no longer highlighted as invalid

  Scenario: Validation updates as user types
    Given the Goto popup is open
    When I type "9"
    Then no error is shown (valid so far)
    When I type "9" (now "99")
    Then an error message is displayed: "Slide number must be between 1 and 42"
    When I press backspace (now "9" again)
    Then the error message disappears

  # Feature 8: Navigation History Integration
  Scenario: Goto adds to visit stack and clears forward stack
    Given I have navigation history: visits [0, 5, 12], forward stack [20]
    And I am on slide 12
    When I press "G", type "30", and press ENTER
    Then I navigate to slide 29 (0-indexed)
    And the visit stack is updated to [0, 5, 12]
    And the forward stack is cleared (was [20], now empty)
    And the current slide is 29

  Scenario: Goto navigation from history can be undone with P key
    Given I am on slide 10
    When I press "G", type "30", and press ENTER
    And I navigate to slide 29
    And I press "P" (previous)
    Then I navigate back to slide 10
    And the forward stack contains [29]

  # Feature 9: Cross-Window Synchronization
  Scenario: Goto navigation syncs to speaker view
    Given the speaker view is open
    And I am on slide 10
    When I press "G", type "25", and press ENTER
    Then the main presentation navigates to slide 24
    And the speaker view immediately updates to show slide 24
    And both windows display the same slide

  Scenario: Goto popup pause syncs timer in speaker view
    Given the speaker view is open
    When I press "G" to open the Goto popup
    Then the PresentationTimer is paused in both windows
    And both timers show the same paused time

  # Feature 10: Session Logging Integration
  Scenario: Goto navigation is logged with NavigationMethod.Goto
    Given session logging is enabled
    When I press "G" at timestamp "2025-12-29T14:35:40Z"
    And I type "25" and press ENTER
    Then the session log records a slide visit to slide 24
    And the navigation method is "Goto"
    And the entry timestamp is "2025-12-29T14:35:40Z"
    And the session log contains an event:
      """
      {
        "timestamp": "2025-12-29T14:35:40Z",
        "key": "G",
        "action": "goto_slide_24"
      }
      """

  Scenario: Goto popup cancellation is not logged
    Given session logging is enabled
    When I press "G" and then press ESC (cancel)
    Then no navigation event is logged
    And no "G" key event is logged (popup was cancelled)

  # Feature 11: Edge Cases
  Scenario: Goto to the current slide
    Given I am on slide 10
    When I press "G", type "11" (0-indexed = 10), and press ENTER
    Then I remain on slide 10
    And the Goto popup closes
    And the navigation history is updated (self-visit)
    And the PresentationTimer resumes

  Scenario: Rapid goto invocations
    Given the Goto popup is closed
    When I press "G" to open
    And I press "G" again
    Then the Goto popup remains open (no action on second "G")
    And the input field remains focused

  Scenario: Goto popup input persistence across open/close
    Given I press "G" to open the Goto popup
    And I type "25"
    When I press ESC to close
    And I press "G" to reopen the Goto popup
    Then the input field is empty (not "25")
    And the popup state is reset

  # Feature 12: Accessibility
  Scenario: Goto popup keyboard accessibility
    Given the Goto popup is open
    Then the input field has focus automatically
    And I can type numbers without clicking
    And I can press TAB to move focus to confirm button
    And I can press ENTER to confirm from any focused element

  Scenario: Goto popup screen reader support
    Given the Goto popup is open
    Then the popup has `role="dialog"` for screen readers
    And the input field has `aria-label="Enter slide number"`
    And error messages have `aria-live="assertive"` for immediate announcement

  # Feature 13: Main View Only (Not Speaker View)
  Scenario: Goto popup only available in main presentation view
    Given the speaker view is open
    When I press "G" in the speaker view
    Then the Goto popup does NOT open
    And a message is displayed: "Goto is only available in the main presentation view"
    And I can still see the current slide and notes

  Scenario: Goto popup in main view syncs navigation to speaker view
    Given the speaker view is open
    And I am in the main presentation view
    When I press "G", type "30", and press ENTER
    Then the main view navigates to slide 29
    And the speaker view syncs to show slide 29
    And the speaker view did not show the Goto popup

  # Feature 14: Boundary Conditions
  Scenario: Goto at first slide
    Given I am on slide 0 (first slide)
    When I press "G", type "1", and press ENTER
    Then I remain on slide 0
    And the Goto popup closes

  Scenario: Goto at last slide
    Given I am on slide 41 (last slide)
    When I press "G", type "42", and press ENTER
    Then I remain on slide 41
    And the Goto popup closes

  Scenario: Goto with very long input
    Given the Goto popup is open
    When I type "12345678901234567890"
    Then an error message is displayed: "Slide number must be between 1 and 42"
    And the input is truncated or rejected

  # Feature 15: Break Mode Integration (Extended)
  Scenario: Activate break mode while goto popup is open
    Given the Goto popup is open
    When I press "B" to activate break mode
    Then the Goto popup closes
    And BreakMode is activated
    And the PresentationTimer is paused (by break mode)
    And I remain on slide 10

  Scenario: Goto popup cannot open during break mode (enforcement)
    Given BreakMode is active
    When I press "G"
    Then the Goto popup does NOT open
    And the break screen remains displayed
    And no navigation occurs

  # Feature 16: Input Field Constraints
  Scenario: Input field only accepts numeric characters
    Given the Goto popup is open
    When I type "1a2b3c"
    Then the input field contains "123" (letters stripped)
    Or an error is shown for non-numeric input

  Scenario: Input field limits length
    Given the Goto popup is open
    When I type "123456789"
    Then the input field accepts only the first few digits (e.g., "1234")
    Or validation flags as out of range

  # Feature 17: Invariant Validation
  Scenario: Goto navigation always results in valid slide index
    Given the Goto popup is open
    When I type any valid number between 1 and 42
    And I press ENTER
    Then the resulting slide index is always >= 0
    And the resulting slide index is always < 42
    And I successfully navigate to the target slide

  Scenario: Timer state consistency
    Given the PresentationTimer is running
    When I open and close the Goto popup (ESC)
    Then the PresentationTimer returns to Running state
    And the elapsed time includes the goto popup pause duration
