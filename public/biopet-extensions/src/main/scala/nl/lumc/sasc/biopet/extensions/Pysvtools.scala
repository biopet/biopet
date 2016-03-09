package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline._

/**
 * Created by wyleung on 8-1-16.
 */
class Pysvtools(val root: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "Input file", required = true)
  var input: List[File] = Nil

  @Argument(doc = "Set flanking amount")
  var flanking: Option[Int] = config("flanking")

  var exclusionRegions: List[File] = config("exclusion_regions")
  var translocationsOnly: Boolean = config("translocations_only", default = false)

  @Output(doc = "Unzipped file", required = true)
  var output: File = _

  var tsvoutput: File = _
  var bedoutput: File = _
  var regionsoutput: File = _

  executable = config("exe", default = "vcf_merge_sv_events")

  def versionRegex = """PySVtools (.*)""".r
  def versionCommand = executable + " --version"
  override def defaultThreads = 2

  override def beforeGraph(): Unit = {
    // TODO: we might want to validate the VCF before we start to tool? or is this a responsibility of the tool itself?
    if (input.isEmpty) {
      Logging.addError("No input VCF is given")
    }

    // redefine the tsv, bed and regions output
    val outputNamePrefix = output.getAbsolutePath.stripSuffix(".vcf")
    tsvoutput = new File(outputNamePrefix + ".tsv")
    bedoutput = new File(outputNamePrefix + ".bed")
    regionsoutput = new File(outputNamePrefix + ".regions.bed")
  }

  /** return commandline to execute */
  def cmdLine = required(executable) +
    repeat("-c", input) +
    optional("-f", flanking) +
    "-i " + repeat(input) +
    "-o " + required(tsvoutput) +
    "-b " + required(bedoutput) +
    "-v " + required(output) +
    "-r " + required(regionsoutput)
}