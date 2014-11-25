package nl.lumc.sasc.biopet.extensions.svcallers

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable

class BreakdancerConfig(val root: Configurable) extends BiopetCommandLineFunction {
  executable = config("exe", default = "bam2cfg.pl", freeVar = false)

  @Input(doc = "Bam File")
  var input: File = _

  @Output(doc = "Output File")
  var output: File = _

  var min_mq: Option[Int] = config("min_mq", default = 20) // minimum of MQ to consider for taking read into histogram
  var use_mq: Boolean = config("use_mq", default = false)
  var min_insertsize: Option[Int] = config("min_insertsize", default = 450)
  var solid_data: Boolean = config("solid", default = false)
  var sd_cutoff: Option[Int] = config("sd_cutoff", default = 4) // Cutoff in unit of standard deviation [4]

  // we set this to a higher number to avoid biases in small numbers in sorted bams
  var min_observations: Option[Int] = config("min_observations", default = 10000) //  Number of observation required to estimate mean and s.d. insert size [10_000]
  var coefvar_cutoff: Option[Int] = config("coef_cutoff", default = 1) // Cutoff on coefficients of variation [1]
  var histogram_bins: Option[Int] = config("histogram_bins", default = 50) // Number of bins in the histogram [50]

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
    return bdconf
  }

  def apply(root: Configurable, input: File, outputDir: String): BreakdancerConfig = {
    val dir = if (outputDir.endsWith("/")) outputDir else outputDir + "/"
    val outputFile = new File(dir + swapExtension(input.getName))
    return apply(root, input, outputFile)
  }

  def apply(root: Configurable, input: File): BreakdancerConfig = {
    return apply(root, input, new File(swapExtension(input.getAbsolutePath)))
  }

  private def swapExtension(inputFile: String) = inputFile.substring(0, inputFile.lastIndexOf(".bam")) + ".breakdancer.cfg"
}
