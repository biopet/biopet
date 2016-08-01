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

  var minMq: Option[Int] = config("min_mq", default = 20) // minimum of MQ to consider for taking read into histogram
  var useMq: Boolean = config("use_mq", default = false)
  var minInsertsize: Option[Int] = config("min_insertsize")
  var solidData: Boolean = config("solid", default = false)
  var sdCutoff: Option[Int] = config("sd_cutoff") // Cutoff in unit of standard deviation [4]

  // we set this to a higher number to avoid biases in small numbers in sorted bams
  var minObservations: Option[Int] = config("min_observations") //  Number of observation required to estimate mean and s.d. insert size [10_000]
  var coefvarCutoff: Option[Int] = config("coef_cutoff") // Cutoff on coefficients of variation [1]
  var histogramBins: Option[Int] = config("histogram_bins") // Number of bins in the histogram [50]

  def cmdLine = required(executable) +
    optional("-q", minMq) +
    conditional(useMq, "-m") +
    optional("-s", minInsertsize) +
    conditional(solidData, "-s") +
    optional("-c", sdCutoff) +
    optional("-n", minObservations) +
    optional("-v", coefvarCutoff) +
    optional("-b", histogramBins) +
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
