Feature: Previous/Next Navigation with History Stack
  As a presentation author
  I want to navigate using P (previous) and N (next) keys based on viewing history
  So that I can revisit slides in the order I viewed them
  And provide a non-linear presentation flow

  Background:
    Given a rendered presentation with 20 slides
    And the presentation is displayed in a browser
    And I start on slide 0 (first slide)

  # Feature 1: Previous Navigation (P Key)
  Scenario: Navigate previous with populated history
    Given I have navigated through slides: 0 → 5 → 12 → 8
    And I am currently on slide 8
    When I press the "P" key
    Then I navigate to slide 12
    And slide 12 is the current slide
    And the forward stack contains [8]

  Scenario: Navigate previous multiple times
    Given I have navigated through slides: 0 → 5 → 12 → 8
    And I am on slide 8
    When I press "P" (navigate to 12)
    And I press "P" (navigate to 5)
    And I press "P" (navigate to 0)
    Then I am on slide 0
    And the forward stack contains [5, 12, 8]

  Scenario: Navigate previous with empty history (default to slide 0)
    Given I am on slide 0
    And the navigation history is empty
    When I press the "P" key
    Then I remain on slide 0
    And no error occurs
    And the forward stack remains empty

  Scenario: Navigate previous from middle slide with empty history
    Given I am on slide 10
    But I navigated directly via URL (no history)
    When I press the "P" key
    Then I navigate to slide 0 (first slide by default)
    And the forward stack contains [10]

  # Feature 2: Next Navigation (N Key)
  Scenario: Navigate next with forward history (redo)
    Given I have navigated through slides: 0 → 5 → 12 → 8
    And I pressed "P" to go back to 12
    And the forward stack contains [8]
    When I press the "N" key
    Then I navigate to slide 8 (popped from forward stack)
    And the forward stack is empty

  Scenario: Navigate next without forward history (linear advance)
    Given I am on slide 5
    And the forward stack is empty
    When I press the "N" key
    Then I navigate to slide 6 (linear next)
    And the visit stack is updated with [5]

  Scenario: Navigate next at last slide with no forward history
    Given I am on slide 19 (last slide)
    And the forward stack is empty
    When I press the "N" key
    Then I remain on slide 19 (no slide 20 exists)
    And no error occurs

  Scenario: Navigate next exhausts forward history then advances linearly
    Given I have navigated: 0 → 5 → 12 → 8
    And I pressed "P" twice (back to 5)
    And the forward stack contains [12, 8]
    When I press "N" (navigate to 12)
    And I press "N" (navigate to 8)
    And I press "N" (linear to 9)
    Then I am on slide 9
    And the forward stack is empty

  # Feature 3: History Stack Management
  Scenario: Visit stack preserves chronological order
    When I navigate to slide 1
    And I navigate to slide 5
    And I navigate to slide 3
    And I navigate to slide 8
    Then the visit stack contains [0, 1, 5, 3]
    And I am on slide 8

  Scenario: Forward stack clears on non-P/N navigation
    Given I have navigated: 0 → 5 → 12
    And I pressed "P" to go back to 5
    And the forward stack contains [12]
    When I press the right arrow to navigate to slide 6 (linear navigation)
    Then the forward stack is cleared
    And the visit stack is updated to [0, 5]
    And I am on slide 6

  Scenario: Goto navigation clears forward stack
    Given I have navigated: 0 → 5 → 12
    And I pressed "P" to go back to 5
    And the forward stack contains [12]
    When I press "G" and navigate to slide 20
    Then the forward stack is cleared
    And the visit stack is updated to [0, 5]
    And I am on slide 20

  # Feature 4: Duplicate Slide Visits
  Scenario: Visiting the same slide multiple times preserves temporal sequence
    When I navigate to slide 5
    And I navigate to slide 10
    And I navigate to slide 5 again
    And I navigate to slide 15
    Then the visit stack contains [0, 5, 10, 5]
    And I am on slide 15

  Scenario: Previous navigation to duplicate slide
    Given I have navigated: 0 → 5 → 10 → 5 → 15
    And I am on slide 15
    When I press "P"
    Then I navigate to slide 5 (most recent visit)
    When I press "P"
    Then I navigate to slide 10
    When I press "P"
    Then I navigate to slide 5 (earlier visit)

  # Feature 5: Unbounded History Stack
  Scenario: History stack grows without limit during session
    Given I navigate through 100 slides in sequence
    When I press "P" 99 times
    Then I successfully navigate back to slide 0
    And all 100 slides are preserved in the history stack

  # Feature 6: Timer Does Not Pause
  Scenario: Previous navigation does not pause timer
    Given the PresentationTimer is running
    And the timer shows "00:05:00"
    When I press "P" to navigate to the previous slide
    Then the PresentationTimer state remains Running
    And the timer continues incrementing
    And the timer is NOT paused

  Scenario: Next navigation does not pause timer
    Given the PresentationTimer is running
    And the timer shows "00:10:30"
    When I press "N" to navigate to the next slide
    Then the PresentationTimer state remains Running
    And the timer continues incrementing
    And the timer is NOT paused

  # Feature 7: Cross-Window Synchronization
  Scenario: Previous navigation syncs to speaker view
    Given the speaker view is open
    And I have navigated: 0 → 5 → 12
    And I am on slide 12
    When I press "P" in the main presentation
    Then the main presentation navigates to slide 5
    And the speaker view immediately updates to show slide 5
    And both windows display the same slide

  Scenario: Next navigation syncs to speaker view
    Given the speaker view is open
    And I have forward history containing [12]
    When I press "N" in the main presentation
    Then the main presentation navigates to slide 12
    And the speaker view immediately updates to show slide 12

  # Feature 8: Integration with Session Logging
  Scenario: Previous navigation is logged with NavigationMethod.Previous
    Given session logging is enabled
    And I have navigated: 0 → 5 → 12
    When I press "P" at timestamp "2025-12-29T14:30:15Z"
    Then the session log records a slide visit to slide 5
    And the navigation method is "Previous"
    And the entry timestamp is "2025-12-29T14:30:15Z"

  Scenario: Next navigation (redo) is logged with NavigationMethod.Next
    Given session logging is enabled
    And I have forward history [12]
    When I press "N" at timestamp "2025-12-29T14:31:22Z"
    Then the session log records a slide visit to slide 12
    And the navigation method is "Next"
    And the entry timestamp is "2025-12-29T14:31:22Z"

  Scenario: Next navigation (linear) is logged with NavigationMethod.Next
    Given session logging is enabled
    And I am on slide 5 with no forward history
    When I press "N" at timestamp "2025-12-29T14:32:10Z"
    Then the session log records a slide visit to slide 6
    And the navigation method is "Next"
    And the entry timestamp is "2025-12-29T14:32:10Z"

  # Feature 9: Complex Navigation Flows
  Scenario: Zig-zag navigation pattern
    When I navigate: 0 → 10 → 5 → 15 → 2
    And I press "P" four times (back to 0)
    And I press "N" four times (forward to 2)
    Then I am on slide 2
    And the forward stack is empty
    And the visit stack ends with [5, 15, 2]

  Scenario: Goto in middle of P/N sequence
    Given I have navigated: 0 → 5 → 10
    And I press "P" to go back to 5
    And the forward stack contains [10]
    When I press "G" and navigate to slide 20
    Then the forward stack is cleared
    And I am on slide 20
    When I press "P"
    Then I navigate to slide 5 (from visit stack)
    And I cannot press "N" to get back to slide 20 (forward stack was cleared)

  # Feature 10: Boundary Conditions
  Scenario: Navigate previous at first slide with history
    Given I have navigated: 0 → 5 → 10
    And I press "P" to slide 5
    And I press "P" to slide 0
    When I press "P" again
    Then I remain on slide 0
    And the forward stack contains [5, 10]

  Scenario: Rapid P/N toggling
    Given I am on slide 10
    When I press "P" (to previous)
    And I immediately press "N" (back to 10)
    And I immediately press "P" (to previous)
    And I immediately press "N" (back to 10)
    Then I am on slide 10
    And the navigation history accurately reflects all transitions

  # Feature 11: Arrow Key Equivalents
  Scenario: Right arrow is equivalent to linear next
    Given I am on slide 5
    And the forward stack is empty
    When I press the right arrow key
    Then I navigate to slide 6
    And the visit stack is updated
    And the forward stack is cleared (same as "N" behavior)

  Scenario: Left arrow is traditional previous (linear, not history-based)
    Given I am on slide 10
    When I press the left arrow key
    Then I navigate to slide 9 (linear previous, NOT history-based)
    And the visit stack is updated with [10]

  # Feature 12: Invariant Validation
  Scenario: Current slide index always valid
    Given I have navigated through various slides
    When I press "P" or "N" multiple times
    Then the current slide index is always >= 0
    And the current slide index is always < 20 (total slides)

  Scenario: Visit stack order preserved
    When I navigate to slides in order: 5, 3, 8, 2, 15
    Then the visit stack contains exactly [0, 5, 3, 8, 2]
    And the order is preserved (FIFO for append, LIFO for P key pop)

  Scenario: Forward stack cleared on new navigation
    Given I have forward history [12, 8]
    When I navigate to slide 15 via goto
    Then the forward stack is empty
    And the visit stack does not contain slide 15 yet (will be added on next navigation)

  # Feature 13: P/N Keys Excluded from Session Logging Events
  Scenario: P key press is not logged as an event (too noisy)
    Given session logging is enabled
    When I press "P" to navigate to the previous slide
    Then the session log records the slide visit
    But the session log does NOT record a "P" key press event
    And the events log does not contain an entry for key "P"

  Scenario: N key press is not logged as an event (too noisy)
    Given session logging is enabled
    When I press "N" to navigate to the next slide
    Then the session log records the slide visit
    But the session log does NOT record an "N" key press event
    And the events log does not contain an entry for key "N"

  # Feature 14: Break Mode Interaction
  Scenario: Previous navigation works during break mode
    Given BreakMode is active
    And I have navigated: 0 → 5 → 10
    And I am on slide 10
    When I press "P"
    Then I navigate to slide 5
    And the speaker view shows slide 5 content
    And the main view still shows the break screen
    And the PresentationTimer remains paused

  Scenario: Next navigation works during break mode
    Given BreakMode is active
    And I have forward history [15]
    When I press "N"
    Then I navigate to slide 15
    And the speaker view shows slide 15 content
    And the main view still shows the break screen
    And the PresentationTimer remains paused
