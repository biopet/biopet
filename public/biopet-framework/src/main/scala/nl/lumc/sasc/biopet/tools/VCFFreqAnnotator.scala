package nl.lumc.sasc.biopet.tools

import nl.lumc.sasc.biopet.core.ToolCommand
import java.io.File

/**
 * This tool annotates frequencies of variants in the input VCF with information from several sources
 * The path to these sources has to be supplied as a json file containing all paths and names of sources
 * Supported sources: VCF, tabix-indexed tsv, and BigWig directory structure
 * Annotation from Varda variation database is done in a different tool
 * Created by ahbbollen on 12/8/14.
 */
object VCFFreqAnnotator extends ToolCommand {
  def main(args: Array[String]): Unit = {
    val commandArgs: Args = new OptParser()
      .parse(args, Args())
      .getOrElse(sys.exit(1))

    val inputvcf = commandArgs.inputVCF
    val outputvcf = commandArgs.inputVCF
    val sourcesjson = commandArgs.sourcesJSON

    logger.info("Starting VCFFreqAnnotator with following flags:")
    logger.info(s"""Input VCF - $inputvcf""")
    logger.info(s"""Output VCF - $outputvcf""")
    logger.info(s"""Sources JSON - $sourcesjson""")

  }

  case class Args(inputVCF: File = null,
                  outputVCF: File = null,
                  sourcesJSON: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    head(s"""$commandName - Annotate input VCF with frequency information from various sources""")

    opt[File]('I', "InputFile") required () valueName "<vcf>" action { (x, c) =>
      c.copy(inputVCF = x)
    } validate {
      x => if (x.exists) success else failure("Input VCF not found")
    } text "Input VCF file"

    opt[File]('j', "json") required () valueName "<json>" action { (x, c) =>
      c.copy(sourcesJSON = x)
    } validate {
      x => if (x.exists) success else failure("Sources JSON not found")
    } text "Sources JSON"

    opt[File]('O', "OutputFile") required () valueName "<vcf>" action { (x, c) =>
      c.copy(outputVCF = x)
    } validate {
      x => if (x.exists) success else success
    } text "Output VCF"
  }

}
