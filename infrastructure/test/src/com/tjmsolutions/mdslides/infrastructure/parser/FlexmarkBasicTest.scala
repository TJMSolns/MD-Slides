package com.tjmsolutions.mdslides.infrastructure.parser

import munit.FunSuite
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.ast._
import scala.jdk.CollectionConverters.*

/**
 * Test basic Flexmark behavior with images.
 */
class FlexmarkBasicTest extends FunSuite:

  test("flexmark parses image nodes"):
    val parser = Parser.builder().build()
    val doc = parser.parse("![Test Image](https://via.placeholder.com/400x200)")

    println(s"\n=== Flexmark AST ===")
    println(s"Document: ${doc.getClass.getSimpleName}")
    println(s"Children count: ${doc.getChildren.asScala.size}")

    doc.getChildren.asScala.foreach { child =>
      println(s"Child: ${child.getClass.getSimpleName} - '${child.getChars}'")
      child.getChildren.asScala.foreach { grandchild =>
        println(s"  Grandchild: ${grandchild.getClass.getSimpleName} - '${grandchild.getChars}'")
      }
    }

    // Count Image nodes
    val imageCount = countNodes(doc, classOf[Image])
    println(s"Image nodes found: $imageCount")

    assertEquals(imageCount, 1, "Should find 1 Image node")

  private def countNodes(node: com.vladsch.flexmark.util.ast.Node, nodeClass: Class[_]): Int =
    val childCount = node.getChildren.asScala.map(child => countNodes(child, nodeClass)).sum
    val thisCount = if nodeClass.isInstance(node) then 1 else 0
    thisCount + childCount

end FlexmarkBasicTest
