package com.tjmsolutions.mdslides.infrastructure.parser

import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.ext.tables.{TablesExtension, TableBlock}
import com.vladsch.flexmark.util.ast.Node
import scala.jdk.CollectionConverters._

class TableParserDebugTest:
  def debugTableStructure(): Unit =
    val markdown = """| Role | Responsibility |
|------|-----------------|
| **Program Manager** | Coordination |"""

    val parser = Parser.builder()
      .extensions(java.util.Arrays.asList(TablesExtension.create()))
      .build()
    
    val doc = parser.parse(markdown)
    
    println("\n=== FLEXMARK AST STRUCTURE ===")
    doc.getChildren.asScala.foreach { child =>
      println(s"Top: ${child.getClass.getSimpleName}")
      if child.isInstanceOf[TableBlock] then
        val table = child.asInstanceOf[TableBlock]
        val childCount = table.getChildren.asScala.size
        println(s"Table has $childCount children")
        
        table.getChildren.asScala.zipWithIndex.foreach { case (node, idx) =>
          println(s"\n  [$idx] ${node.getClass.getSimpleName}")
          debugNode(node, 2)
        }
    }

  private def debugNode(node: Node, indent: Int): Unit =
    val prefix = "  " * indent
    node.getChildren.asScala.foreach { child =>
      val chars = try 
        val c = child.getChars
        if c != null && c.length > 0 then s"'${c.toString}'" else "(empty)"
      catch 
        case _: Exception => "(no chars)"
      
      println(s"$prefix${child.getClass.getSimpleName}: $chars")
      debugNode(child, indent + 1)
    }

@main def runDebug: Unit =
  val test = TableParserDebugTest()
  test.debugTableStructure()
