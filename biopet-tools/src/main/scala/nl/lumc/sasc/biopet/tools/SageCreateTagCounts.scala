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

import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.collection.{SortedMap, mutable}
import scala.io.Source

object SageCreateTagCounts extends ToolCommand {
  case class Args(input: File = null,
                  tagLib: File = null,
                  countSense: File = null,
                  countAllSense: File = null,
                  countAntiSense: File = null,
                  countAllAntiSense: File = null)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "input") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('t', "tagLib") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(tagLib = x)
    }
    opt[File]("countSense") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(countSense = x)
    }
    opt[File]("countAllSense") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(countAllSense = x)
    }
    opt[File]("countAntiSense") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(countAntiSense = x)
    }
    opt[File]("countAllAntiSense") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(countAllAntiSense = x)
    }
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    if (!commandArgs.input.exists)
      throw new IllegalStateException("Input file not found, file: " + commandArgs.input)

    val rawCounts: mutable.Map[String, Long] = mutable.Map()
    for (line <- Source.fromFile(commandArgs.input).getLines()) {
      val values = line.split("\t")
      val gene = values(0)
      val count = values(1).toLong
      if (rawCounts.contains(gene)) rawCounts(gene) += count
      else rawCounts += gene -> count
    }

    val senseCounts: mutable.Map[String, Long] = mutable.Map()
    val allSenseCounts: mutable.Map[String, Long] = mutable.Map()
    val antiSenseCounts: mutable.Map[String, Long] = mutable.Map()
    val allAntiSenseCounts: mutable.Map[String, Long] = mutable.Map()

    for (line <- Source.fromFile(commandArgs.tagLib).getLines() if !line.startsWith("#")) {
      val values = line.split("\t")
      val tag = values(0)
      val sense = values(1)
      val allSense = values(2)
      val antiSense = if (values.size > 3) values(3) else ""
      val allAntiSense = if (values.size > 4) values(4) else ""

      if (!sense.isEmpty && !sense.contains(",")) {
        val count = if (rawCounts.contains(tag)) rawCounts(tag) else 0
        if (senseCounts.contains(sense)) senseCounts(sense) += count
        else senseCounts += sense -> count
      }

      if (!allSense.isEmpty && !allSense.contains(",")) {
        val count = if (rawCounts.contains(tag)) rawCounts(tag) else 0
        if (allSenseCounts.contains(allSense)) allSenseCounts(allSense) += count
        else allSenseCounts += allSense -> count
      }

      if (!antiSense.isEmpty && !antiSense.contains(",")) {
        val count = if (rawCounts.contains(tag)) rawCounts(tag) else 0
        if (antiSenseCounts.contains(antiSense)) antiSenseCounts(antiSense) += count
        else antiSenseCounts += antiSense -> count
      }

      if (!allAntiSense.isEmpty && !allAntiSense.contains(",")) {
        val count = if (rawCounts.contains(tag)) rawCounts(tag) else 0
        if (allAntiSenseCounts.contains(allAntiSense)) allAntiSenseCounts(allAntiSense) += count
        else allAntiSenseCounts += allAntiSense -> count
      }
    }

    def writeFile(file: File, counts: mutable.Map[String, Long]) {
      val sorted: SortedMap[String, Long] = SortedMap(counts.toArray: _*)
      if (file != null) {
        val writer = new PrintWriter(file)
        for ((gene, count) <- sorted) {
          if (count > 0) writer.println(gene + "\t" + count)
        }
        writer.close()
      }
    }

    if (commandArgs.countSense != null) {
      writeFile(commandArgs.countSense, senseCounts)
    }
    if (commandArgs.countAllAntiSense != null) {
      writeFile(commandArgs.countAllAntiSense, allAntiSenseCounts)
    }
    if (commandArgs.countAllSense != null) {
      writeFile(commandArgs.countAllSense, allSenseCounts)
    }
    if (commandArgs.countAntiSense != null) {
      writeFile(commandArgs.countAntiSense, antiSenseCounts)
    }
  }
}
