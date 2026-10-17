Feature: Smart Default Command
  As a presentation author
  I want a single command that intelligently decides whether to report, render, and/or display
  So that I can minimize repetitive CLI invocations and follow a smart workflow

  Background:
    Given I have a markdown source file "my-talk.md"

  # Feature 1: Command Invocation (No Subcommand)
  Scenario: Smart command invoked without subcommand
    When I run "java -jar ../mdslides.jar my-talk"
    Then the smart workflow analyzer starts
    And the workflow decision tree is evaluated

  Scenario: Smart command requires deck name
    When I run "java -jar ../mdslides.jar" (no deck name)
    Then an error is displayed: "Usage: mdslides <deck-name> [options]"
    Or the help text is displayed

  # Feature 2: First-Time Presenter (No Log, No Output)
  Scenario: First time running smart command (render + display)
    Given "my-talk.md" exists
    But the output directory "my-talk/" does NOT exist
    And no log file exists
    When I run "java -jar ../mdslides.jar my-talk"
    Then the console output is:
      """
      No log file found (presentation not given yet).

      Rendering presentation: my-talk
      ✓ Pre-rendering 5 mermaid diagram(s)...
      ✓ Rendering HTML with theme: light
      ✓ Writing files to: my-talk/
      ✓ Opened presentation in system default browser
        URL: file:///home/user/projects/mdslides/my-talk/index.html
      """
    And the workflow decision is: shouldReport=false, shouldRender=true, shouldDisplay=true
    And the render reason is "OutputNotFound"

  # Feature 3: After Giving Presentation (Log Exists, Output Up-to-Date)
  Scenario: Smart command after presentation session (report + display)
    Given "my-talk.md" exists
    And "my-talk/index.html" exists and is up-to-date
    And "my-talk/my-talk.log" exists from yesterday's presentation
    When I run "java -jar ../mdslides.jar my-talk"
    Then the console output is:
      """
      ═══════════════════════════════════════════════════════════════
        Presentation Report: my-talk
      ═══════════════════════════════════════════════════════════════

      [... full report output ...]

      Press Enter to continue...

      Presentation up-to-date: my-talk
      ✓ Opened presentation in system default browser
        URL: file:///home/user/projects/mdslides/my-talk/index.html
      """
    And the workflow decision is: shouldReport=true, shouldRender=false, shouldDisplay=true
    And the command waits for user input before displaying presentation

  Scenario: User presses Enter after viewing report
    Given the report is displayed
    And the command is waiting with prompt "Press Enter to continue..."
    When the user presses Enter
    Then the display phase begins
    And the browser opens the presentation

  # Feature 4: Modified Source File (Log Exists, Output Outdated)
  Scenario: Source modified after render (re-render + display)
    Given "my-talk.md" was modified at "2025-12-29T15:00:00Z"
    And "my-talk/index.html" was rendered at "2025-12-29T14:00:00Z" (older)
    And "my-talk/my-talk.log" exists
    When I run "java -jar ../mdslides.jar my-talk"
    Then the console output is:
      """
      Rendering presentation: my-talk (source modified)
      ✓ Pre-rendering 5 mermaid diagram(s)...
      ✓ Rendering HTML with theme: light
      ✓ Writing files to: my-talk/
      ✓ Opened presentation in system default browser
        URL: file:///home/user/projects/mdslides/my-talk/index.html
      """
    And the workflow decision is: shouldReport=false, shouldRender=true, shouldDisplay=true
    And the render reason is "OutputOutdated"
    And the report is NOT displayed (output is stale)

  # Feature 5: Workflow Decision Tree
  Scenario: Decision logic - Check log file first
    When the smart command starts
    Then the first check is: does log file exist?
    If yes: shouldReport = true
    If no: shouldReport = false

  Scenario: Decision logic - Check render status
    When the log file check completes
    Then the second check is: does output exist and is it up-to-date?
    If output missing: shouldRender = true (OutputNotFound)
    If source newer: shouldRender = true (OutputOutdated)
    If up-to-date: shouldRender = false

  Scenario: Decision logic - Always display
    When the workflow analysis completes
    Then shouldDisplay is always true
    And the display phase always executes last

  # Feature 6: Timestamp Comparison
  Scenario: Source timestamp vs. output timestamp
    Given "my-talk.md" has lastModified "2025-12-29T14:30:00Z"
    And "my-talk/index.html" has lastModified "2025-12-29T14:00:00Z"
    When the smart command compares timestamps
    Then the source is determined to be newer
    And the render reason is "OutputOutdated"

  Scenario: Second-level precision for timestamp comparison
    Given "my-talk.md" has lastModified "2025-12-29T14:30:45.123Z"
    And "my-talk/index.html" has lastModified "2025-12-29T14:30:45.789Z"
    When the smart command compares timestamps (truncated to seconds)
    Then both timestamps are "2025-12-29T14:30:45Z"
    And the output is considered up-to-date

  # Feature 7: Source File Not Found
  Scenario: Error when source file doesn't exist
    Given "my-talk.md" does NOT exist
    But "my-talk/index.html" exists
    When I run "java -jar ../mdslides.jar my-talk"
    Then the following error is displayed:
      """
      ✗ Source file not found: my-talk.md

        Smart command requires source file to determine render status.

        If you only want to display the existing rendered presentation:
          java -jar ../mdslides.jar display my-talk

        If you renamed or moved the source file, update the deck name.
      """
    And the command exits with code 1

  # Feature 8: Render Failure Handling
  Scenario: Render fails during smart workflow
    Given the source file contains invalid markdown
    When the smart command decides to render
    And the render phase fails
    Then the following error is displayed:
      """
      ✗ Render failed for: my-talk

        [Render error details...]

        Workflow halted. Fix the error and try again.
      """
    And the display phase does NOT execute
    And the command exits with code 1

  Scenario: No retry on render failure
    Given the render phase fails
    Then the smart command does NOT retry
    And the command exits immediately
    And the user must fix the error and re-run the command

  # Feature 9: Config Inheritance
  Scenario: Smart command respects render config
    Given the project config specifies theme "dark"
    And the project config specifies footer-text "Confidential"
    When the smart command decides to render
    Then the render uses theme "dark"
    And the footer text is "Confidential"
    And all 4-layer config precedence rules apply

  Scenario: Smart command respects display config
    Given the project config specifies browser "firefox"
    When the smart command decides to display
    Then Firefox is launched
    And all browser resolution rules apply

  # Feature 10: Exit Codes
  Scenario Outline: Smart command exit codes
    Given the workflow status is <status>
    When I run the smart command
    Then the exit code is <code>

    Examples:
      | status                      | code |
      | all phases successful       | 0    |
      | source file not found       | 1    |
      | render failed               | 1    |
      | display failed (browser)    | 2    |
      | log file parse error        | 0    |

  # Feature 11: Multiple Workflows
  Scenario Outline: Different workflow paths
    Given log exists: <log>, output exists: <output>, source newer: <newer>
    When I run the smart command
    Then shouldReport=<report>, shouldRender=<render>, shouldDisplay=<display>

    Examples:
      | log   | output | newer | report | render | display |
      | no    | no     | n/a   | false  | true   | true    |
      | no    | yes    | no    | false  | false  | true    |
      | no    | yes    | yes   | false  | true   | true    |
      | yes   | no     | n/a   | true   | true   | true    |
      | yes   | yes    | no    | true   | false  | true    |
      | yes   | yes    | yes   | false  | true   | true    |

  # Feature 12: Report Skip Optimization
  Scenario: Skip report when render is needed
    Given the log file exists from yesterday
    But the source file was modified (render needed)
    When I run the smart command
    Then the report is NOT displayed
    And the render phase executes immediately
    And the display phase executes after render
    And the workflow is optimized (report skipped for stale data)

  Scenario: Show report when output is up-to-date
    Given the log file exists
    And the output is up-to-date
    When I run the smart command
    Then the report is displayed first
    And the user reviews the report
    And after Enter, the presentation is displayed
    And the workflow values user review time

  # Feature 13: Blocking Prompt After Report
  Scenario: Prompt blocks until user presses Enter
    Given the report is displayed
    When the prompt "Press Enter to continue..." is shown
    Then the command waits for user input
    And the shell is blocked
    And pressing Enter continues the workflow

  Scenario: User can review report before continuing
    Given the report shows I spent 5 minutes on slide 12
    And I want to review this timing
    When the prompt is displayed
    Then I can take as long as needed to review
    And the command waits patiently
    And pressing Enter continues when I'm ready

  # Feature 14: Console Output Clarity
  Scenario: Clear status messages at each phase
    When the smart command executes
    Then each phase displays a clear status message
    And the user knows what's happening at every step

  Scenario: Status message for render (output missing)
    When the render phase executes because output is missing
    Then the message is "Rendering presentation: my-talk"
    And no reason suffix is displayed

  Scenario: Status message for render (source modified)
    When the render phase executes because source is newer
    Then the message is "Rendering presentation: my-talk (source modified)"

  Scenario: Status message for display (output up-to-date)
    When the display phase executes after skipping render
    Then the message is "Presentation up-to-date: my-talk"

  Scenario: Status message for display (after render)
    When the display phase executes after render
    Then no "up-to-date" message is displayed
    And the display success message is shown

  # Feature 15: Single Deck Only
  Scenario: Smart command processes one deck at a time
    When I run "java -jar ../mdslides.jar my-talk other-talk"
    Then an error is displayed: "Smart command accepts only one deck name"
    Or the second argument is ignored

  Scenario: No batch processing in v3.0.0
    When I want to process multiple decks
    Then I must run the smart command multiple times
    Or use a shell script to loop through decks

  # Feature 16: Integration with Display Logging
  Scenario: Smart command enables session logging
    When the smart command opens the presentation via display
    Then a log file is created for this session
    And all navigation is logged
    And the next smart command invocation will show the report

  # Feature 17: Error Recovery
  Scenario: No auto-retry on failure
    When any phase fails
    Then the command exits immediately
    And the user must fix the issue
    And re-run the command manually

  # Feature 18: Relative Path Support
  Scenario: Smart command with relative path
    When I run "java -jar ../mdslides.jar talks/my-talk"
    Then the source is "talks/my-talk.md"
    And the output is "talks/my-talk/"
    And the log is "talks/my-talk/my-talk.log"

  # Feature 19: Edge Cases
  Scenario: Empty log file (corrupted)
    Given the log file exists but is empty or corrupted
    When I run the smart command
    Then the report phase attempts to parse the log
    And a parse error is displayed
    But the command continues to render/display phases
    And the command is non-fatal (exit code 0)

  Scenario: Log file from very old session
    Given the log file is from 6 months ago
    And the output is up-to-date
    When I run the smart command
    Then the old report is still displayed
    And the user can review the old session
    And the workflow continues normally

  Scenario: Rapid smart command invocations
    When I run the smart command twice in quick succession
    Then both invocations succeed independently
    And the second may overwrite the first's log file

  # Feature 20: Workflow Tracing (Verbose Mode)
  Scenario: Verbose mode shows decision process
    When I run "java -jar ../mdslides.jar my-talk --verbose"
    Then the output includes:
      """
      Analyzing workflow for: my-talk
      Checking log file: my-talk/my-talk.log
      Log file: FOUND
      Decision: shouldReport = true

      Checking output status: my-talk/index.html
      Output exists: YES
      Source modified time: 2025-12-29T14:00:00Z
      Output modified time: 2025-12-29T14:00:00Z
      Decision: shouldRender = false (up-to-date)

      Workflow plan:
      1. Display report
      2. Skip render (output up-to-date)
      3. Display presentation

      Executing workflow...
      """

  # Feature 21: Help and Usage
  Scenario: Smart command help
    When I run "java -jar ../mdslides.jar --help"
    Then the help text includes information about the smart default command:
      """
      Smart Default Command:
        mdslides <deck-name>

      Intelligently decides whether to report, render, and/or display based on file state:
      - If log exists: display report first
      - If output missing or outdated: render
      - Always: display presentation

      This is the recommended workflow for iterative development and presentation delivery.

      Examples:
        mdslides my-talk              # Smart workflow
        mdslides talks/conference     # With relative path
      """

  # Feature 22: Comparison with Explicit Commands
  Scenario: Smart command vs. explicit commands
    When the output is up-to-date and log exists
    Then "mdslides my-talk" is equivalent to:
      """
      mdslides report my-talk
      <Press Enter>
      mdslides display my-talk
      """

    When the output is missing
    Then "mdslides my-talk" is equivalent to:
      """
      mdslides render my-talk
      mdslides display my-talk
      """

  # Feature 23: Performance
  Scenario: Fast workflow analysis
    When I run the smart command
    Then the file system checks complete in < 100ms
    And the workflow decision is near-instantaneous
    And the user perceives no delay

  # Feature 24: Cross-Platform Consistency
  Scenario: Smart command on Linux
    When I run the smart command on Linux
    Then the workflow logic is identical to other platforms
    And file paths use forward slashes

  Scenario: Smart command on Windows
    When I run the smart command on Windows
    Then the workflow logic is identical to other platforms
    And file paths are handled correctly

  # Feature 25: Future Enhancements
  Scenario: Multi-deck support in v3.1.0
    When I run "mdslides --batch talk-1 talk-2 talk-3" in the future
    Then all three decks are processed sequentially
    And the workflow is applied to each deck independently

  Scenario: Watch mode in v3.1.0
    When I run "mdslides my-talk --watch" in the future
    Then the command watches for file changes
    And automatically re-renders and refreshes browser on changes

  # Feature 26: CLI Options Inheritance
  Scenario: CLI options apply to render and display phases
    When I run "mdslides my-talk --theme dark --browser firefox"
    Then the render phase uses theme "dark"
    And the display phase uses browser "firefox"
    And all options are correctly inherited

  # Feature 27: Scenario Testing
  Scenario: Developer workflow (iterative editing)
    Given I'm iteratively editing "my-talk.md"
    When I run "mdslides my-talk" after each edit
    Then the command detects source changes
    And re-renders automatically
    And opens the updated presentation

  Scenario: Presenter workflow (after giving talk)
    Given I gave a presentation yesterday
    When I run "mdslides my-talk" today
    Then I see the report from yesterday's session
    And I can analyze my timing
    And decide whether to present again or make changes

  Scenario: Reviewer workflow (checking rendered output)
    Given a colleague rendered the presentation
    And I want to view it without modifying anything
    When I run "mdslides my-talk"
    Then the output is up-to-date
    And the presentation opens immediately
    And no unnecessary re-rendering occurs

  # Feature 28: Invariants
  Scenario: Display phase always executes
    Given any workflow path
    When the smart command completes successfully
    Then the display phase has executed
    And the browser has opened

  Scenario: Render phase never executes if output up-to-date
    Given the output is up-to-date
    When the smart command runs
    Then the render phase is skipped
    And the presentation opens immediately (fast path)

  Scenario: Report phase only if log exists
    Given the log file exists
    When the smart command runs
    Then the report phase executes first
    Given no log file exists
    When the smart command runs
    Then the report phase is skipped
