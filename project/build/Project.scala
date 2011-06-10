import sbt._
import de.element34.sbteclipsify._

class Project(info: ProjectInfo) extends DefaultProject(info) with Eclipsify {
  
  val liftVersion = "2.4-M1"

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

 // To publish to the cloudbees repos:
  override def managedStyle = ManagedStyle.Maven
  val publishTo = "liftmodules repository" at "https://repository-liftmodules.forge.cloudbees.com/release/"

  lazy val repo_user = systemOptional[String]("repo.user", "USERNAME NOT SET")
  lazy val repo_password = systemOptional[String]("repo.password", "PASSWORD NOT SET")

  // The name and domain format here are not arbitrary:
  Credentials.add("liftmodules repository", "repository-liftmodules.forge.cloudbees.com", repo_user.value, repo_password.value)

}
