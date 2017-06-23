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
package nl.lumc.sasc.biopet.extensions.bcftools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/**
  * Created by ahbbollen on 12-10-15.
  */
class BcftoolsView(val parent: Configurable) extends Bcftools {

  @Input(doc = "Input VCF file")
  var input: File = _

  @Output(doc = "Output file")
  var output: File = _

  @Argument(doc = "drop individual genotypes", required = false)
  var dropGenotype: Boolean = config("drop_genotype", default = false)

  @Argument(doc = "header only", required = false)
  var headerOnly: Boolean = config("header_only", false)

  @Argument(doc = "Compression level", required = false)
  var compressionLevel: Int = config("compression_level", default = 9)

  @Argument(doc = "output type", required = false)
  var outputType: String = "z"

  @Argument(doc = "regions", required = false)
  var regions: Option[String] = config("r")

  @Argument(doc = "region file", required = false)
  var regionFile: Option[File] = config("R")

  @Argument(doc = "targets", required = false)
  var targets: Option[String] = config("t")

  @Argument(doc = "targets file", required = false)
  var targetFile: Option[File] = config("T")

  @Argument(doc = "trim alt alleles", required = false)
  var trimAltAlleles: Boolean = config("trim_alt_allele", default = false)

  @Argument(doc = "no update", required = false)
  var noUpdate: Boolean = config("no_update", default = false)

  @Argument(doc = "samples", required = false)
  var samples: List[String] = config("s", default = Nil)

  @Argument(doc = "samples file", required = false)
  var sampleFile: Option[File] = config("S")

  @Argument(doc = "minimum allele count", required = false)
  var minAC: Option[Int] = config("c")

  @Argument(doc = "max allele count", required = false)
  var maxAC: Option[Int] = config("C")

  @Argument(doc = "exclude (expression)", required = false)
  var exclude: Option[String] = config("e")

  @Argument(doc = "apply filters", required = false)
  var applyFilters: List[String] = config("F", default = Nil)

  @Argument(doc = "genotype", required = false)
  var genotype: Option[String] = config("g")

  @Argument(doc = "include (expression)", required = false)
  var include: Option[String] = config("i")

  @Argument(doc = "Known (ID field is not .) only", required = false)
  var known: Boolean = config("k", default = false)

  @Argument(doc = "min alleles", required = false)
  var minAlleles: Option[Int] = config("m")

  @Argument(doc = "max alleles", required = false)
  var maxAlleles: Option[Int] = config("M")

  @Argument(doc = "novel (ID field is .) only", required = false)
  var novel: Boolean = config("n", false)

  @Argument(doc = "phased only", required = false)
  var phased: Boolean = config("p", false)

  @Argument(doc = "exclude phased (only)", required = false)
  var excludePhased: Boolean = config("P", false)

  @Argument(doc = "min allele frequency", required = false)
  var minAF: Option[Int] = config("q")

  @Argument(doc = "max allele frequency", required = false)
  var maxAF: Option[Int] = config("Q")

  @Argument(doc = "uncalled only", required = false)
  var uncalled: Boolean = config("u", default = false)

  @Argument(doc = "exclude uncalled (only)", required = false)
  var excludeUncalled: Boolean = config("U", default = false)

  @Argument(doc = "types", required = false)
  var types: Option[String] = config("v")

  @Argument(doc = "exclude types", required = false)
  var excludeTypes: Option[String] = config("V")

  @Argument(doc = "private (requires samples)", required = false)
  var onlyPrivate: Boolean = config("x", default = false)

  @Argument(doc = "Exclude privates", required = false)
  var excludePrivate: Boolean = config("X", default = false)

  override def beforeGraph() = {
    super.beforeGraph()

    require((compressionLevel <= 9) && (compressionLevel >= 0))
    require(
      (outputType.length == 1) &&
        (outputType == "z" || outputType == "b" || outputType == "u" || outputType == "v")
    )
  }

  def baseCmd = {
    executable +
      required("view") +
      conditional(dropGenotype, "-G") +
      conditional(headerOnly, "-h") +
      required("-l", compressionLevel) +
      required("-O", outputType) +
      optional("-r", regions) +
      optional("-R", regionFile) +
      optional("-t", targets) +
      optional("-T", targetFile) +
      conditional(trimAltAlleles, "-a") +
      conditional(noUpdate, "-I") +
      repeat("-s", samples) +
      optional("-S", sampleFile) +
      optional("-c", minAC) +
      optional("-C", maxAC) +
      optional("-e", exclude) +
      optional("-f", applyFilters) +
      optional("-g", genotype) +
      optional("-i", include) +
      conditional(known, "-k") +
      optional("-m", minAlleles) +
      optional("-M", maxAlleles) +
      conditional(novel, "-n") +
      conditional(phased, "-p") +
      conditional(excludePhased, "-P") +
      optional("-q", minAF) +
      optional("-Q", maxAF) +
      conditional(uncalled, "-u") +
      conditional(excludeUncalled, "-U") +
      optional("-v", types) +
      optional("-V", excludeTypes) +
      conditional(onlyPrivate, "-x") +
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
