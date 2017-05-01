/**
  * Biopet is built on top of GATK Queue for building bioinformatic
  * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
  * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
  * should also be able to execute Biopet tools and pipelines.
  *
  * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
  *
  * Contact us at: sasc@lumc.nl
  *
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.utils

import java.io.{PrintWriter, StringWriter}

import nl.lumc.sasc.biopet.{FullVersion, LastCommitHash}
import org.apache.log4j.Logger

import scala.io.Source

/**
  * This is the main trait for the biopet executable
  */
trait BiopetExecutable extends Logging {

  def pipelines: List[MainCommand]

  def tools: List[MainCommand]

  def templates: List[MainCommand]

  val modules: Map[String, List[MainCommand]] =
    Map("pipeline" -> pipelines, "tool" -> tools, "template" -> templates)

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    checkDirtyBuild(logger)

    def toBulletedList(m: List[MainCommand], kind: String = "", bullet: String = "-") =
      "Available %s(s):\n  ".format(kind) + bullet + " " + m
        .map(x => x.commandName)
        .sorted
        .mkString("\n  " + bullet + " ")

    def usage(module: String = null): String = {
      if (module != null) checkModule(module)
      val usage: String = {
        val set = if (module == null) modules.keySet else Set(module)
        val u = for (m <- set) yield toBulletedList(modules(m), m)
        u.mkString("\n\n")
      }
      """
        |Usage   : java -jar <path/to/biopet.jar> {%s} <name> [args]
        |Version : %s
        |
        |%s
        |
        |Subcommands:
        |  - version
        |  - license
        |
        |Questions or comments? Email sasc@lumc.nl or check out the project page at https://git.lumc.nl/biopet/biopet.git
      """.stripMargin.format(modules.keys.mkString(","), FullVersion, usage)
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
      if (command.isEmpty) {
        System.err.println(
          s"ERROR: command '$name' does not exist in module '$module'\n" + usage(module))
        System.exit(1)
      }
      command.get
    }

    args match {
      case Array("version") =>
        println("version: " + FullVersion)
      case Array("license") =>
        println(BiopetExecutable.getLicense)
      case Array(module, name, passArgs @ _ *) =>
        try {
          getCommand(module, name).main(passArgs.toArray)
        } catch {
          case e: Exception =>
            val sWriter = new StringWriter()
            val pWriter = new PrintWriter(sWriter)
            e.printStackTrace(pWriter)
            pWriter.close()
            val trace = sWriter.toString.split("\n")

            if (!logger.isDebugEnabled) {
              trace.filterNot(_.startsWith("\tat")).foreach(logger.error(_))
              logger.error("For more info please run with -l debug")
            } else {
              trace.foreach(logger.debug(_))
            }
            sys.exit(1)
        }
      case Array(module) =>
        System.err.println(usage(module))
        sys.exit(1)
      case _ =>
        System.err.println(usage())
        sys.exit(1)
    }
  }

  /** This function checks if current build is based on a dirty repository (uncommitted changes) */
  def checkDirtyBuild(logger: Logger): Unit =
    if (LastCommitHash.endsWith("-dirty")) {
      logger.warn("***********************************************************")
      logger.warn("* This JAR was built while there were uncommited changes. *")
      logger.warn("* Reproducible results are *not* guaranteed.              *")
      logger.warn("***********************************************************")
    }
}

object BiopetExecutable {
  def getLicense: String = {
    val stream = getClass.getClassLoader.getResourceAsStream("nl/lumc/sasc/biopet/License.txt")
    Source.fromInputStream(stream).getLines().mkString("\n", "\n", "\n")
  }
}
