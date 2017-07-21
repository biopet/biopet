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
package nl.lumc.sasc.biopet.extensions.varscan

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

class VarscanMpileup2cns(val parent: Configurable) extends Varscan {

  @Input(doc = "Input mpileup file", required = false) // if not defined, input is stdin
  var input: Option[File] = None

  @Output(doc = "Output file", required = false) // if not defined, output is stdout
  var output: Option[File] = None

  var minCoverage: Option[Int] = config("min_coverage")
  var minReads2: Option[Int] = config("min_reads_2")
  var minAvgQual: Option[Int] = config("min_avg_qual")
  var minVarFreq: Option[Double] = config("min_var_freq")
  var minFreqForHom: Option[Double] = config("min_freq_for_hom")
  var pValue: Option[Double] = config("p_value")
  var strandFilter: Option[Int] = config("strand_filter")
  var outputVcf: Option[Int] = config("output_vcf")
  var vcfSampleList: Option[File] = config("vcf_sample_list")
  var variants: Option[Int] = config("variants")

  override def defaultCoreMemory = 8.0

  override def beforeGraph(): Unit = {
    val validValues: Set[Int] = Set(0, 1)
    // check for boolean vars that are passed as ints
    strandFilter.foreach(v =>
      require(validValues.contains(v), "strand_filter value must be either 0 or 1"))
    outputVcf.foreach(v =>
      require(validValues.contains(v), "output_vcf value must be either 0 or 1"))
    variants.foreach(v => require(validValues.contains(v), "variants value must be either 0 or 1"))
  }

  override def cmdLine: String =
    super.cmdLine + required("mpileup2cns") +
      required(input) +
      optional("--min-coverage", minCoverage) +
      optional("--min-reads2", minReads2) +
      optional("--min-avg-qual", minAvgQual) +
      optional("--min-var-freq", minVarFreq) +
      optional("--min-freq-for-hom", minFreqForHom) +
      optional("--p-value", pValue) +
      optional("--strand-filter", strandFilter) +
      optional("--output-vcf", outputVcf) +
      optional("--vcf-sample-list", vcfSampleList) +
      optional("--variants", variants) +
      (if (outputAsStdout) "" else " > " + required(output))
}
