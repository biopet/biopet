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

  @Argument(doc = "drop individual genotypes")
  var dropGenotype: Boolean = false

  @Argument(doc = "header only")
  var headerOnly: Boolean = false

  @Argument(doc = "Compression level")
  var compressionLevel: Int = 9

  @Argument(doc = "output type")
  var outputType: String = "z"

  @Argument(doc = "regions")
  var regions: String = _

  @Argument(doc = "region file")
  var regionFile: File = _

  @Argument(doc = "targets")
  var targets: String = _

  @Argument(doc = "targets file")
  var targetFile: File = _

  @Argument(doc = "trim alt alleles")
  var trimAltAlleles: Boolean = false

  @Argument(doc = "no update")
  var noUpdate: Boolean = false

  @Argument(doc = "samples")
  var samples: List[String] = Nil

  @Argument(doc = "samples file")
  var sampleFile: File = _

  @Argument(doc = "minimum allele count")
  var minAC: Int = _

  @Argument(doc = "max allele count")
  var maxAC: Int = _

  @Argument(doc = "exclude (expression)")
  var exclude: String = _

  @Argument(doc = "apply filters")
  var applyFilters: List[String] = Nil

  @Argument(doc = "genotype")
  var genotype: String = _

  @Argument(doc = "include (expression)")
  var include: String = _

  @Argument(doc = "Known (ID field is not .) only")
  var known: Boolean = false

  @Argument(doc = "min alleles")
  var minAlleles: Int = _

  @Argument(doc = "max alleles")
  var maxAlleles: Int = _

  @Argument(doc = "novel (ID field is .) only")
  var novel: Boolean = false

  @Argument(doc = "phased only")
  var phased: Boolean = false

  @Argument(doc = "exclude phased (only)")
  var excludePhased: Boolean = false

  @Argument(doc = "min allele frequency")
  var minAF: Int = _

  @Argument(doc = "max allele frequency")
  var maxAF: Int = _

  @Argument(doc = "uncalled only")
  var uncalled: Boolean = false

  @Argument(doc = "exclude uncalled (only)")
  var excludeUncalled: Boolean = false

  @Argument(doc = "types")
  var types: String = _

  @Argument(doc = "exclude types")
  var excludeTypes: String = _

  @Argument(doc = "private (requires samples)")
  var onlyPrivate: Boolean = false

  @Argument(doc = "Exclude privates")
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
    executable + conditional(dropGenotype, "-G") + conditional(headerOnly, "-h") +
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

}
