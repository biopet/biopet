package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{ Version, Reference, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

import scala.util.matching.Regex

/**
 * Created by pjvan_thof on 3/25/16.
 */
class Snptest(val root: Configurable) extends BiopetCommandLineFunction with Reference with Version {
  @Input(required = true)
  var inputGenotypes: List[File] = Nil

  @Input(required = true)
  var inputSampleFiles: List[File] = Nil

  @Output(required = false)
  var logFile: Option[File] = None

  @Output(required = false)
  var outputFile: Option[File] = None

  @Output(required = false)
  var outputDatabaseFile: Option[File] = None

  var assumeChromosome: Option[String] = config("assume_chromosome")
  var genotypeField: Option[String] = config("genotype_field")
  var genotypeProbabilityScale: Option[String] = config("genotype_probability_scale")
  var haploidGenotypeCoding: Option[String] = config("haploid_genotype_coding")
  var missingCode: Option[String] = config("missing_code")
  var totalProbLimit: Option[Double] = config("total_prob_limit")
  var tableName: Option[String] = config("table_name")
  var useLongColumnNamingScheme: Boolean = config("use_long_column_naming_scheme", default = false)
  var summaryStatsOnly: Boolean = config("summary_stats_only", default = false)

  executable = config("exe", default = "snptest")

  def versionCommand: String = executable + " -help"
  def versionRegex: Regex = "Welcome to SNPTEST (.*)".r

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    require(inputGenotypes.length == inputSampleFiles.length)
  }

  def multiArg(argName: String, values: Iterable[Any]): String = {
    required(argName) + values.map(required(_)).mkString
  }

  def cmdLine: String = {
    val data = inputGenotypes.zip(inputSampleFiles).flatMap(x => List(x._1, x._2))
    required(executable) +
      optional("-assume_chromosome", assumeChromosome) +
      multiArg("-data", data) +
      optional("-genotype_field", genotypeField) +
      optional("-genotype_probability_scale", genotypeProbabilityScale) +
      optional("-haploid_genotype_coding", haploidGenotypeCoding) +
      optional("-missing_code", missingCode) +
      optional("-total_prob_limit", totalProbLimit) +
      optional("-log", logFile) +
      optional("-o", outputFile) +
      optional("-odb", outputDatabaseFile) +
      optional("-table_name", tableName) +
      conditional(useLongColumnNamingScheme, "-use_long_column_naming_scheme") +
      conditional(summaryStatsOnly, "-summary_stats_only")
  }

}
