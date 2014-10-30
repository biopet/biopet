package nl.lumc.sasc.biopet.tools

import java.io.File
import java.io.PrintWriter
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import scala.io.Source
import scala.collection.mutable.Map
import scala.collection.SortedMap

class SageCreateTagCounts(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Raw count file", shortName = "input", required = true)
  var input: File = _

  @Input(doc = "tag library", shortName = "taglib", required = true)
  var tagLib: File = _

  @Output(doc = "Sense count file", shortName = "sense", required = true)
  var countSense: File = _

  @Output(doc = "Sense all coun filet", shortName = "allsense", required = true)
  var countAllSense: File = _

  @Output(doc = "AntiSense count file", shortName = "antisense", required = true)
  var countAntiSense: File = _

  @Output(doc = "AntiSense all count file", shortName = "allantisense", required = true)
  var countAllAntiSense: File = _

  override val defaultVmem = "8G"
  memoryLimit = Option(4.0)

  override def commandLine = super.commandLine +
    required("-I", input) +
    required("--taglib", tagLib) +
    optional("--sense", countSense) +
    optional("--allsense", countAllSense) +
    optional("--antisense", countAntiSense) +
    optional("--allantisense", countAllAntiSense)
}

object SageCreateTagCounts extends ToolCommand {
  case class Args(input: File = null, tagLib: File = null, countSense: File = null, countAllSense: File = null,
                  countAntiSense: File = null, countAllAntiSense: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "input") required () unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('t', "tagLib") required () unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(tagLib = x)
    }
    opt[File]("countSense") unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(countSense = x)
    }
    opt[File]("countAllSense") unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(countAllSense = x)
    }
    opt[File]("countAntiSense") unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(countAntiSense = x)
    }
    opt[File]("countAllAntiSense") unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(countAllAntiSense = x)
    }
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    if (!commandArgs.input.exists) throw new IllegalStateException("Input file not found, file: " + commandArgs.input)

    val rawCounts: Map[String, Long] = Map()
    for (line <- Source.fromFile(commandArgs.input).getLines) {
      val values = line.split("\t")
      val gene = values(0)
      val count = values(1).toLong
      if (rawCounts.contains(gene)) rawCounts(gene) += count
      else rawCounts += gene -> count
    }

    val senseCounts: Map[String, Long] = Map()
    val allSenseCounts: Map[String, Long] = Map()
    val antiSenseCounts: Map[String, Long] = Map()
    val allAntiSenseCounts: Map[String, Long] = Map()

    for (line <- Source.fromFile(commandArgs.tagLib).getLines if !line.startsWith("#")) {
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

    def writeFile(file: File, counts: Map[String, Long]) {
      val sorted: SortedMap[String, Long] = SortedMap(counts.toArray: _*)
      if (file != null) {
        val writer = new PrintWriter(file)
        for ((gene, count) <- sorted) {
          if (count > 0) writer.println(gene + "\t" + count)
        }
        writer.close
      }
    }
    writeFile(commandArgs.countSense, senseCounts)
    writeFile(commandArgs.countAllSense, allSenseCounts)
    writeFile(commandArgs.countAntiSense, antiSenseCounts)
    writeFile(commandArgs.countAllAntiSense, allAntiSenseCounts)
  }
}