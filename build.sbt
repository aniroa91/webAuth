name := """bigdata-play"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.11.8"

libraryDependencies += guice
libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test,
// Elastic4s
  "com.sksamuel.elastic4s" %% "elastic4s-core" % "5.4.0",
  "com.sksamuel.elastic4s" %% "elastic4s-tcp"  % "5.4.0",
  "com.sksamuel.elastic4s" %% "elastic4s-http" % "5.4.0",
  "com.google.code.gson" % "gson" % "2.8.1",
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0",
  "org.scalaj" % "scalaj-http_2.11" % "2.3.0",
  "org.jsoup" % "jsoup" % "1.8.3",
  // Slick
  "org.postgresql"  % "postgresql" % "42.0.0",
  "com.typesafe.slick" % "slick_2.11" % "3.2.0",
  "com.typesafe.slick" % "slick-hikaricp_2.11" % "3.2.0",
  guice,
  ehcache,
  "org.pac4j" % "play-pac4j" % "4.0.0",
  "org.pac4j" % "pac4j-oidc" % "2.1.0" exclude("commons-io" , "commons-io"),
  "commons-io" % "commons-io" % "2.4"
  )
libraryDependencies += "com.h2database" % "h2" % "1.4.194"
