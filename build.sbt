name := "poke"

version := "0.1"

scalaVersion := "2.13.4"

val circeVersion = "0.12.3"

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.3.0",
  "org.typelevel" %% "cats-effect" % "2.3.0",
  "org.http4s" %% "http4s-blaze-client" % "0.21.19",
  "org.http4s" %% "http4s-blaze-server" % "0.21.19",
  "org.http4s" %% "http4s-core" % "0.21.19",
  "org.http4s" %% "http4s-circe" % "0.21.19",
  "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.17.12",
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "0.17.12",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "0.17.12"
) ++ circe
