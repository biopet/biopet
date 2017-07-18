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
package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}

import scala.io.Source

/**
  * Created by pjvan_thof on 12/18/15.
  */
object MergeOtuMaps extends ToolCommand {
  case class Args(inputFiles: List[File] = Nil,
                  outputFile: File = null,
                  skipPrefix: List[String] = Nil)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('I', "input") minOccurs 2 required () unbounded () valueName "<file>" action {
      (x, c) =>
        c.copy(inputFiles = x :: c.inputFiles)
    }
    opt[File]('o', "output") required () unbounded () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
        c.copy(outputFile = x)
    }
    opt[String]('p', "skipPrefix") unbounded () valueName "<text>" action { (x, c) =>
      c.copy(skipPrefix = x :: c.skipPrefix)
    }
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    var map: Map[String, String] = Map()

    for (inputFile <- cmdArgs.inputFiles) {
      logger.info(s"Start reading $inputFile")
      val reader = Source.fromFile(inputFile)
      reader.getLines().foreach { line =>
        val values = line.split("\t", 2)
        val key = values.head
        if (!cmdArgs.skipPrefix.exists(key.startsWith))
          map += key -> (line.stripPrefix(s"$key") + map.getOrElse(key, ""))
      }
      reader.close()
    }

    logger.info(s"Start writing to ${cmdArgs.outputFile}")
    val writer = new PrintWriter(cmdArgs.outputFile)
    map.foreach { case (key, list) => writer.println(key + list) }
    writer.close()
  }
}
