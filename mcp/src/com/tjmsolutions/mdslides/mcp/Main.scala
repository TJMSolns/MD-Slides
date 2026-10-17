package com.tjmsolutions.mdslides.mcp

import cats.effect.{ExitCode, IO, IOApp}

import java.io.{BufferedReader, InputStreamReader, PrintStream}

/** Entry point for the mdslides MCP server.
 *
 * Reads JSON-RPC 2.0 requests from stdin, dispatches to tool handlers,
 * writes responses to stdout. Stderr is used for diagnostic logging.
 *
 * Usage: mill mcp.run
 * Or as an MCP server in Claude Desktop / Claude Code settings.
 *
 * Related: ADR-013, MS-012
 */
object Main extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    val stdin = new BufferedReader(new InputStreamReader(System.in))
    val stdout = new PrintStream(System.out, true, "UTF-8")

    IO.blocking(System.err.println("mdslides MCP server started")) *>
      McpServer.run(stdin, stdout).as(ExitCode.Success)
