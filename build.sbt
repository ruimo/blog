name := """blog-server"""

maintainer := "ruimo.uno@gmail.com"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "3.4.0"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers += "ruimo.com" at "https://static.ruimo.com/release"

libraryDependencies ++= Seq(
  jdbc,
  ws,
  filters,
  evolutions,
  guice,
  "com.ruimo" %% "scoins" % "1.29",
  "com.h2database"  %  "h2" % "1.4.193",
  "org.playframework.anorm" %% "anorm" % "2.7.0",
  "org.playframework" %% "play-mailer" % "10.0.0",
  "org.playframework" %% "play-mailer-guice" % "10.0.0",
  specs2 % Test
)

fork in Test := true
parallelExecution in Test := false
