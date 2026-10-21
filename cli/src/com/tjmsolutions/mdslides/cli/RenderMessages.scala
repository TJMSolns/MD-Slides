package com.tjmsolutions.mdslides.cli

/** CLI messages whose wording is worth a test (MS-031). */
object RenderMessages:

  /** The shell command that creates an empty config file. */
  def createConfigCommand(path: String): String = s"echo '{}' > $path"

  /** One line summarizing diagram pre-rendering, counting only diagrams that actually rendered. */
  def diagramSummary(total: Int, rendered: Int): Option[String] =
    val fallback = total - rendered
    if total == 0 then None
    else if rendered == total then Some(s"✓ Pre-rendered $total diagram(s)")
    else if rendered == 0 then Some(s"⚠ $total diagram(s) will show their source with an install notice (install mermaid-cli for full support)")
    else Some(s"✓ Pre-rendered $rendered of $total diagram(s); $fallback will show its source with an install notice")

end RenderMessages
