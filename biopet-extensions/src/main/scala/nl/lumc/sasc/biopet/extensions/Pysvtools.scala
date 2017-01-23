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

  var flanking: Option[Int] = config("flanking")

  var exclusionRegions: List[File] = config("exclusion_regions", default = Nil)
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
    repeat("-c", exclusionRegions) +
    optional("-f", flanking) +
    "-i " + repeat(input) +
    "-o " + required(tsvoutput) +
    "-b " + required(bedoutput) +
    "-v " + required(output) +
    "-r " + required(regionsoutput)
}