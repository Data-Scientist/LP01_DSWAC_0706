name := "Classifying Users"

version := "1.0"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-encoding", "UTF-8")

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.0.1",
  "org.apache.spark" % "spark-mllib_2.10" % "1.0.1",
  // access hadoop hdfs
  "org.apache.hadoop" % "hadoop-client" % "2.0.0-mr1-cdh4.4.0"
)

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

resolvers += "Cloudera Repository" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
