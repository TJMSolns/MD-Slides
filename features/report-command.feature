Feature: Report Command
  As a presentation author
  I want to view a formatted report of my presentation session
  So that I can analyze timing, review navigation patterns, and improve future presentations

  Background:
    Given I have a rendered presentation "mdslides-tutorial"
    And the output directory is "mdslides-tutorial/"

  # Feature 1: Command Invocation
  Scenario: Successful report command invocation
    Given a session log exists at "mdslides-tutorial/mdslides-tutorial.log"
    When I run "java -jar ../mdslides.jar report mdslides-tutorial"
    Then the report is displayed to stdout
    And the command exits with code 0

  Scenario: Report command requires deck name argument
    When I run "java -jar ../mdslides.jar report" (no deck name)
    Then an error is displayed: "Usage: mdslides report <deck-name>"
    And the command exits with code 1

  # Feature 2: Log File Not Found
  Scenario: Error when log file doesn't exist
    Given no log file exists at "mdslides-tutorial/mdslides-tutorial.log"
    When I run "java -jar ../mdslides.jar report mdslides-tutorial"
    Then the following error is displayed:
      """
      ✗ No log file found for presentation: mdslides-tutorial

        Expected location: mdslides-tutorial/mdslides-tutorial.log

        To create a log file, present the deck using:
          java -jar ../mdslides.jar display mdslides-tutorial

        Note: Opening index.html directly in browser does NOT create logs.
      """
    And the command exits with code 1

  Scenario: Suggest display command when log not found
    Given no log file exists for "my-talk"
    When I run "java -jar ../mdslides.jar report my-talk"
    Then the error message suggests running "display my-talk"
    And the error explains that direct HTML opening doesn't create logs

  # Feature 3: Log File Parsing
  Scenario: Successful JSON log file parsing
    Given the log file contains valid JSON
    When I run the report command
    Then the log file is successfully parsed
    And the report is generated from the parsed data

  Scenario: Corrupted log file (invalid JSON)
    Given the log file contains invalid JSON at line 23
    When I run the report command
    Then the following error is displayed:
      """
      ✗ Failed to parse log file: mdslides-tutorial/mdslides-tutorial.log

        Parse error at line 23: Unexpected end of JSON input

        The log file may be corrupted. To regenerate:
        1. Delete or rename the corrupted log file
        2. Present the deck again using: java -jar ../mdslides.jar display mdslides-tutorial
      """
    And the command exits with code 2

  # Feature 4: Report Header Section
  Scenario: Report header displays session information
    Given the session log contains:
      """
      {
        "session": {
          "presentationName": "mdslides-tutorial",
          "startTime": "2025-12-29T14:23:45Z",
          "endTime": "2025-12-29T15:09:17Z",
          "theme": "dark",
          "totalSlides": 42
        }
      }
      """
    When I run the report command
    Then the output includes:
      """
      ═══════════════════════════════════════════════════════════════
        Presentation Report: mdslides-tutorial
      ═══════════════════════════════════════════════════════════════

      Session Information:
        Started:        2025-12-29 14:23:45
        Duration:       00:45:32
        Theme:          dark
        Total Slides:   42
        Slides Viewed:  38/42 (90%)
      """

  # Feature 5: Slide Timing Table
  Scenario: Slide timing summary table
    Given the session log contains slide visits for 42 slides
    When I run the report command
    Then the output includes a slide timing table:
      """
      Slide Timing Summary:
        ┌─────┬──────────────────────────────────────┬──────────┐
        │ No. │ Title                                │ Duration │
        ├─────┼──────────────────────────────────────┼──────────┤
        │  1  │ MDSlides v3.0.0                      │ 00:00:27 │
        │  2  │ About This Tutorial                  │ 00:00:51 │
        │  3  │ Configuration Precedence             │ 00:01:15 │
        │ ... │ ...                                  │ ...      │
        │ 42  │ Thank You!                           │ 00:00:18 │
        └─────┴──────────────────────────────────────┴──────────┘
      """

  Scenario: Slide title truncation
    Given slide 12 has title "This is a very long slide title that exceeds the maximum character limit for the table column width"
    When I run the report command
    Then the slide timing table shows:
      """
      │ 12  │ This is a very long slide title t... │ 00:03:25 │
      """
    And the title is truncated at 38 characters + "..."

  # Feature 6: Top 5 Longest Slides
  Scenario: Top 5 longest slides section
    Given the session log contains slides with various durations
    And slide 12 took 205 seconds
    And slide 18 took 167 seconds
    And slide 28 took 135 seconds
    And slide 3 took 75 seconds
    And slide 23 took 62 seconds
    When I run the report command
    Then the output includes:
      """
      Top 5 Longest Slides:
        1. Mermaid Diagrams (Slide 12)           03:25
        2. Two-Column Layout (Slide 18)          02:47
        3. Accessibility Features (Slide 28)     02:15
        4. Configuration System (Slide 3)        01:15
        5. Code Highlighting (Slide 23)          01:02
      """

  Scenario: Fewer than 5 slides viewed
    Given I only viewed 3 slides
    When I run the report command
    Then the "Top 5 Longest Slides" section shows only 3 slides
    And the title is "Top 3 Longest Slides" (dynamic)

  # Feature 7: Events Log Section
  Scenario: Events log displays chronological key presses
    Given the session log contains events:
      """
      [
        {
          "timestamp": "2025-12-29T14:26:15Z",
          "key": "B",
          "action": "break_mode_enabled"
        },
        {
          "timestamp": "2025-12-29T14:28:22Z",
          "key": "B",
          "action": "break_mode_disabled"
        },
        {
          "timestamp": "2025-12-29T14:29:10Z",
          "key": "S",
          "action": "speaker_view_opened"
        }
      ]
      """
    When I run the report command
    Then the output includes:
      """
      Events Log:
        14:26:15  [B] Break mode enabled
        14:28:22  [B] Break mode disabled
        14:29:10  [S] Speaker view opened
        14:35:42  [G] Goto slide 25
      """

  Scenario: No events logged (empty events array)
    Given the session log has an empty events array
    When I run the report command
    Then the output includes:
      """
      Events Log:
        No events recorded during this session.
      """

  # Feature 8: Navigation Statistics Section
  Scenario: Navigation statistics display
    Given the session log contains:
      - 35 forward navigations
      - 8 backward navigations (P key)
      - 3 goto navigations
      - 7 slides revisited
      - 45 total slide views
    When I run the report command
    Then the output includes:
      """
      Navigation Statistics:
        Forward navigations:      35
        Backward navigations:     8
        Goto jumps:              3
        Slides revisited:        7
      """

  Scenario: Navigation statistics calculation
    Given I viewed slides: 0, 5, 10, 5, 15, 20, 5
    When I run the report command
    Then the navigation stats show:
      - Total slide views: 7
      - Unique slides viewed: 5 (slides 0, 5, 10, 15, 20)
      - Slides revisited: 1 (slide 5 visited 3 times)

  # Feature 9: Duration Formatting
  Scenario Outline: Consistent hh:mm:ss duration format
    Given a slide duration of <seconds> seconds
    When I run the report command
    Then the duration is displayed as "<formatted>"

    Examples:
      | seconds | formatted |
      | 27      | 00:00:27  |
      | 185     | 00:03:05  |
      | 3625    | 01:00:25  |
      | 7325    | 02:02:05  |

  # Feature 10: Ongoing Session Support
  Scenario: Report for in-progress presentation
    Given the session log has startTime but no endTime (session still active)
    And the current time is "2025-12-29T14:42:15Z"
    And the startTime was "2025-12-29T14:23:45Z"
    When I run the report command
    Then the session information shows:
      """
      Session Information:
        Started:        2025-12-29 14:23:45
        Duration:       00:18:30 (IN PROGRESS)
        Theme:          dark
        Total Slides:   42
        Slides Viewed:  15/42 (36%)
      """
    And a note is displayed: "Session is currently in progress. Report shows data up to now."

  # Feature 11: Unicode Box-Drawing Tables
  Scenario: Report uses Unicode box-drawing characters
    When I run the report command
    Then the table borders use Unicode characters:
      | Character | Purpose                  |
      | ┌         | Top-left corner          |
      | ┐         | Top-right corner         |
      | └         | Bottom-left corner       |
      | ┘         | Bottom-right corner      |
      | ─         | Horizontal line          |
      | │         | Vertical line            |
      | ├         | Left intersection        |
      | ┤         | Right intersection       |
      | ┬         | Top intersection         |
      | ┴         | Bottom intersection      |
      | ┼         | Cross intersection       |

  # Feature 12: Report Footer
  Scenario: Report ends with separator line
    When I run the report command
    Then the last line of output is:
      """
      ═══════════════════════════════════════════════════════════════
      """

  # Feature 13: Multiple Presentation Names
  Scenario: Report for presentation in subdirectory
    Given the presentation is at "talks/conference-2025.md"
    And the log file is at "talks/conference-2025/conference-2025.log"
    When I run "java -jar ../mdslides.jar report talks/conference-2025"
    Then the report header shows "Presentation Report: conference-2025"
    And the report is generated successfully

  # Feature 14: Exit Codes
  Scenario Outline: Report command exit codes
    Given the log file status is <status>
    When I run the report command
    Then the exit code is <code>

    Examples:
      | status          | code |
      | exists, valid   | 0    |
      | not found       | 1    |
      | corrupted       | 2    |

  # Feature 15: Console Output Only (No File Write)
    Scenario: Report is displayed to stdout, not saved
    When I run the report command
    Then the report is printed to the console
    And no report file is created
    And the user can redirect output manually: "mdslides report my-talk > report.txt"

  # Feature 16: Empty Sections Handling
  Scenario: No slides viewed (empty slides array)
    Given the session log has an empty slides array
    When I run the report command
    Then the slide timing table shows:
      """
      Slide Timing Summary:
        No slides viewed during this session.
      """

  Scenario: All slides viewed exactly once
    Given I viewed all 42 slides exactly once
    When I run the report command
    Then the navigation stats show:
      - Unique slides viewed: 42
      - Total slide views: 42
      - Slides revisited: 0

  # Feature 17: Timezone Display
  Scenario: Timestamps displayed in local time
    Given the log file contains UTC timestamps
    And my system timezone is "America/New_York" (EST)
    When I run the report command
    Then timestamps are displayed in local time
    And the format is "YYYY-MM-DD HH:MM:SS" (without timezone suffix)

  # Feature 18: Error Handling for Malformed Log
  Scenario: Log file missing required fields
    Given the log file is missing the "session" field
    When I run the report command
    Then an error is displayed: "Invalid log file: missing required field 'session'"
    And the command exits with code 2

  Scenario: Log file has invalid timestamp format
    Given a slide visit has entryTime "not-a-timestamp"
    When I run the report command
    Then an error is displayed: "Invalid timestamp format in log file"
    And the command exits with code 2

  # Feature 19: Performance with Large Logs
  Scenario: Report handles large log files efficiently
    Given the session log contains 1000 slide visits
    And the log file is 5 MB in size
    When I run the report command
    Then the report is generated in under 2 seconds
    And all 1000 slides are included in the timing table

  # Feature 20: Verbose Output Option (Future Enhancement)
  Scenario: Normal output (default)
    When I run "java -jar ../mdslides.jar report mdslides-tutorial"
    Then the report shows summary sections only

  Scenario: Verbose output includes all slide visits
    When I run "java -jar ../mdslides.jar report mdslides-tutorial --verbose"
    Then the report includes detailed per-visit information
    And revisited slides show all visit instances

  # Feature 21: Report Width Constraints
  Scenario: Report fits standard terminal width (80 columns)
    When I run the report command
    Then the report width does not exceed 80 characters
    And the report renders correctly in a standard terminal

  Scenario: Wide terminal support
    When I run the report command in a 120-column terminal
    Then the report uses the full width
    And the slide title column is wider (no truncation)

  # Feature 22: Color Output (Future Enhancement)
  Scenario: No ANSI colors in default output
    When I run the report command
    Then the output contains no ANSI escape codes
    And the report is plain text with Unicode box-drawing only

  Scenario: Color output with --color flag
    When I run "java -jar ../mdslides.jar report mdslides-tutorial --color"
    Then section headers are colored (e.g., blue)
    And errors are colored red
    And the report uses ANSI color codes

  # Feature 23: Integration with Smart Workflow
  Scenario: Smart workflow shows report before displaying presentation
    Given a log file exists
    When I run "java -jar ../mdslides.jar mdslides-tutorial" (smart command)
    Then the report is displayed first
    And I see "Press Enter to continue..."
    And after pressing Enter, the presentation opens

  # Feature 24: Revisit Details
  Scenario: Identifying revisited slides
    Given I visited slides: 0, 5, 10, 5, 15, 10, 20
    When I run the report command
    Then the navigation stats show "Slides revisited: 2" (slides 5 and 10)
    And the slide timing table shows cumulative time for slides 5 and 10

  # Feature 25: Summary Calculations
  Scenario: Total duration includes paused time
    Given the session startTime is "2025-12-29T14:00:00Z"
    And the session endTime is "2025-12-29T14:45:32Z"
    When I run the report command
    Then the total duration is calculated as 45 minutes 32 seconds (2732 seconds)
    And the duration includes any break time

  Scenario: Slides viewed percentage
    Given total slides is 42
    And unique slides viewed is 38
    When I run the report command
    Then the session info shows "Slides Viewed: 38/42 (90%)"
    And the percentage is rounded to nearest integer

  # Feature 26: Edge Cases
  Scenario: Report for 1-slide presentation
    Given a presentation with only 1 slide
    And I viewed the single slide for 30 seconds
    When I run the report command
    Then the report displays correctly
    And the top slides section shows only 1 slide

  Scenario: Report for presentation with no breaks
    Given no break mode was activated during the session
    When I run the report command
    Then the events log shows no break events
    Or displays "No events recorded during this session."

  Scenario: Report for presentation closed immediately
    Given the session duration is 0 seconds
    When I run the report command
    Then the duration is "00:00:00"
    And the slides viewed is 1 (slide 0 only)
