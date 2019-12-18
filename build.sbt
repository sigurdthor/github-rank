scalaVersion := "2.13.1"

val http4sVersion = "0.21.0-SNAPSHOT"

// Only necessary for SNAPSHOT releases
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "dev.zio" %% "zio" % "1.0.0-RC17",
  "dev.zio" %% "zio-test" % "1.0.0-RC17",
  "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC10"
)

val sttpVersion = "2.0.0-RC5"
libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client" %% "core",
  "com.softwaremill.sttp.client" %% "async-http-client-backend-zio-streams"
).map(_ % sttpVersion)

val circeVersion = "0.12.3"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)