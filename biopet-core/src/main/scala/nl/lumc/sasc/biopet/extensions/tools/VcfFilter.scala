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
package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

class VcfFilter(val parent: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.VcfFilter

  @Input(doc = "Input vcf", shortName = "I", required = true)
  var inputVcf: File = _

  @Output(doc = "Output vcf", shortName = "o", required = false)
  var outputVcf: File = _

  @Output
  var outputVcfIndex: File = _

  var minSampleDepth: Option[Int] = config("min_sample_depth")
  var minTotalDepth: Option[Int] = config("min_total_depth")
  var minAlternateDepth: Option[Int] = config("min_alternate_depth")
  var minSamplesPass: Option[Int] = config("min_samples_pass")
  var minGenomeQuality: Option[Int] = config("min_genome_quality")
  var filterRefCalls: Boolean = config("filter_ref_calls", default = false)
  var invertedOutputVcf: Option[File] = None
  var resToDom: Option[String] = None
  var trioCompound: Option[String] = None
  var deNovoInSample: Option[String] = None
  var deNovoTrio: Option[String] = None
  var trioLossOfHet: Option[String] = None
  var mustHaveVariant: List[String] = Nil
  var calledIn: List[String] = Nil
  var mustHaveGenotype: List[String] = Nil
  var diffGenotype: List[String] = Nil
  var filterHetVarToHomVar: List[String] = Nil
  var minQualScore: Option[Double] = None
  var id: List[String] = Nil
  var idFile: Option[File] = None

  override def defaultCoreMemory = 3.0

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (outputVcf.getName.endsWith("vcf.gz"))
      outputVcfIndex = new File(outputVcf.getAbsolutePath + ".tbi")
  }

  override def cmdLine: String =
    super.cmdLine +
      required("-I", inputVcf) +
      required("-o", outputVcf) +
      optional("--minSampleDepth", minSampleDepth) +
      optional("--minTotalDepth", minTotalDepth) +
      optional("--minAlternateDepth", minAlternateDepth) +
      optional("--minSamplesPass", minSamplesPass) +
      optional("--minGenomeQuality", minGenomeQuality) +
      conditional(filterRefCalls, "--filterRefCalls") +
      optional("--invertedOutputVcf", invertedOutputVcf) +
      optional("--resToDom", resToDom) +
      optional("--trioCompound", trioCompound) +
      optional("--deNovoInSample", deNovoInSample) +
      optional("--deNovoTrio", deNovoTrio) +
      optional("--trioLossOfHet", trioLossOfHet) +
      repeat("--mustHaveVariant", mustHaveVariant) +
      repeat("--calledIn", calledIn) +
      repeat("--mustHaveGenotype", mustHaveGenotype) +
      repeat("--diffGenotype", diffGenotype) +
      repeat("--filterHetVarToHomVar", filterHetVarToHomVar) +
      optional("--minQualScore", minQualScore) +
      repeat("--id", id) +
      optional("--idFile", idFile)
}
