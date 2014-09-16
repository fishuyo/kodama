
excludeFilter in unmanagedSources := "live.scala"

//mainClass in (Compile) := Some("com.fishuyo.seer.kodama.Gallery")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-remote" % "2.3.4"
)