package com.tjmsolutions.mdslides.cli

/**
 * CLI Command enumeration (v3.0.0).
 *
 * Supported commands:
 * - render: Render markdown to HTML
 * - display: Open presentation in browser with logging
 * - report: Show analytics from session log
 * - config: Show merged configuration
 * - default: Smart default (report → render if needed → display)
 */
enum Command:
  case Render
  case Display
  case Report
  case Config
  case Default  // Smart workflow
