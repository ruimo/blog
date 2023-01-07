name := """blog-server"""

version := "1.0-SNAPSHOT"

maintainer := "ruimo.uno@gmail.com"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers += "ruimo.com" at "https://static.ruimo.com/release"

libraryDependencies ++= Seq(
  jdbc,
  ws,
  filters,
  evolutions,
  guice,
  "com.ruimo" %% "scoins" % "1.22",
  "com.h2database"  %  "h2" % "1.4.193",
  "org.playframework.anorm" %% "anorm" % "2.6.2",
  "com.typesafe.play" %% "play-mailer" % "7.0.0",
  "com.typesafe.play" %% "play-mailer-guice" % "7.0.0",
  specs2 % Test
)

fork in Test := true
parallelExecution in Test := false
