package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.extensions.gatk.CommandLineGATK.isFileWithTag
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

class ContEst(val parent: Configurable) extends CommandLineGATK {

  def analysis_type: String = "ContEst"

  /** Variant file containing information about the population allele frequencies. */
  @Input(fullName = "popfile", shortName="pf", required = true)
  var popFile: File = config("popfile")

  /** Output file of the program. */
  @Output(fullName = "out", shortName = "o", required = true)
  var output: File = _

  /** Where to write a full report about the loci we processed. */
  @Output(fullName = "base_report", shortName = "br", required = false)
  var baseReportFile: Option[File] = config("base_report")

  @Argument(fullName = "beta_threshold", required = false)
  var betaThreshold: Option[Double] = config("beta_threshold")

  @Argument(fullName = "genotype_mode", shortName= "gm", required = false)
  var genotypeMode: Option[String] = config("genotype_mode")

  @Argument(fullName = "lane_level_contamination", shortName= "llc", required = false)
  var laneLevelContamination: String = "SAMPLE"

  @Output(fullName = "likelihood_file", shortName = "lf", required = false)
  var likelihoodFile: Option[File] = config("likelihood_file")

  /** Threshold for minimum mapping quality score.
    * Default value: 20 */
  @Argument(fullName = "min_mapq", required = false)
  var minMapQ: Option[Int] = config("min_mapq")

  /** Threshold for minimum base quality score.
    * Default value: 20 */
  @Argument(fullName = "min_qscore", required = false)
  var minQScore: Option[Int] = config("min_qscore")

  @Argument(fullName = "minimum_base_count", shortName = "mbc", required = false)
  var minimumBaseCount: Option[Int] = config("minimum_base_count")

  /** Evaluate contamination for just a single contamination population.
    * Default value: CEU */
  @Argument(fullName = "population", shortName = "population", required = false)
  var population: Option[String] = config("population")

  @Argument(fullName = "precision", shortName = "pc", required = false)
  var precision: Option[Double] = config("precision")

  @Argument(fullName = "trim_fraction", required = false)
  var trimFraction: Option[Double] = config("trim_fraction")

  override def cmdLine = super.cmdLine +
    required("--popfile", popFile) +
    required("--out", output) +
    optional("--base_report", baseReportFile) +
    optional("--beta_threshold", betaThreshold) +
    optional("--genotype_mode", genotypeMode) +
    optional("--lane_level_contamination", laneLevelContamination) +
    optional("--likelihood_file", likelihoodFile) +
    optional("--min_mapq", minMapQ) +
    optional("--min_qscore", minQScore) +
    optional("--minimum_base_count", minimumBaseCount) +
    optional("--population", population) +
    optional("--precision", precision) +
    optional("--trim_fraction", trimFraction)

}
