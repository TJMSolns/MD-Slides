Feature: Display Command
  As a presentation author
  I want to open my rendered presentation in a browser with logging enabled
  So that I can deliver my talk and capture session metrics for later analysis

  Background:
    Given I have a markdown source file "my-talk.md"

  # Feature 1: Command Invocation
  Scenario: Successful display command invocation
    Given the presentation was rendered to "my-talk/index.html"
    When I run "java -jar ../mdslides.jar display my-talk"
    Then the browser opens "file:///absolute/path/to/my-talk/index.html"
    And a log file is created at "my-talk/my-talk.log"
    And the command exits with code 0
    And the command returns to the shell immediately (non-blocking)

  Scenario: Display command requires deck name argument
    When I run "java -jar ../mdslides.jar display" (no deck name)
    Then an error is displayed: "Usage: mdslides display <deck-name> [--browser <name>]"
    And the command exits with code 1

  # Feature 2: Presentation Not Rendered
  Scenario: Error when index.html doesn't exist
    Given the output directory "my-talk/" exists
    But "my-talk/index.html" does NOT exist
    When I run "java -jar ../mdslides.jar display my-talk"
    Then the following error is displayed:
      """
      ✗ Presentation not rendered: my-talk

        Expected file: my-talk/index.html (NOT FOUND)

        To render the presentation first, run:
          java -jar ../mdslides.jar render my-talk

        Or use the smart default command to auto-render:
          java -jar ../mdslides.jar my-talk
      """
    And the command exits with code 1
    And the browser does NOT open

  Scenario: Error when output directory doesn't exist
    Given the directory "my-talk/" does NOT exist
    When I run "java -jar ../mdslides.jar display my-talk"
    Then an error is displayed: "Output directory not found: my-talk/"
    And the command suggests running render first
    And the command exits with code 1

  # Feature 3: Log File Initialization
  Scenario: Log file created and initialized on display
    Given the presentation is rendered
    When I run the display command at "2025-12-29T14:23:45Z"
    Then a log file is created at "my-talk/my-talk.log"
    And the log file contains session metadata:
      """
      {
        "session": {
          "sessionId": "<UUID>",
          "presentationName": "my-talk",
          "startTime": "2025-12-29T14:23:45Z",
          "theme": "dark",
          "totalSlides": 42
        }
      }
      """

  Scenario: Log file overwrites previous session
    Given a log file already exists at "my-talk/my-talk.log"
    And the existing log contains data from yesterday's session
    When I run the display command today
    Then the old log file is truncated
    And the new session data overwrites the previous session
    And the old session data is lost

  # Feature 4: Log File Creation Failure (Non-Fatal)
  Scenario: Warning when log file cannot be created
    Given the output directory "my-talk/" is read-only
    When I run the display command
    Then a warning is displayed:
      """
      ⚠ Warning: Unable to create log file: my-talk/my-talk.log
        Reason: Permission denied

        Presentation will open without session logging.
        To enable logging, fix file permissions or run with appropriate privileges.
      """
    But the browser opens successfully
    And the command exits with code 0
    And I can give the presentation without logging

  Scenario: Disk full during log initialization
    Given the disk is full
    When I run the display command
    Then a warning about log file creation is displayed
    But the presentation opens anyway
    And the command is non-fatal

  # Feature 5: Browser Resolution (4-Layer Config Precedence)
  Scenario: Browser from CLI argument (highest precedence)
    Given the project config specifies browser "chromium"
    And the global config specifies browser "brave"
    When I run "java -jar ../mdslides.jar display my-talk --browser firefox"
    Then Firefox is launched
    And the CLI argument overrides all config

  Scenario: Browser from project config
    Given no CLI argument for browser
    And the project config contains:
      """
      {
        "browser": "chromium"
      }
      """
    And the global config specifies "firefox"
    When I run the display command
    Then Chromium is launched
    And project config overrides global config

  Scenario: Browser from global config
    Given no CLI argument or project config for browser
    And the global config contains:
      """
      {
        "defaults": {
          "browser": "brave"
        }
      }
      """
    When I run the display command
    Then Brave Browser is launched
    And global config overrides system default

  Scenario: System default browser (lowest precedence)
    Given no CLI argument, project config, or global config for browser
    When I run the display command on Linux
    Then the browser command is "xdg-open"
    When I run the display command on macOS
    Then the browser command is "open"
    When I run the display command on Windows
    Then the browser command is "cmd /c start"

  # Feature 6: Supported Browsers
  Scenario Outline: Launching different browsers
    When I run "java -jar ../mdslides.jar display my-talk --browser <browser>"
    Then the browser command is "<command>"
    And the presentation URL is passed as an argument

    Examples:
      | browser       | command       |
      | default       | xdg-open      |
      | firefox       | firefox       |
      | chromium      | chromium      |
      | google-chrome | google-chrome |
      | brave         | brave         |

  Scenario: Custom browser path
    When I run "java -jar ../mdslides.jar display my-talk --browser /usr/bin/custom-browser"
    Then the browser command is "/usr/bin/custom-browser"
    And the presentation URL is passed to the custom browser

  # Feature 7: Browser Not Found
  Scenario: Configured browser is not installed
    When I run "java -jar ../mdslides.jar display my-talk --browser safari"
    And "safari" is not found in PATH
    Then the following error is displayed:
      """
      ✗ Failed to launch browser: safari

        Error: Command not found: safari

        Troubleshooting:
        1. Check if Safari is installed: which safari
        2. Specify a different browser: --browser chromium
        3. Use system default: --browser default

        Available browsers to try:
        - firefox, chromium, google-chrome, brave
        - Or provide full path: --browser /usr/bin/custom-browser
      """
    And the command exits with code 2

  # Feature 8: Absolute File URLs
  Scenario: Browser launched with absolute file URL
    Given the output directory is "/home/user/projects/mdslides/my-talk/"
    When I run the display command
    Then the browser URL is "file:///home/user/projects/mdslides/my-talk/index.html"
    And the URL is an absolute path (not relative)

  Scenario: File URL on Windows
    Given the output directory is "C:\Users\user\mdslides\my-talk\"
    When I run the display command on Windows
    Then the browser URL is "file:///C:/Users/user/mdslides/my-talk/index.html"
    And backslashes are converted to forward slashes

  # Feature 9: Non-Blocking Browser Launch
  Scenario: Command returns to shell after launching browser
    When I run the display command
    Then the browser process starts in the background
    And the command returns to the shell immediately
    And I can continue using the terminal

  Scenario: Success message displayed
    When I run the display command
    Then the output includes:
      """
      ✓ Opened presentation in system default browser
        URL: file:///home/user/projects/mdslides/my-talk/index.html
      """

  # Feature 10: Multiple Display Invocations
  Scenario: Running display command multiple times
    When I run "java -jar ../mdslides.jar display my-talk"
    And I run "java -jar ../mdslides.jar display my-talk" again
    Then two browser windows/tabs are opened
    And the second invocation overwrites the log file
    And only the most recent session is logged

  # Feature 11: Different Deck Names
  Scenario: Display command resolves deck name to output directory
    When I run "java -jar ../mdslides.jar display talks/conference-2025"
    Then the output directory is resolved to "talks/conference-2025/"
    And the browser opens "talks/conference-2025/index.html"
    And the log file is "talks/conference-2025/conference-2025.log"

  # Feature 12: Console Output Verbosity
  Scenario: Default output (quiet)
    When I run the display command
    Then only success messages are printed
    And the output is:
      """
      ✓ Opened presentation in Firefox
        URL: file:///home/user/projects/mdslides/my-talk/index.html
      """

  Scenario: Verbose output with --verbose flag
    When I run "java -jar ../mdslides.jar display my-talk --verbose"
    Then detailed output is displayed:
      """
      Resolving output directory: my-talk
      ✓ Output directory: /home/user/projects/mdslides/my-talk
      ✓ Presentation file: /home/user/projects/mdslides/my-talk/index.html
      ✓ Log file initialized: /home/user/projects/mdslides/my-talk/my-talk.log
      ✓ Browser resolved: Firefox (from global config)
      ✓ Launching browser: firefox file:///home/user/projects/mdslides/my-talk/index.html
      ✓ Browser process started (PID: 12345)
      """

  # Feature 13: Exit Codes
  Scenario Outline: Display command exit codes
    Given the presentation status is <status>
    When I run the display command
    Then the exit code is <code>

    Examples:
      | status                     | code |
      | rendered, browser launched | 0    |
      | not rendered               | 1    |
      | browser not found          | 2    |
      | log file permission denied | 0    |

  # Feature 14: Integration with History Logging
  Scenario: Display command enables session logging
    When I run the display command
    Then a SessionLog is initialized
    And all slide visits are logged to "my-talk/my-talk.log"
    And all key press events (B, S, G) are logged
    And the log file is written as I navigate

  Scenario: Direct HTML open does NOT create log
    When I open "my-talk/index.html" directly in a browser (not via display command)
    Then no log file is created
    And no session logging occurs
    And the presentation runs normally without logging

  # Feature 15: Browser Process Management
  Scenario: Browser process runs independently
    When I run the display command
    And the browser starts
    And I close the terminal
    Then the browser continues running
    And the presentation remains open

  Scenario: Command does not wait for browser to close
    When I run the display command
    Then the command does NOT block until browser closes
    And the command returns immediately

  # Feature 16: Error Messages are Actionable
  Scenario: All error messages suggest solutions
    When any error occurs during display command
    Then the error message includes:
      - What went wrong
      - Why it happened
      - How to fix it (actionable steps)

  # Feature 17: Configuration Validation
  Scenario: Invalid browser configuration is caught early
    Given the project config contains:
      """
      {
        "browser": 123
      }
      """
    When I run the display command
    Then an error is displayed: "Invalid browser configuration: expected string, got number"
    And the command exits with code 1

  # Feature 18: Path Resolution
  Scenario: Deck name with relative path
    When I run "java -jar ../mdslides.jar display ../presentations/my-talk"
    Then the output directory is resolved to absolute path
    And the browser receives an absolute file URL

  Scenario: Deck name without extension
    When I run "java -jar ../mdslides.jar display my-talk"
    Then the output directory is "my-talk/"
    And the source is assumed to be "my-talk.md"

  # Feature 19: Cross-Platform Compatibility
  Scenario: Display command on Linux
    When I run the display command on Linux
    Then the default browser command is "xdg-open"
    And the file URL uses forward slashes

  Scenario: Display command on macOS
    When I run the display command on macOS
    Then the default browser command is "open"
    And the file URL uses forward slashes

  Scenario: Display command on Windows
    When I run the display command on Windows
    Then the default browser command is "cmd /c start"
    And the file URL is properly formatted for Windows

  # Feature 20: Log File Format Validation
  Scenario: Log file is valid JSON after initialization
    When I run the display command
    Then the log file "my-talk/my-talk.log" contains valid JSON
    And the JSON can be parsed by the report command

  # Feature 21: Session ID Generation
  Scenario: Each display invocation generates unique session ID
    When I run the display command at "2025-12-29T14:00:00Z"
    Then the session ID is a valid UUID v4
    When I run the display command again at "2025-12-29T15:00:00Z"
    Then a new session ID is generated
    And the two session IDs are different

  # Feature 22: Theme Detection
  Scenario: Log file records theme used for presentation
    Given the presentation was rendered with theme "dark"
    When I run the display command
    Then the log file session metadata includes `"theme": "dark"`

  Scenario: Theme detection from rendered HTML
    Given the HTML contains `<body class="theme-retisio">`
    When I run the display command
    Then the log file records theme as "retisio"

  # Feature 23: Total Slides Detection
  Scenario: Log file records total slide count
    Given the rendered presentation has 42 slides
    When I run the display command
    Then the log file session metadata includes `"totalSlides": 42`

  # Feature 24: Browser Arguments (Future Enhancement)
  Scenario: No custom browser arguments in v3.0.0
    When I run the display command
    Then the browser is launched with no custom arguments
    And the browser opens in default mode (not private/incognito)

  Scenario: Custom browser arguments in v3.1.0
    When I configure `"browserArgs": ["--private-window", "--new-instance"]`
    And I run the display command
    Then Firefox opens in private window mode with a new instance

  # Feature 25: Edge Cases
  Scenario: Display very long presentation name
    When I run "java -jar ../mdslides.jar display very-long-presentation-name-with-many-characters"
    Then the command resolves the output directory correctly
    And the log file is created with the full name

  Scenario: Display presentation with special characters in name
    When I run "java -jar ../mdslides.jar display 'my-talk-2025-q4'"
    Then the output directory is "my-talk-2025-q4/"
    And the log file is "my-talk-2025-q4/my-talk-2025-q4.log"

  Scenario: Display immediately after render
    When I run "java -jar ../mdslides.jar render my-talk"
    And I immediately run "java -jar ../mdslides.jar display my-talk"
    Then the display command succeeds
    And the browser opens the freshly rendered presentation

  # Feature 26: Concurrent Display Sessions
  Scenario: Two different presentations displayed concurrently
    When I run "java -jar ../mdslides.jar display talk-1"
    And I run "java -jar ../mdslides.jar display talk-2"
    Then both browsers open successfully
    And "talk-1/talk-1.log" is created
    And "talk-2/talk-2.log" is created
    And both sessions run independently

  # Feature 27: Help and Usage
  Scenario: Display command help
    When I run "java -jar ../mdslides.jar display --help"
    Then the usage information is displayed:
      """
      Usage: mdslides display <deck-name> [options]

      Open a rendered presentation in a browser with session logging enabled.

      Options:
        --browser <name>   Browser to use (firefox, chromium, google-chrome, brave, default)
        --verbose          Display detailed output
        --help             Show this help message

      Examples:
        mdslides display my-talk
        mdslides display my-talk --browser firefox
        mdslides display talks/conference-2025

      Note: Opening index.html directly in browser does NOT enable logging.
      """

  # Feature 28: Environment Variable Support (Future)
  Scenario: Browser from environment variable
    Given the environment variable MDSLIDES_BROWSER is set to "firefox"
    And no CLI argument or config specifies browser
    When I run the display command
    Then Firefox is launched
    And the environment variable has lower precedence than config
