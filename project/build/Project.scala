import sbt._
import de.element34.sbteclipsify._

class Project(info: ProjectInfo) extends DefaultProject(info) with Eclipsify {
  
  val liftVersion = "2.2"

  override def compileOptions = super.compileOptions ++ Seq(Unchecked)

  override def libraryDependencies = Set(
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default" withSources(),
    
 	// These just to get the sources-jar
    "net.liftweb" %% "lift-util" % liftVersion % "compile->default" withSources(),
    "net.liftweb" %% "lift-common" % liftVersion % "compile->default" withSources(),
    "net.liftweb" %% "lift-actor" % liftVersion % "compile->default" withSources(),

	"log4j" % "log4j" % "1.2.16",
    "org.slf4j" % "slf4j-log4j12" % "1.6.1"

  ) ++ super.libraryDependencies

}
