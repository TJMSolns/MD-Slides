Feature: Presentation Timer
  As a presenter
  I want to see elapsed presentation time
  So that I can manage my presentation duration

  Background:
    Given a presentation is loaded in the browser
    And the PresentationTimer is in NotStarted state

  # =========================================================================
  # Scenario 1: Timer Starts Automatically
  # =========================================================================
  Scenario: Timer starts when presentation loads
    When the presentation finishes loading
    Then the PresentationTimer state should be Running
    And the elapsed time should be "00:00:00"
    And the timer should be displayed in the footer bottom-left corner

  # =========================================================================
  # Scenario 2: Timer Increments Every Second
  # =========================================================================
  Scenario: Timer increments while running
    Given the PresentationTimer is in Running state
    And the elapsed time is "00:00:00"
    When 5 seconds pass
    Then the elapsed time should be "00:00:05"
    And the footer display should update every second

  # =========================================================================
  # Scenario 3: Timer Shows Correct Format
  # =========================================================================
  Scenario Outline: Timer displays in hh:mm:ss format
    Given the PresentationTimer is in Running state
    And <seconds> seconds have elapsed
    When I view the timer display
    Then the formatted time should be "<formatted>"

    Examples:
      | seconds | formatted |
      | 0       | 00:00:00  |
      | 59      | 00:00:59  |
      | 60      | 00:01:00  |
      | 61      | 00:01:01  |
      | 3599    | 00:59:59  |
      | 3600    | 01:00:00  |
      | 3661    | 01:01:01  |
      | 7200    | 02:00:00  |

  # =========================================================================
  # Scenario 4: Timer Pauses During Break Mode
  # =========================================================================
  Scenario: Pause timer when break mode activated
    Given the PresentationTimer is in Running state
    And the elapsed time is "00:05:30"
    When the presenter presses the 'B' key
    Then the PresentationTimer state should be Paused
    And the elapsed time should remain "00:05:30"
    And the timer display should stop updating

  # =========================================================================
  # Scenario 5: Timer Resumes After Break
  # =========================================================================
  Scenario: Resume timer when break mode deactivated
    Given the PresentationTimer is in Paused state
    And the elapsed time is "00:10:00"
    And the timer was paused for 120 seconds
    When the presenter presses the 'B' key again
    Then the PresentationTimer state should be Running
    And the elapsed time should be "00:10:00"
    And the timer display should resume updating every second

  # =========================================================================
  # Scenario 6: Paused Duration Excluded from Elapsed Time
  # =========================================================================
  Scenario: Paused time does not count toward elapsed time
    Given the PresentationTimer is in Running state
    And 300 seconds have elapsed
    When the presenter pauses the timer
    And 120 seconds pass while paused
    And the presenter resumes the timer
    And 300 seconds more pass
    Then the elapsed time should be "00:10:00"
    And the total paused duration should be 120 seconds

  # =========================================================================
  # Scenario 7: Multiple Pause/Resume Cycles
  # =========================================================================
  Scenario: Handle multiple pause/resume cycles
    Given the PresentationTimer is in Running state
    When the following sequence occurs:
      | action | duration_seconds |
      | run    | 300              |
      | pause  | 60               |
      | resume | 0                |
      | run    | 300              |
      | pause  | 120              |
      | resume | 0                |
      | run    | 300              |
    Then the elapsed time should be "00:15:00"
    And the total paused duration should be 180 seconds

  # =========================================================================
  # Scenario 8: Timer Syncs to Speaker View
  # =========================================================================
  Scenario: Timer synchronizes with speaker view
    Given the PresentationTimer is in Running state
    And the elapsed time is "00:05:00"
    When the presenter opens the speaker view
    Then the speaker view timer should display "00:05:00"
    And the speaker view timer state should be Running

  # =========================================================================
  # Scenario 9: Pause Syncs to Speaker View
  # =========================================================================
  Scenario: Pause state synchronizes with speaker view
    Given the PresentationTimer is in Running state
    And the speaker view is open
    And the elapsed time is "00:08:30"
    When the presenter pauses the timer in the main window
    Then the speaker view timer should also pause
    And the speaker view should display "00:08:30"

  # =========================================================================
  # Scenario 10: Resume Syncs to Speaker View
  # =========================================================================
  Scenario: Resume state synchronizes with speaker view
    Given the PresentationTimer is in Paused state
    And the speaker view is open
    And the elapsed time is "00:12:00"
    When the presenter resumes the timer in the main window
    Then the speaker view timer should also resume
    And both timers should continue incrementing from "00:12:00"

  # =========================================================================
  # Scenario 11: Cannot Pause When Not Running
  # =========================================================================
  Scenario: Reject pause command when timer not running
    Given the PresentationTimer is in NotStarted state
    When the presenter attempts to pause the timer
    Then the pause command should be rejected
    And the PresentationTimer state should remain NotStarted
    And an error message should indicate "Cannot pause when not running"

  # =========================================================================
  # Scenario 12: Cannot Resume When Not Paused
  # =========================================================================
  Scenario: Reject resume command when timer not paused
    Given the PresentationTimer is in Running state
    When the presenter attempts to resume the timer
    Then the resume command should be rejected
    And the PresentationTimer state should remain Running
    And an error message should indicate "Cannot resume when not paused"

  # =========================================================================
  # Scenario 13: Timer Has No Reset Capability
  # =========================================================================
  Scenario: Timer cannot be reset to 00:00:00
    Given the PresentationTimer is in Running state
    And the elapsed time is "00:15:30"
    When the presenter attempts to reset the timer
    Then the reset command should not be available
    And the elapsed time should remain "00:15:30"

  # =========================================================================
  # Scenario 14: Timer State Persists During Navigation
  # =========================================================================
  Scenario: Timer continues running during slide navigation
    Given the PresentationTimer is in Running state
    And the elapsed time is "00:03:00"
    And the presentation is on slide 5
    When the presenter navigates to slide 10
    Then the PresentationTimer state should remain Running
    And the timer should continue incrementing
    And the elapsed time should be greater than "00:03:00"

  # =========================================================================
  # Scenario 15: Paused Timer Persists During Navigation
  # =========================================================================
  Scenario: Paused timer remains paused during slide navigation
    Given the PresentationTimer is in Paused state
    And the elapsed time is "00:07:45"
    And the presentation is on slide 3
    When the presenter navigates to slide 8
    Then the PresentationTimer state should remain Paused
    And the elapsed time should remain "00:07:45"

  # =========================================================================
  # Scenario 16: Timer Ends When Presentation Closes
  # =========================================================================
  Scenario: Record final time when presentation closes
    Given the PresentationTimer is in Running state
    And the elapsed time is "00:45:00"
    When the presenter closes the presentation window
    Then a PresentationEnded event should be emitted
    And the final elapsed time should be recorded as "00:45:00"

  # =========================================================================
  # Scenario 17: Timer Display Format Edge Cases
  # =========================================================================
  Scenario Outline: Handle edge case durations correctly
    Given the PresentationTimer is in Running state
    And <seconds> seconds have elapsed
    When I view the timer display
    Then the formatted time should be "<formatted>"
    And the hours component should be "<hours>"
    And the minutes component should be "<minutes>"
    And the seconds component should be "<secs>"

    Examples:
      | seconds | formatted | hours | minutes | secs |
      | 0       | 00:00:00  | 00    | 00      | 00   |
      | 1       | 00:00:01  | 00    | 00      | 01   |
      | 59      | 00:00:59  | 00    | 00      | 59   |
      | 60      | 00:01:00  | 00    | 01      | 00   |
      | 3599    | 00:59:59  | 00    | 59      | 59   |
      | 3600    | 01:00:00  | 01    | 00      | 00   |
      | 86399   | 23:59:59  | 23    | 59      | 59   |
      | 86400   | 24:00:00  | 24    | 00      | 00   |
      | 359999  | 99:59:59  | 99    | 59      | 59   |

  # =========================================================================
  # Speaker Notes (for presenter reference)
  # =========================================================================
  # - Timer starts automatically when presentation loads (no manual start needed)
  # - Press 'B' to pause/resume timer (toggle break mode)
  # - Paused time is NOT included in elapsed time (only active presentation time counts)
  # - Timer syncs between main presentation and speaker view automatically
  # - No reset capability - timer represents true session duration
  # - Timer continues running when navigating between slides
  # - Maximum supported duration: 99:59:59 (nearly 100 hours)
