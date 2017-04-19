package nl.lumc.sasc.biopet.tools.vcfstats

import java.io.File

import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.{ ConfigUtils, ToolCommand }

import scala.collection.JavaConversions._

object VcfStatsForSv extends ToolCommand {
  /** Commandline arguments */
  case class Args(inputFile: File = null, outputFile: File = null, histBinBoundaries: Array[Int] = Array()) extends AbstractArgs

  /** Parsing commandline arguments */
  class OptParser extends AbstractOptParser {
    opt[File]('i', "inputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputFile = x)
    } validate {
      x => if (x.exists) success else failure("Input VCF required")
    } text "Input VCF file (required)"

    opt[File]('o', "outputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    } text "Output file (required)"

    opt[Int]("histBinBoundaries") required () unbounded () action { (x, c) =>
      c.copy(histBinBoundaries = c.histBinBoundaries :+ x)
    } text "When counting the records, sv-s are divided to different size classes, this parameter should give the boundaries between these classes."
  }

  protected var cmdArgs: Args = _

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    cmdArgs = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info(s"Parsing file: ${cmdArgs.inputFile}")

    val stats: Map[String, Any] = getVariantCounts(cmdArgs.inputFile, cmdArgs.histBinBoundaries)

    ConfigUtils.mapToYamlFile(stats, cmdArgs.outputFile)

  }

  /** Parses a vcf-file and counts sv-s by type and size. Sv-s are divided to different size classes, the parameter histogramBinBoundaries gives the boundaries between these classes. */
  def getVariantCounts(vcfFile: File, histogramBinBoundaries: Array[Int]): Map[String, Any] = {
    val delCounts, insCounts, dupCounts, invCounts = Array.fill(histogramBinBoundaries.size + 1) { 0 }
    var traCount = 0

    val reader = new VCFFileReader(vcfFile, false)
    for (record <- reader) {
      record.getAttributeAsString("SVTYPE", "") match {
        case "TRA" | "CTX" | "ITX" => traCount += 1
        case svType => {
          val size = record.getEnd - record.getStart
          var i = 0
          while (i < histogramBinBoundaries.size && size > histogramBinBoundaries(i)) i += 1
          svType match {
            case "DEL" => delCounts(i) += 1
            case "INS" => insCounts(i) += 1
            case "DUP" => dupCounts(i) += 1
            case "INV" => invCounts(i) += 1
            case _     => logger.warn(s"Vcf file contains a record of unknown type: file-$vcfFile, type-$svType")
          }
        }
      }
    }
    reader.close()

    Map("DEL" -> delCounts, "INS" -> insCounts, "DUP" -> dupCounts, "INV" -> invCounts, "TRA" -> traCount)
  }

}
