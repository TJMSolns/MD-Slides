import mill._
import mill.scalalib._

// Shared Scala 3 configuration for all modules
trait MDSlidesModule extends ScalaModule {
  def scalaVersion = "3.3.1"

  override def scalacOptions = Seq(
    "-encoding", "UTF-8",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-Xfatal-warnings"
  )
}

// Pure functional domain — no I/O, no side effects (see docs/decisions/adr/ADR-007-pure-functional-domain.md)
object domain extends MDSlidesModule {
  override def ivyDeps = Agg(
    ivy"org.typelevel::cats-core:2.10.0"
  )

  object test extends ScalaTests with TestModule.Munit {
    override def ivyDeps = Agg(
      ivy"org.scalameta::munit:0.7.29",
      ivy"org.scalameta::munit-scalacheck:0.7.29",
      ivy"org.scalacheck::scalacheck:1.17.0"
    )
  }
}

// I/O adapters: markdown parsing, HTML rendering, theme loading, file I/O
object infrastructure extends MDSlidesModule {
  override def moduleDeps = Seq(domain)

  override def ivyDeps = Agg(
    ivy"org.typelevel::cats-effect:3.5.4",
    ivy"com.lihaoyi::os-lib:0.9.3",
    ivy"com.lihaoyi::scalatags:0.12.0",
    ivy"com.vladsch.flexmark:flexmark-all:0.64.8",
    ivy"io.circe::circe-core:0.14.6",
    ivy"io.circe::circe-generic:0.14.6",
    ivy"io.circe::circe-parser:0.14.6"
  )

  object test extends ScalaTests with TestModule.Munit {
    override def ivyDeps = Agg(
      ivy"org.scalameta::munit:0.7.29",
      ivy"org.typelevel::munit-cats-effect:2.0.0"
    )
  }
}

// CLI entry point — wires domain and infrastructure, no business logic
object cli extends MDSlidesModule {
  override def moduleDeps = Seq(domain, infrastructure)

  override def ivyDeps = Agg(
    ivy"com.monovore::decline:2.4.1",
    ivy"org.typelevel::cats-effect:3.5.4"
  )

  def mainClass = T { Some("com.tjmsolutions.mdslides.cli.Main") }

  object test extends ScalaTests with TestModule.Munit {
    override def ivyDeps = Agg(
      ivy"org.scalameta::munit:0.7.29",
      ivy"org.typelevel::munit-cats-effect:2.0.0"
    )
  }
}
