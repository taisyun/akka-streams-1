name := "akka-stream-1"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
//  "com.typesafe.akka" %% "akka-stream-experimental" % "2.0.1"
  "com.typesafe.akka" %% "akka-stream" % "2.4.2",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.2",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.2",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "junit" % "junit" % "4.12" % "test"

)
