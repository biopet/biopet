package nl.lumc.sasc.biopet.core

import java.util.Properties
import org.apache.log4j.Logger

trait BiopetExecutable extends Logging {

  val pipelines: List[MainCommand]

  val tools: List[MainCommand]

  val modules: Map[String, List[MainCommand]] = Map(
    "pipeline" -> pipelines,
    "tool" -> tools
  )

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {

    def toBulletedList(m: List[MainCommand], kind: String = "", bullet: String = "-") =
      "Available %s(s):\n  ".format(kind) + bullet + " " + m.map(x => x.commandName).sorted.mkString("\n  " + bullet + " ")

    def usage(module: String = null): String = {
      if (module != null) checkModule(module)
      val usage: String = {
        val set = if (module == null) modules.keySet else Set(module)
        val u = for (m <- set) yield toBulletedList(modules(m), m)
        u.mkString("\n\n")
      }
      """
        |Usage   : java -jar BiopetFramework.jar {%s} <name> [args]
        |Version : %s
        |
        |%s
        |
        |Subcommands:
        |  - version
        |
        |Questions or comments? Email sasc@lumc.nl or check out the project page at https://git.lumc.nl/biopet/biopet.git
      """.stripMargin.format(modules.keys.mkString(","), getVersion, usage)
    }

    def checkModule(module: String) {
      if (!modules.contains(module)) {
        System.err.println(s"ERROR: module '$module' does not exist\n" + usage())
        System.exit(1)
      }
    }

    def getCommand(module: String, name: String): MainCommand = {
      checkModule(module)
      val command = modules(module).find(p => p.commandName.toLowerCase == name.toLowerCase)
      if (command == None) {
        System.err.println(s"ERROR: command '$name' does not exist in module '$module'\n" + usage(module))
        System.exit(1)
      }
      return command.get
    }

    args match {
      case Array("version") => {
        println("version: " + getVersion)
      }
      case Array(module, name, passArgs @ _*) => {
        getCommand(module, name).main(passArgs.toArray)
      }
      case Array(module) => {
        System.err.println(usage(module))
        System.exit(1)
      }
      case _ => {
        System.err.println(usage())
        System.exit(1)
      }
    }
  }

  def getVersion = BiopetExecutable.getVersion

  def getCommitHash = BiopetExecutable.getCommitHash

  def checkDirtyBuild(logger: Logger) {
    val prop = new Properties()
    prop.load(getClass.getClassLoader.getResourceAsStream("git.properties"))
    val describeShort = prop.getProperty("git.commit.id.describe-short")
    if (describeShort.endsWith("-dirty")) {
      logger.warn("**********************************************************")
      logger.warn("* This JAR was built while there are uncommited changes. *")
      logger.warn("* Reproducible results are *not* guaranteed.             *")
      logger.warn("**********************************************************")
    }
  }
  checkDirtyBuild(logger)
}

object BiopetExecutable {
  def getVersion = {
    getClass.getPackage.getImplementationVersion + " (" + getCommitHash + ")"
  }

  def getCommitHash = {
    val prop = new Properties()
    prop.load(getClass.getClassLoader.getResourceAsStream("git.properties"))
    prop.getProperty("git.commit.id.abbrev")
  }
}