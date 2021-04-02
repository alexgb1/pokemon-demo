name := "poke"

version := "0.1"

scalaVersion := "2.13.4"

val circeVersion = "0.12.3"

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

val logging = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "io.chrisdavenport" %% "log4cats-slf4j" % "1.1.1",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.4"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.3.0",
  "org.typelevel" %% "cats-effect" % "2.3.0",
  "org.http4s" %% "http4s-blaze-client" % "0.21.19",
  "org.http4s" %% "http4s-blaze-server" % "0.21.19",
  "org.http4s" %% "http4s-core" % "0.21.19",
  "org.http4s" %% "http4s-circe" % "0.21.19",
  "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.17.12",
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "0.17.12",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "0.17.12",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % "0.17.12",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % "0.17.12",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % "0.17.12",
) ++ circe ++ logging
