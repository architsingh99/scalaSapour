name := """sapour"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.8"

PlayKeys.fileWatchService := play.dev.filewatch.FileWatchService.jdk7(play.sbt.run.toLoggerProxy(sLog.value))
libraryDependencies ++= Seq(
  filters,
  cacheApi,
  jdbc,
  ws,
  guice,
  "mysql" % "mysql-connector-java" % "8.0.17",
  "com.typesafe.play" %% "play-slick" % "5.0.0",
  "com.typesafe.slick" %% "slick" % "3.3.2",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.4.2",
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "com.github.nscala-time" %% "nscala-time" % "2.24.0",
  "com.github.mumoshu" %% "play2-memcached-play28" % "0.11.0",
  "org.playframework.anorm" %% "anorm" % "2.6.7",
  "com.enragedginger" %% "akka-quartz-scheduler" % "1.8.5-akka-2.6.x",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.2",
  "com.pauldijou" %% "jwt-play-json" % "4.2.0",
)
