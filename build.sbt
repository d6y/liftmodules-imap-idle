name := "imap-idle"

liftVersion <<= liftVersion ?? "2.4"

version <<= liftVersion apply { _ + "-0.92" }
 
scalaVersion := "2.9.1"
 
crossScalaVersions := Seq("2.8.1", "2.9.0-1", "2.9.1")

resolvers += "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"

resolvers += "Scala Snapshots" at "http://scala-tools.org/repo-snapshots"

libraryDependencies <++= liftVersion { v =>
  "net.liftweb" %% "lift-webkit" % v % "compile->default" ::
  "net.liftweb" %% "lift-mapper" % v % "compile->default" ::
  Nil
}    

// Customize any further dependencies as desired
libraryDependencies ++= Seq(
  "org.scala-tools.testing" % "specs_2.9.0" % "1.6.8" % "test", // For specs.org tests
  "junit" % "junit" % "4.8" % "test->default", // For JUnit 4 testing
  "javax.servlet" % "servlet-api" % "2.5" % "provided->default",
  "com.h2database" % "h2" % "1.2.138", // In-process database, useful for development systems
  "ch.qos.logback" % "logback-classic" % "0.9.26" % "compile->default" // Logging
)

 // To publish to the Cloudbees repos:

publishTo := Some("liftmodules repository" at "https://repository-liftmodules.forge.cloudbees.com/release/")
 
credentials += Credentials( file("/private/liftmodules/cloudbees.credentials") )

