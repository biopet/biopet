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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions.breakdancer

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class BreakdancerConfig(val root: Configurable) extends BiopetCommandLineFunction {
  executable = config("exe", default = "bam2cfg.pl", freeVar = false)

  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "Output File")
  var output: File = _

  var min_mq: Option[Int] = config("min_mq", default = 20) // minimum of MQ to consider for taking read into histogram
  var use_mq: Boolean = config("use_mq", default = false)
  var min_insertsize: Option[Int] = config("min_insertsize")
  var solid_data: Boolean = config("solid", default = false)
  var sd_cutoff: Option[Int] = config("sd_cutoff") // Cutoff in unit of standard deviation [4]

  // we set this to a higher number to avoid biases in small numbers in sorted bams
  var min_observations: Option[Int] = config("min_observations") //  Number of observation required to estimate mean and s.d. insert size [10_000]
  var coefvar_cutoff: Option[Int] = config("coef_cutoff") // Cutoff on coefficients of variation [1]
  var histogram_bins: Option[Int] = config("histogram_bins") // Number of bins in the histogram [50]

  def cmdLine = required(executable) +
    optional("-q", min_mq) +
    conditional(use_mq, "-m") +
    optional("-s", min_insertsize) +
    conditional(solid_data, "-s") +
    optional("-c", sd_cutoff) +
    optional("-n", min_observations) +
    optional("-v", coefvar_cutoff) +
    optional("-b", histogram_bins) +
    required(input) + " 1> " + required(output)
}

object BreakdancerConfig {
  def apply(root: Configurable, input: File, output: File): BreakdancerConfig = {
    val bdconf = new BreakdancerConfig(root)
    bdconf.input = input
    bdconf.output = output
    bdconf
  }

  def apply(root: Configurable, input: File, outputDir: String): BreakdancerConfig = {
    val dir = if (outputDir.endsWith("/")) outputDir else outputDir + "/"
    val outputFile = new File(dir + swapExtension(input.getName))
    apply(root, input, outputFile)
  }

  def apply(root: Configurable, input: File): BreakdancerConfig = {
    apply(root, input, new File(swapExtension(input.getAbsolutePath)))
  }

  private def swapExtension(inputFile: String) = inputFile.substring(0, inputFile.lastIndexOf(".bam")) + ".breakdancer.cfg"
}
