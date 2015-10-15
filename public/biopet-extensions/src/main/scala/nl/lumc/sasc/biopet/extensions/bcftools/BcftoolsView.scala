package nl.lumc.sasc.biopet.extensions.bcftools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Created by ahbbollen on 12-10-15.
 */
class BcftoolsView(val root: Configurable) extends Bcftools {

  @Input(doc = "Input VCF file")
  var input: File = _

  @Output(doc = "Output file")
  var output: File = _

  @Argument(doc = "drop individual genotypes", required = false)
  var dropGenotype: Boolean = false

  @Argument(doc = "header only", required = false)
  var headerOnly: Boolean = false

  @Argument(doc = "Compression level", required = false)
  var compressionLevel: Int = 9

  @Argument(doc = "output type", required = false)
  var outputType: String = "z"

  @Argument(doc = "regions", required = false)
  var regions: String = _

  @Argument(doc = "region file", required = false)
  var regionFile: File = _

  @Argument(doc = "targets", required = false)
  var targets: String = _

  @Argument(doc = "targets file", required = false)
  var targetFile: File = _

  @Argument(doc = "trim alt alleles", required = false)
  var trimAltAlleles: Boolean = false

  @Argument(doc = "no update", required = false)
  var noUpdate: Boolean = false

  @Argument(doc = "samples", required = false)
  var samples: List[String] = Nil

  @Argument(doc = "samples file", required = false)
  var sampleFile: File = _

  @Argument(doc = "minimum allele count", required = false)
  var minAC: Option[Int] = _

  @Argument(doc = "max allele count", required = false)
  var maxAC: Option[Int] = _

  @Argument(doc = "exclude (expression)", required = false)
  var exclude: String = _

  @Argument(doc = "apply filters", required = false)
  var applyFilters: List[String] = Nil

  @Argument(doc = "genotype", required = false)
  var genotype: String = _

  @Argument(doc = "include (expression)", required = false)
  var include: String = _

  @Argument(doc = "Known (ID field is not .) only", required = false)
  var known: Boolean = false

  @Argument(doc = "min alleles", required = false)
  var minAlleles: Option[Int] = _

  @Argument(doc = "max alleles", required = false)
  var maxAlleles: Option[Int] = _

  @Argument(doc = "novel (ID field is .) only", required = false)
  var novel: Boolean = false

  @Argument(doc = "phased only", required = false)
  var phased: Boolean = false

  @Argument(doc = "exclude phased (only)", required = false)
  var excludePhased: Boolean = false

  @Argument(doc = "min allele frequency", required = false)
  var minAF: Option[Int] = _

  @Argument(doc = "max allele frequency", required = false)
  var maxAF: Option[Int] = _

  @Argument(doc = "uncalled only", required = false)
  var uncalled: Boolean = false

  @Argument(doc = "exclude uncalled (only)", required = false)
  var excludeUncalled: Boolean = false

  @Argument(doc = "types", required = false)
  var types: String = _

  @Argument(doc = "exclude types", required = false)
  var excludeTypes: String = _

  @Argument(doc = "private (requires samples)", required = false)
  var onlyPrivate: Boolean = false

  @Argument(doc = "Exclude privates", required = false)
  var excludePrivate: Boolean = false

  override def beforeGraph() = {
    super.beforeGraph()

    require((compressionLevel <= 9) && (compressionLevel >= 0))
    require(
      (outputType.length == 1) &&
        (outputType == "z" || outputType == "b" || outputType == "u" || outputType == "v")
    )
  }

  def baseCmd = {
    executable + " view " + conditional(dropGenotype, "-G") + conditional(headerOnly, "-h") +
      required("-l", compressionLevel) + required("-O", outputType) +
      optional("-r", regions) + optional("-R", regionFile) +
      optional("-t", targets) + optional("-T", targetFile) +
      conditional(trimAltAlleles, "-a") + conditional(noUpdate, "-I") +
      repeat("-s", samples) + optional("-S", sampleFile) +
      optional("-c", minAC) + optional("-C", maxAC) +
      optional("-e", exclude) + optional("-f", applyFilters) +
      optional("-g", genotype) + optional("-i", include) +
      conditional(known, "-k") + optional("-m", minAlleles) +
      optional("-M", maxAlleles) + conditional(novel, "-n") +
      conditional(phased, "-p") + conditional(excludePhased, "-P") +
      optional("-q", minAF) + optional("-Q", maxAF) +
      conditional(uncalled, "-u") + conditional(excludeUncalled, "-U") +
      optional("-v", types) + conditional(onlyPrivate, "-x") +
      conditional(excludePrivate, "-X")
  }

  def cmdPipeInput = {
    baseCmd + "-"
  }

  def cmdPipe = {
    baseCmd + required(input)
  }

  def cmdLine = {
    baseCmd + required("-o", output) + required(input)
  }

  /**
   * Convert cmdLine into line without quotes and double spaces
   * primarily for testing
   * @return
   */
  final def cmd = {
    val a = cmdLine
    a.replace("'", "").replace("  ", " ").trim
  }

}
