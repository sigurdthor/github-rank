val http4sVersion = "0.21.0-SNAPSHOT"
val `zio-version` = "1.0.0-RC17"
val izumi_version = "0.10.0-M8-ZIORC16"
val circeVersion = "0.12.3"
val SilencerVersion = "1.4.4"

// Only necessary for SNAPSHOT releases
resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root = (project in file("."))
  .settings(
    organization := "org.sigurdthor",
    name := "github-rank",
    scalaVersion := "2.13.1",
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    scalacOptions := Seq(
      "-feature",
      "-deprecation",
      "-explaintypes",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-language:higherKinds",
      "-language:existentials",
      "-Xfatal-warnings",
      "-Xlint:-infer-any,_",
      "-Ywarn-value-discard",
      "-Ywarn-numeric-widen",
      "-Ywarn-extra-implicit",
      "-Ywarn-unused:_"
    ) ++ (if (isSnapshot.value) Seq.empty
    else
      Seq(
        "-opt:l:inline"
      )),
    libraryDependencies ++= Seq(
      "io.7mind.izumi" %% "logstage-core" % izumi_version,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-literal" % circeVersion % "test",
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "dev.zio" %% "zio" % `zio-version`,
      "dev.zio" %% "zio-test" % `zio-version` % "test",
      "dev.zio" %% "zio-test-sbt" % `zio-version` % "test",
      "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC10",
      ("com.github.ghik" % "silencer-lib" % SilencerVersion % "provided")
        .cross(CrossVersion.full),
      // plugins
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      compilerPlugin(
        ("org.typelevel" % "kind-projector" % "0.11.0").cross(CrossVersion.full)
      ),
      compilerPlugin(
        ("com.github.ghik" % "silencer-plugin" % SilencerVersion)
          .cross(CrossVersion.full)
      )
    )
  )