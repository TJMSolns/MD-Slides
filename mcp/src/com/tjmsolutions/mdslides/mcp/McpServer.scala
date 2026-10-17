package com.tjmsolutions.mdslides.mcp

import cats.effect.IO
import com.tjmsolutions.mdslides.mcp.model.*
import com.tjmsolutions.mdslides.mcp.tools.{GetDeckInfoTool, ListThemesTool, RenderDeckTool, ValidateDeckTool}
import io.circe.{Json, parser}
import io.circe.syntax.*

import java.io.{BufferedReader, InputStreamReader, PrintStream}

/** MCP stdio transport layer (JSON-RPC 2.0 over stdin/stdout). */
object McpServer:

  /** Capabilities advertised during initialize handshake. */
  private val capabilities: Json = Json.obj(
    "capabilities" -> Json.obj(
      "tools" -> Json.obj()
    ),
    "serverInfo" -> Json.obj(
      "name" -> Json.fromString("mdslides"),
      "version" -> Json.fromString("1.1.0")
    ),
    "protocolVersion" -> Json.fromString("2024-11-05")
  )

  private val toolList: Json = Json.obj(
    "tools" -> Json.arr(
      Json.obj(
        "name" -> Json.fromString("render_deck"),
        "description" -> Json.fromString(
          "Render a markdown slide deck to HTML. Returns paths to generated output files."
        ),
        "inputSchema" -> Json.obj(
          "type" -> Json.fromString("object"),
          "properties" -> Json.obj(
            "input_path" -> Json.obj(
              "type" -> Json.fromString("string"),
              "description" -> Json.fromString("Absolute or relative path to the .md deck file")
            ),
            "output_dir" -> Json.obj(
              "type" -> Json.fromString("string"),
              "description" -> Json.fromString("Directory where generated HTML files will be written")
            ),
            "theme" -> Json.obj(
              "type" -> Json.fromString("string"),
              "description" -> Json.fromString("Theme name: light (default), dark, or corporate"),
              "default" -> Json.fromString("light")
            ),
            "no_copy_images" -> Json.obj(
              "type" -> Json.fromString("boolean"),
              "description" -> Json.fromString("Skip copying image assets to output directory"),
              "default" -> Json.fromBoolean(false)
            )
          ),
          "required" -> Json.arr(
            Json.fromString("input_path"),
            Json.fromString("output_dir")
          )
        )
      ),
      Json.obj(
        "name" -> Json.fromString("validate_deck"),
        "description" -> Json.fromString(
          "Validate a markdown slide deck without rendering. Returns errors and density warnings."
        ),
        "inputSchema" -> Json.obj(
          "type" -> Json.fromString("object"),
          "properties" -> Json.obj(
            "input_path" -> Json.obj(
              "type" -> Json.fromString("string"),
              "description" -> Json.fromString("Absolute or relative path to the .md deck file")
            )
          ),
          "required" -> Json.arr(Json.fromString("input_path"))
        )
      ),
      Json.obj(
        "name" -> Json.fromString("list_themes"),
        "description" -> Json.fromString(
          "List available built-in and directory-based themes."
        ),
        "inputSchema" -> Json.obj(
          "type" -> Json.fromString("object"),
          "properties" -> Json.obj(
            "themes_dir" -> Json.obj(
              "type" -> Json.fromString("string"),
              "description" -> Json.fromString("Directory containing directory-based themes (default: ./themes)")
            )
          ),
          "required" -> Json.arr()
        )
      ),
      Json.obj(
        "name" -> Json.fromString("get_deck_info"),
        "description" -> Json.fromString(
          "Inspect a markdown slide deck without rendering: slide count, templates used, images referenced, and whether it contains Mermaid diagrams."
        ),
        "inputSchema" -> Json.obj(
          "type" -> Json.fromString("object"),
          "properties" -> Json.obj(
            "input_path" -> Json.obj(
              "type" -> Json.fromString("string"),
              "description" -> Json.fromString("Absolute or relative path to the .md deck file")
            )
          ),
          "required" -> Json.arr(Json.fromString("input_path"))
        )
      )
    )
  )

  def run(in: BufferedReader, out: PrintStream): IO[Unit] =
    def loop(): IO[Unit] =
      IO.blocking(Option(in.readLine())).flatMap {
        case None => IO.unit  // stdin closed
        case Some(line) if line.isBlank => loop()
        case Some(line) =>
          handleLine(line, out) *> loop()
      }
    loop()

  private def handleLine(line: String, out: PrintStream): IO[Unit] =
    parser.parse(line) match
      case Left(err) =>
        emit(out, McpResponse.error(None, -32700, s"Parse error: ${err.message}"))
      case Right(json) =>
        json.as[McpRequest] match
          case Left(err) =>
            emit(out, McpResponse.error(None, -32600, s"Invalid request: ${err.message}"))
          case Right(req) =>
            dispatch(req, out)

  private def dispatch(req: McpRequest, out: PrintStream): IO[Unit] =
    req.method match
      case "initialize" =>
        emit(out, McpResponse.success(req.id, capabilities))

      case "initialized" =>
        IO.unit  // notification; no response

      case "notifications/initialized" =>
        IO.unit  // notification; no response

      case "tools/list" =>
        emit(out, McpResponse.success(req.id, toolList))

      case "tools/call" =>
        handleToolCall(req, out)

      case other =>
        emit(out, McpResponse.error(req.id, -32601, s"Method not found: $other"))

  private def handleToolCall(req: McpRequest, out: PrintStream): IO[Unit] =
    val argsJson = req.params
      .flatMap(_.asObject)
      .flatMap(_.apply("arguments"))
      .getOrElse(Json.obj())
    val toolName = req.params
      .flatMap(_.asObject)
      .flatMap(_.apply("name"))
      .flatMap(_.asString)

    toolName match
      case None =>
        emit(out, McpResponse.error(req.id, -32602, "tools/call requires 'name' in params"))

      case Some("render_deck") =>
        RenderDeckTool.parseParams(argsJson) match
          case Left(err) =>
            emit(out, McpResponse.error(req.id, -32602, err))
          case Right(params) =>
            RenderDeckTool.execute(params).flatMap {
              case Right(result) =>
                import com.tjmsolutions.mdslides.mcp.model.RenderResult.given
                emit(out, McpResponse.success(req.id,
                  Json.obj("content" -> Json.arr(
                    Json.obj(
                      "type" -> Json.fromString("text"),
                      "text" -> Json.fromString(result.asJson.noSpaces)
                    )
                  ))
                ))
              case Left(err) =>
                emit(out, McpResponse.error(req.id, -32603, err))
            }

      case Some("validate_deck") =>
        ValidateDeckTool.parseParams(argsJson) match
          case Left(err) =>
            emit(out, McpResponse.error(req.id, -32602, err))
          case Right(params) =>
            ValidateDeckTool.execute(params).flatMap {
              case Right(result) =>
                import com.tjmsolutions.mdslides.mcp.model.ValidationResult.given
                emit(out, McpResponse.success(req.id,
                  Json.obj("content" -> Json.arr(
                    Json.obj(
                      "type" -> Json.fromString("text"),
                      "text" -> Json.fromString(result.asJson.noSpaces)
                    )
                  ))
                ))
              case Left(err) =>
                emit(out, McpResponse.error(req.id, -32603, err))
            }

      case Some("list_themes") =>
        ListThemesTool.parseParams(argsJson) match
          case Left(err) =>
            emit(out, McpResponse.error(req.id, -32602, err))
          case Right(params) =>
            ListThemesTool.execute(params).flatMap {
              case Right(result) =>
                import com.tjmsolutions.mdslides.mcp.model.ThemesResult.given
                emit(out, McpResponse.success(req.id,
                  Json.obj("content" -> Json.arr(
                    Json.obj(
                      "type" -> Json.fromString("text"),
                      "text" -> Json.fromString(result.asJson.noSpaces)
                    )
                  ))
                ))
              case Left(err) =>
                emit(out, McpResponse.error(req.id, -32603, err))
            }

      case Some("get_deck_info") =>
        GetDeckInfoTool.parseParams(argsJson) match
          case Left(err) =>
            emit(out, McpResponse.error(req.id, -32602, err))
          case Right(params) =>
            GetDeckInfoTool.execute(params).flatMap {
              case Right(result) =>
                import com.tjmsolutions.mdslides.mcp.model.DeckInfo.given
                emit(out, McpResponse.success(req.id,
                  Json.obj("content" -> Json.arr(
                    Json.obj(
                      "type" -> Json.fromString("text"),
                      "text" -> Json.fromString(result.asJson.noSpaces)
                    )
                  ))
                ))
              case Left(err) =>
                emit(out, McpResponse.error(req.id, -32603, err))
            }

      case Some(name) =>
        emit(out, McpResponse.error(req.id, -32601, s"Unknown tool: $name"))

  private def emit(out: PrintStream, response: McpResponse): IO[Unit] =
    import com.tjmsolutions.mdslides.mcp.model.McpResponse.given
    IO.blocking(out.println(response.asJson.noSpaces))
