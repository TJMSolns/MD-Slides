Feature: Break Mode
  As a presentation author
  I want to activate break mode during my presentation
  So that I can take a break without showing slide content to the audience
  And the timer pauses to exclude break time from my elapsed presentation time

  Background:
    Given a rendered presentation with 42 slides
    And the presentation is displayed in a browser
    And the PresentationTimer is running
    And I am on slide 15

  # Feature 1: Break Mode Activation
  Scenario: Activate break mode with B key
    When I press the "B" key
    Then BreakMode is activated
    And the PresentationTimer is paused
    And the main presentation view displays the break screen
    And the speaker view shows slide 15 with a break indicator
    And the speaker notes remain visible

  Scenario: Deactivate break mode with B key
    Given BreakMode is active
    And the PresentationTimer is paused
    When I press the "B" key again
    Then BreakMode is deactivated
    And the PresentationTimer resumes
    And the main presentation view displays slide 15
    And the speaker view no longer shows the break indicator

  # Feature 2: Break Screen Configuration
  Scenario: Default break screen (no custom configuration)
    Given no break screen is configured
    When I activate break mode
    Then the break screen displays a solid black background
    And no error is shown

  Scenario: Custom break screen via CLI argument
    Given the presentation was rendered with "--break-screen images/break.png"
    And "images/break.png" exists and is readable
    When I activate break mode
    Then the break screen displays the image from "images/break.png"
    And the image is centered and scaled to fit the viewport

  Scenario: Custom break screen via project config
    Given the project config contains:
      """
      {
        "breakScreen": "images/custom-break.jpg"
      }
      """
    And "images/custom-break.jpg" exists
    When I activate break mode
    Then the break screen displays the image from "images/custom-break.jpg"

  Scenario: Custom break screen via global config
    Given no CLI argument or project config for break screen
    And the global config contains:
      """
      {
        "defaults": {
          "breakScreen": "~/default-break.png"
        }
      }
      """
    And "~/default-break.png" exists
    When I activate break mode
    Then the break screen displays the image from "~/default-break.png"

  Scenario: Break screen file not found (fallback to default)
    Given the presentation was rendered with "--break-screen images/missing.png"
    But "images/missing.png" does not exist
    When I activate break mode
    Then the break screen displays a solid black background
    And a warning is logged: "Break screen not found: images/missing.png (using default)"

  # Feature 3: Timer Integration
  Scenario: Timer pauses when break mode activates
    Given the PresentationTimer shows "00:15:30"
    And the PresentationTimer is running
    When I activate break mode
    Then the PresentationTimer state is Paused
    And the timer display shows "00:15:30" (frozen)
    And the timer does not increment while break mode is active

  Scenario: Timer resumes when break mode deactivates
    Given BreakMode is active
    And the PresentationTimer is paused at "00:15:30"
    And 2 minutes pass during the break
    When I deactivate break mode
    Then the PresentationTimer state is Running
    And the timer resumes from "00:15:30"
    And the elapsed time increases as normal
    And the 2-minute break duration is excluded from elapsed time

  Scenario: Multiple break sessions accumulate paused time
    Given the PresentationTimer has been running for 10 minutes
    When I activate break mode for 2 minutes
    And I deactivate break mode
    And I continue presenting for 5 minutes
    And I activate break mode for 3 minutes
    And I deactivate break mode
    Then the total elapsed time is 15 minutes (10 + 5)
    And the total paused duration is 5 minutes (2 + 3)
    And the PresentationTimer displays "00:15:00"

  # Feature 4: Cross-Window Synchronization
  Scenario: Break mode syncs to speaker view
    Given the speaker view is open in a separate window
    And the main presentation is on slide 15
    When I activate break mode in the main presentation
    Then the speaker view immediately shows a break indicator
    And the speaker view still displays slide 15 content and notes
    And the speaker view timer is paused at the same time as the main view

  Scenario: Break mode deactivation syncs to speaker view
    Given BreakMode is active
    And the speaker view shows the break indicator
    When I deactivate break mode in the main presentation
    Then the speaker view immediately removes the break indicator
    And the speaker view timer resumes

  # Feature 5: Speaker View Behavior During Break
  Scenario: Speaker view shows current slide during break
    Given I am on slide 20
    And the speaker notes for slide 20 are "Discuss the architecture diagram"
    When I activate break mode
    Then the main presentation shows the break screen
    And the speaker view shows slide 20 content
    And the speaker view shows the notes "Discuss the architecture diagram"
    And the speaker view displays a break indicator (e.g., "BREAK MODE ACTIVE")

  Scenario: Navigate slides while in break mode
    Given BreakMode is active
    And I am on slide 15
    When I press the right arrow key to advance to slide 16
    Then the main presentation still shows the break screen (no change)
    And the speaker view updates to show slide 16 content
    And the speaker view still shows the break indicator
    And the PresentationTimer remains paused

  # Feature 6: Invariant Enforcement
  Scenario: Cannot activate break mode when already active
    Given BreakMode is active
    When I press the "B" key
    Then BreakMode is deactivated (toggle behavior)
    And no error occurs

  Scenario: Break mode activation pauses timer automatically
    Given the PresentationTimer is running
    And BreakMode is inactive
    When I activate break mode
    Then the PresentationTimer state transitions from Running to Paused
    And no manual pause command is required

  # Feature 7: Break Mode and Goto Interaction
  Scenario: Goto popup is disabled during break mode
    Given BreakMode is active
    When I press the "G" key
    Then the Goto popup does NOT open
    And a message is shown: "Goto is disabled during break mode"
    And BreakMode remains active

  Scenario: Break mode can be activated while Goto popup is open
    Given the Goto popup is open
    When I press the "B" key
    Then the Goto popup closes
    And BreakMode is activated
    And the PresentationTimer is paused

  # Feature 8: Break Count Tracking
  Scenario: Break count increments with each break session
    Given this is a new presentation session
    And no breaks have been taken yet
    When I activate break mode
    And I deactivate break mode
    Then the break count is 1

    When I activate break mode again
    And I deactivate break mode
    Then the break count is 2

  Scenario: Break count is session-scoped (not cumulative across presentations)
    Given I gave a presentation yesterday with 3 breaks
    And today I start a new presentation session
    When I activate break mode
    And I deactivate break mode
    Then the break count is 1 (not 4)

  # Feature 9: Edge Cases
  Scenario: Break mode at the first slide
    Given I am on slide 0 (first slide)
    When I activate break mode
    Then the break screen is displayed
    And the speaker view shows slide 0 content
    And the timer is paused

  Scenario: Break mode at the last slide
    Given I am on slide 41 (last slide of 42 total)
    When I activate break mode
    Then the break screen is displayed
    And the speaker view shows slide 41 content
    And the timer is paused

  Scenario: Rapid break mode toggling
    Given BreakMode is inactive
    When I press "B" to activate
    And I immediately press "B" to deactivate
    And I immediately press "B" to activate again
    Then BreakMode is active
    And the timer accurately reflects only the time outside of break mode

  Scenario: Break mode persists during slide navigation
    Given BreakMode is active on slide 10
    When I navigate to slide 15
    And I navigate to slide 20
    Then BreakMode remains active throughout navigation
    And the break screen is continuously displayed
    And the speaker view updates to show the current slide

  # Feature 10: Session Logging Integration
  Scenario: Break mode events are logged to session history
    Given session logging is enabled (display command was used)
    When I activate break mode at timestamp "2025-12-29T14:26:15Z"
    And I deactivate break mode at timestamp "2025-12-29T14:28:22Z"
    Then the session log contains:
      """
      {
        "timestamp": "2025-12-29T14:26:15Z",
        "key": "B",
        "action": "break_mode_enabled"
      }
      """
    And the session log contains:
      """
      {
        "timestamp": "2025-12-29T14:28:22Z",
        "key": "B",
        "action": "break_mode_disabled"
      }
      """
