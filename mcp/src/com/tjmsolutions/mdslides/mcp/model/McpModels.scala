package com.tjmsolutions.mdslides.mcp.model

import io.circe.{Encoder, Decoder, Json}
import io.circe.syntax.*
import io.circe.generic.semiauto.*

/** JSON-RPC 2.0 request from MCP client (via stdin). */
case class McpRequest(
  jsonrpc: String,
  id: Option[Json],
  method: String,
  params: Option[Json]
)

object McpRequest:
  given Decoder[McpRequest] = deriveDecoder

/** JSON-RPC 2.0 response to MCP client (via stdout). */
case class McpResponse(
  jsonrpc: String = "2.0",
  id: Option[Json],
  result: Option[Json] = None,
  error: Option[McpError] = None
)

object McpResponse:
  given Encoder[McpResponse] = Encoder.instance { r =>
    // JSON-RPC 2.0: omit null fields; result and error are mutually exclusive
    val fields = List(
      Some("jsonrpc" -> Json.fromString(r.jsonrpc)),
      r.id.map(v => "id" -> v),
      r.result.map(v => "result" -> v),
      r.error.map(e => "error" -> Encoder[McpError].apply(e))
    ).flatten
    Json.obj(fields*)
  }

  def success(id: Option[Json], result: Json): McpResponse =
    McpResponse(id = id, result = Some(result))

  def error(id: Option[Json], code: Int, message: String): McpResponse =
    McpResponse(id = id, error = Some(McpError(code, message)))

case class McpError(code: Int, message: String)
object McpError:
  given Encoder[McpError] = deriveEncoder

/** Result returned by render_deck tool. */
case class RenderResult(
  success: Boolean,
  outputDir: String,
  files: List[String],
  slideCount: Int,
  warnings: List[String],
  errors: List[String]
)

object RenderResult:
  given Encoder[RenderResult] = deriveEncoder

/** Result returned by validate_deck tool. */
case class ValidationResult(
  valid: Boolean,
  slideCount: Int,
  errors: List[String],
  warnings: List[String]
)

object ValidationResult:
  given Encoder[ValidationResult] = deriveEncoder

/** Result returned by list_themes tool. */
case class ThemesResult(themes: List[String])

object ThemesResult:
  given Encoder[ThemesResult] = deriveEncoder

/** Result returned by get_deck_info tool. */
case class DeckInfo(
  slideCount: Int,
  templatesUsed: List[String],
  imagesReferenced: List[String],
  hasMermaidDiagrams: Boolean
)

object DeckInfo:
  given Encoder[DeckInfo] = deriveEncoder
