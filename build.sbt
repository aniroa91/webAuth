name := """bigdata-play"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.11.8"

libraryDependencies += guice
libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test,
  "com.ftel" % "bigdata-core_2.11" % "0.1.0-SNAPSHOT",
  "com.ftel" % "bigdata-dns_2.11" % "0.1.0-SNAPSHOT",
  "com.google.code.gson" % "gson" % "2.8.1",
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0"
  )
libraryDependencies += "com.h2database" % "h2" % "1.4.194"

//"com.ftel" % "bigdata-dns_2.11" % "0.1.0-SNAPSHOT",
