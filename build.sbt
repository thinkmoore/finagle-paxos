import com.twitter.scrooge.ScroogeSBT

name := "benchmarks"

version := "0.0"

com.twitter.scrooge.ScroogeSBT.newSettings

libraryDependencies ++= Seq(
  "org.apache.thrift" % "libthrift" % "0.9.1",
  "com.twitter" %% "scrooge-core" % "3.11.2",
  "com.twitter" %% "finagle-core" % "6.2.0",
  "com.twitter" %% "finagle-thrift" % "6.2.0",
  "com.twitter" %% "finagle-http" % "6.2.0",
  "com.twitter" %% "util-collection" % "6.3.6"
)
