name := """blog-server"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers += "ruimo.com" at "http://static.ruimo.com/release"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  filters,
  evolutions,
  "com.ruimo" %% "scoins" % "1.7-SNAPSHOT",
  "com.h2database"  %  "h2" % "1.4.193",
  "com.typesafe.play" %% "anorm" % "2.5.0",
  specs2 % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

