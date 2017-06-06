package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

class ContEst(val parent: Configurable) extends CommandLineGATK {

  def analysis_type: String = "ContEst"

  /** Bam file for the tumor sample. */
  @Input(fullName = "tumor_bam", required = true)
  var tumorSampleBam: File = _

  /** Bam file for the normal sample. */
  @Input(fullName = "normal_bam", required = true)
  var normalSampleBam: File = _

  /** Variant file containing information about the population allele frequencies. */
  @Input(fullName = "popfile", shortName="pf", required = true)
  var popFile: File = config("popfile")

  /** Output file of the program. */
  @Output(fullName = "out", shortName = "o", required = true)
  var outputVcf: File = _

  /** Where to write a full report about the loci we processed. */
  @Output(fullName = "base_report", shortName = "br", required = false)
  var baseReportFile: Option[File] = _

  @Output(fullName = "likelihood_file", shortName = "lf", required = false)
  var likelihoodFile: Option[File] = _

  @Argument(fullName = "beta_threshold", required = false)
  var betaThreshold: Option[Double] = _

  @Argument(fullName = "genotype_mode", shortName= "gm", required = false)
  var genotypeMode: Option[String] = _

  @Argument(fullName = "lane_level_contamination", shortName= "llc", required = false)
  var laneLevelContamination: String = "SAMPLE"

  /** Threshold for minimum mapping quality score.
    * Default value: 20 */
  @Argument(fullName = "min_mapq", required = false)
  var minMapQ: Option[Int] = config("min_mapq")

  /** Threshold for minimum base quality score.
    * Default value: 20 */
  @Argument(fullName = "min_qscore", required = false)
  var minQScore: Option[Int] = config("min_qscore")

  /** Default value: 500 */
  @Argument(fullName = "minimum_base_count", shortName = "mbc", required = false)
  var minimumBaseCount: Option[Int] = config("minimum_base_count")

  /** Evaluate contamination for just a single contamination population.
    * Default value: CEU */
  @Argument(fullName = "population", shortName = "population", required = false)
  var population: Option[String] = config("population")

  /** Default value: 0.1 */
  @Argument(fullName = "precision", shortName = "pc", required = false)
  var precision: Option[Double] = config("precision")

/*--sample_name
    -sn 	unknown 	The sample name; used to extract the correct genotypes from mutli-sample truth vcfs
  --trim_fraction
    0.01 	at most, what fraction of sites should be trimmed based on BETA_THRESHOLD    */
}