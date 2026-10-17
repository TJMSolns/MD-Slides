Feature: History Logging (Session Analytics)
  As a presentation author
  I want my presentation session to be automatically logged
  So that I can review timing, navigation patterns, and improve future presentations

  Background:
    Given a rendered presentation "mdslides-tutorial" with 42 slides
    And the presentation uses the "dark" theme

  # Feature 1: Log File Creation (Display Command Only)
  Scenario: Display command creates log file
    When I run "java -jar ../mdslides.jar display mdslides-tutorial"
    Then a log file is created at "mdslides-tutorial/mdslides-tutorial.log"
    And the log file is initialized with session metadata
    And the browser opens the presentation

  Scenario: Opening HTML directly does NOT create log file
    When I open "mdslides-tutorial/index.html" directly in a browser
    Then no log file is created
    And no session logging occurs

  Scenario: Log file location matches output directory structure
    When I run "java -jar ../mdslides.jar display talks/conference-2025"
    Then the log file is created at "talks/conference-2025/conference-2025.log"
    And the presentation name in the log is "conference-2025"

  # Feature 2: Log File Format (JSON)
  Scenario: Log file is valid JSON
    When I run the display command
    And I give the presentation
    And I close the browser
    Then the log file contains valid JSON
    And the JSON can be parsed without errors

  Scenario: Log file structure includes required sections
    When I run the display command and give the presentation
    Then the log file contains a "session" object
    And the log file contains a "slides" array
    And the log file contains an "events" array
    And the log file contains a "summary" object

  # Feature 3: Session Metadata
  Scenario: Session metadata is logged on initialization
    When I run the display command at "2025-12-29T14:23:45Z"
    Then the log file contains session metadata:
      """
      {
        "session": {
          "sessionId": "<UUID>",
          "presentationName": "mdslides-tutorial",
          "startTime": "2025-12-29T14:23:45Z",
          "theme": "dark",
          "totalSlides": 42
        }
      }
      """

  Scenario: Session end time is recorded when presentation closes
    When I run the display command at "2025-12-29T14:23:45Z"
    And I give the presentation for 30 minutes
    And I close the browser at "2025-12-29T14:53:45Z"
    Then the session metadata includes:
      """
      {
        "endTime": "2025-12-29T14:53:45Z"
      }
      """

  # Feature 4: Slide Visit Tracking
  Scenario: First slide visit is logged automatically
    When I run the display command
    And the presentation loads
    Then the log file contains a slide visit for slide 0:
      """
      {
        "slideIndex": 0,
        "slideTitle": "MDSlides v3.0.0",
        "entryTime": "2025-12-29T14:23:45Z",
        "exitTime": null,
        "navigationMethod": "start"
      }
      """

  Scenario: Slide navigation records entry and exit times
    When I run the display command at "2025-12-29T14:23:45Z"
    And I view slide 0 for 27 seconds
    And I navigate to slide 1 at "2025-12-29T14:24:12Z"
    Then the log file contains:
      """
      {
        "slideIndex": 0,
        "entryTime": "2025-12-29T14:23:45Z",
        "exitTime": "2025-12-29T14:24:12Z",
        "elapsedSeconds": 27,
        "navigationMethod": "start"
      }
      """
    And the log file contains:
      """
      {
        "slideIndex": 1,
        "entryTime": "2025-12-29T14:24:12Z",
        "exitTime": null,
        "navigationMethod": "next"
      }
      """

  Scenario: Auto-exit previous slide on new navigation
    When I am on slide 5
    And the slide 5 entry does not have an exitTime
    When I navigate to slide 6
    Then the slide 5 entry is updated with exitTime = navigation timestamp
    And the slide 6 entry is created with entryTime = navigation timestamp

  # Feature 5: Navigation Method Tracking
  Scenario Outline: Different navigation methods are logged correctly
    When I navigate to slide 10 using "<method>"
    Then the slide visit is logged with navigationMethod "<logged_method>"

    Examples:
      | method                | logged_method |
      | right arrow           | next          |
      | space bar             | next          |
      | P key (previous)      | previous      |
      | goto popup            | goto          |
      | direct URL navigation | direct        |
      | session start         | start         |

  Scenario: Linear next navigation
    When I press the right arrow to advance from slide 5 to slide 6
    Then the slide 6 visit has navigationMethod "next"

  Scenario: History-based previous navigation
    When I press "P" to go back from slide 10 to slide 5
    Then the slide 5 visit has navigationMethod "previous"

  Scenario: Goto navigation
    When I press "G", type "25", and press ENTER
    Then the slide 24 visit has navigationMethod "goto"

  # Feature 6: Event Logging (Key Presses)
  Scenario: Break mode activation is logged
    When I press "B" at timestamp "2025-12-29T14:26:15Z"
    Then the events array contains:
      """
      {
        "timestamp": "2025-12-29T14:26:15Z",
        "eventType": "KeyPress",
        "key": "B",
        "action": "break_mode_enabled"
      }
      """

  Scenario: Break mode deactivation is logged
    When I press "B" to deactivate break mode at "2025-12-29T14:28:22Z"
    Then the events array contains:
      """
      {
        "timestamp": "2025-12-29T14:28:22Z",
        "eventType": "KeyPress",
        "key": "B",
        "action": "break_mode_disabled"
      }
      """

  Scenario: Speaker view opened is logged
    When I press "S" at timestamp "2025-12-29T14:29:10Z"
    Then the events array contains:
      """
      {
        "timestamp": "2025-12-29T14:29:10Z",
        "eventType": "KeyPress",
        "key": "S",
        "action": "speaker_view_opened"
      }
      """

  Scenario: Goto navigation is logged as event
    When I press "G" and navigate to slide 25 at "2025-12-29T14:35:42Z"
    Then the events array contains:
      """
      {
        "timestamp": "2025-12-29T14:35:42Z",
        "eventType": "KeyPress",
        "key": "G",
        "action": "goto_slide_24"
      }
      """

  # Feature 7: Excluded Events (Not Logged)
  Scenario: P key press is NOT logged as an event (too noisy)
    When I press "P" to navigate to the previous slide
    Then the slide visit is logged with navigationMethod "previous"
    But the events array does NOT contain a key "P" entry

  Scenario: F key press (fullscreen toggle) is NOT logged (too noisy)
    When I press "F" to toggle fullscreen
    Then the events array does NOT contain a key "F" entry

  Scenario: Arrow key presses are NOT logged as events
    When I press the right arrow to navigate
    Then the slide visit is logged
    But the events array does NOT contain an arrow key entry

  # Feature 8: Session Summary
  Scenario: Session summary is calculated on presentation close
    When I run the display command
    And I view 38 unique slides out of 42 total
    And I make 45 total slide views (some slides revisited)
    And I present for 3600 seconds (1 hour)
    And I close the browser
    Then the log file contains a summary:
      """
      {
        "summary": {
          "totalDurationSeconds": 3600,
          "uniqueSlidesViewed": 38,
          "totalSlideViews": 45
        }
      }
      """

  Scenario: Summary duration calculation
    When I run the display command at "2025-12-29T14:00:00Z"
    And I close the browser at "2025-12-29T14:45:32Z"
    Then the summary totalDurationSeconds is 2732 (45 minutes 32 seconds)

  # Feature 9: Log File Overwrite Behavior
  Scenario: New display session overwrites previous log
    Given I ran the display command yesterday
    And a log file exists at "mdslides-tutorial/mdslides-tutorial.log"
    When I run the display command again today
    Then the old log file is overwritten
    And the new log file contains only today's session
    And the previous session data is lost

  Scenario: Preserving old session logs manually
    Given I want to preserve multiple session logs
    When I copy "mdslides-tutorial/mdslides-tutorial.log" to "mdslides-tutorial/mdslides-tutorial-2025-12-28.log"
    And I run the display command again
    Then both log files exist:
      | mdslides-tutorial/mdslides-tutorial.log          | (new session) |
      | mdslides-tutorial/mdslides-tutorial-2025-12-28.log | (old session) |

  # Feature 10: Multiple Break Sessions
  Scenario: Multiple break sessions are logged separately
    When I press "B" at "2025-12-29T14:10:00Z" (activate)
    And I press "B" at "2025-12-29T14:12:00Z" (deactivate)
    And I press "B" at "2025-12-29T14:20:00Z" (activate)
    And I press "B" at "2025-12-29T14:25:00Z" (deactivate)
    Then the events array contains 4 entries for key "B"
    And I can calculate total break time: (2 min + 5 min) = 7 minutes

  # Feature 11: Slide Revisit Tracking
  Scenario: Visiting the same slide multiple times is logged
    When I navigate to slide 5 at "2025-12-29T14:10:00Z"
    And I navigate to slide 10 at "2025-12-29T14:12:00Z"
    And I navigate to slide 5 again at "2025-12-29T14:15:00Z"
    Then the slides array contains two separate visits for slide 5:
      """
      [
        {
          "slideIndex": 5,
          "entryTime": "2025-12-29T14:10:00Z",
          "exitTime": "2025-12-29T14:12:00Z",
          "navigationMethod": "next"
        },
        {
          "slideIndex": 5,
          "entryTime": "2025-12-29T14:15:00Z",
          "exitTime": null,
          "navigationMethod": "goto"
        }
      ]
      """

  # Feature 12: Incomplete Sessions (No End Time)
  Scenario: Browser crash leaves session without endTime
    When I run the display command
    And I give the presentation
    And the browser crashes (no clean close)
    Then the log file contains startTime
    But endTime is null or missing
    And the slides array contains partial data up to the crash

  Scenario: Report command handles ongoing session
    When I run the display command
    And I give the presentation
    And the session is still active
    And I run "java -jar ../mdslides.jar report mdslides-tutorial" from another terminal
    Then the report displays the current data
    And the report shows "Session In Progress" indicator
    And the duration is calculated as (currentTime - startTime)

  # Feature 13: Slide Title Extraction
  Scenario: Slide titles are extracted from slide content
    When I navigate to slide 3
    And slide 3 has the heading "Configuration Precedence"
    Then the slide visit contains:
      """
      {
        "slideIndex": 3,
        "slideTitle": "Configuration Precedence"
      }
      """

  Scenario: Slide without heading has default title
    When I navigate to slide 10
    And slide 10 has no heading
    Then the slide visit contains:
      """
      {
        "slideIndex": 10,
        "slideTitle": "Slide 11"
      }
      """

  # Feature 14: Log File Initialization Failure (Non-Fatal)
  Scenario: Log file creation fails due to permissions
    Given the output directory is read-only
    When I run the display command
    Then a warning is displayed: "Unable to create log file: permission denied"
    But the presentation opens successfully
    And I can give the presentation without logging

  Scenario: Log file write fails during presentation
    Given logging is enabled
    But the disk becomes full during the presentation
    When I navigate to a new slide
    Then the slide visit write may fail silently
    And the presentation continues without interruption

  # Feature 15: Integration with Report Command
  Scenario: Report command reads the log file
    When I run the display command
    And I give the presentation
    And I close the browser
    And I run "java -jar ../mdslides.jar report mdslides-tutorial"
    Then the report displays the session data from the log file
    And the report shows slide timing, events, and navigation stats

  Scenario: Report command fails if log file doesn't exist
    Given no log file exists for "mdslides-tutorial"
    When I run "java -jar ../mdslides.jar report mdslides-tutorial"
    Then an error is displayed: "No log file found for presentation: mdslides-tutorial"
    And the error suggests running the display command

  # Feature 16: JSON Schema Validation
  Scenario: Log file conforms to expected JSON schema
    When I run the display command and give the presentation
    Then the log file JSON has required fields:
      | session.sessionId         | string  |
      | session.presentationName  | string  |
      | session.startTime         | ISO8601 |
      | session.theme             | string  |
      | session.totalSlides       | number  |
      | slides                    | array   |
      | events                    | array   |
      | summary                   | object  |

  # Feature 17: Edge Cases
  Scenario: Presentation with no navigation (only view first slide)
    When I run the display command
    And I view only slide 0 for 10 seconds
    And I close the browser
    Then the slides array contains exactly 1 entry (slide 0)
    And the summary uniqueSlidesViewed is 1
    And the summary totalSlideViews is 1

  Scenario: Rapid slide navigation
    When I rapidly navigate through 20 slides in 10 seconds
    Then all 20 slide visits are logged
    And each visit has a unique entryTime
    And the exitTime of slide N matches the entryTime of slide N+1

  Scenario: Log file size with long presentation
    When I give a 3-hour presentation
    And I visit 100 slides multiple times
    And I take 5 break sessions
    Then the log file size is manageable (< 1 MB)
    And the JSON remains parseable

  # Feature 18: Timezone Handling
  Scenario: Timestamps are in UTC ISO 8601 format
    When I run the display command in timezone "America/New_York"
    At local time "2025-12-29 09:23:45 EST"
    Then the log file startTime is "2025-12-29T14:23:45Z" (UTC)
    And all timestamps use the "Z" suffix (Zulu time)

  # Feature 19: Session ID Uniqueness
  Scenario: Each session has a unique session ID
    When I run the display command
    Then the session.sessionId is a valid UUID v4
    And the session ID is unique across multiple presentation sessions

  # Feature 20: Accessibility Events (Not Logged)
  Scenario: Accessibility features do not generate noise in log
    When I press "H" to toggle high contrast
    Or I press "+" to increase font size
    Or I press "-" to decrease font size
    Then these events are NOT logged in the events array
    And the log remains focused on presentation flow
