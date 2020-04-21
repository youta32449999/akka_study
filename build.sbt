name := "akka_study"

version := "0.1"

scalaVersion := "2.12.7"

val akkaVersion = "2.6.4"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5"
)