/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * Author: Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.core.apps

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable

class WipeReads(val root: Configurable) extends BiopetJavaCommandLineFunction {

  javaMainClass = getClass.getName

  @Input(doc = "Input BAM file (must be indexed)", shortName = "I", required = true)
  var inputBAM: File = _

  @Output(doc = "Output BAM", shortName = "o", required = true)
  var outputBAM: File = _


}

object WipeReads {

  type OptionMap = Map[String, Any]

  def main(args: Array[String]): Unit = {

    // simple command line parser. adapted from @pjotrp's answer at
    // http://stackoverflow.com/questions/2315912/scala-best-way-to-parse-command-line-parameters-cli
    if (args.length == 0) {
      println(usage)
      System.exit(1)
    }
    val argList = args.toList

    def nextOption(map: OptionMap, list: List[String]): OptionMap =
      list match {
        case ("--inputBAM" | "-I") :: value :: tail => nextOption(map ++ Map("inputBAM" -> new File(value)), tail)
        // TODO: add other flags
        case option :: tail => map
      }

    val options = nextOption(Map(), argList)

    val inputBAM = options.get("inputBAM") match {
      case Some(value)  => value
      // TODO: make exception object more specific
      case None         => throw new Exception("Required input flag '--inputBAM / -I' not found")
    }
  }

  val usage: String = """
                        |usage: java -cp BiopetFramework.jar nl.lumc.sasc.biopet-core.apps.WipeReads [options] -I input -r regions -o output
                        |
                        |WipeReads - Tool for reads removal from an indexed BAM file.
                        |
                        |positional arguments:
                        |  -I,--inputBAM              input BAM file, must be indexed
                        |  -r,--regions               input BED file
                        |  -o,--outputBAM             output BAM file
                        |
                        |optional arguments:
                        |  -f,--fraction_overlap      Minimum overlap of reads and target regions
                        |
                        |This tool will remove BAM records that overlaps a set of given regions.
                        |By default, if the removed reads are also mapped to other regions outside
                        |the given ones, they will also be removed.
                      """.stripMargin


}
