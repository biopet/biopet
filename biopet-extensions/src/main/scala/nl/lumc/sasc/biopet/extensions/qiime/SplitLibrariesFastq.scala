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
package nl.lumc.sasc.biopet.extensions.qiime

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.util.matching.Regex

/**
  * Created by pjvan_thof on 12/10/15.
  */
class SplitLibrariesFastq(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Version {
  executable = config("exe", default = "split_libraries_fastq.py")

  def versionCommand: String = executable + " --version"
  def versionRegex: Regex = """Version: (.*)""".r

  @Input
  var input: List[File] = Nil
  var outputDir: File = _

  var v: Option[String] = config("v")
  var m: Option[String] = config("m")
  var b: Option[String] = config("b")
  var storeQualScores: Boolean = config("store_qual_scores", default = false)
  var sampleIds: List[String] = Nil
  var storeDemultiplexedFastq: Boolean = config("store_demultiplexed_fastq", default = false)
  var retainUnassignedReads: Boolean = config("retain_unassigned_reads", default = false)
  var r: Option[Int] = config("r")
  var p: Option[Double] = config("p")
  var n: Option[Int] = config("n")
  var s: Option[Int] = config("s")
  var revCompBarcode: Boolean = config("rev_comp_barcode", default = false)
  var revCompMappingBarcodes: Boolean = config("rev_comp_mapping_barcodes", default = false)
  var revComp: Boolean = config("rev_comp", default = false)
  var q: Option[Int] = config("q")
  var lastBadQualityChar: Option[String] = config("last_bad_quality_char")
  var barcodeType: Option[String] = config("barcode_type")
  var maxBarcodeErrors: Option[Double] = config("max_barcode_errors")
  var phredOffset: Option[String] = config("phred_offset")

  def outputSeqs = new File(outputDir, "seqs.fna")

  @Output
  private var _outputSeqs: File = _

  override def defaultCoreMemory = 4.0

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    require(input.nonEmpty)
    require(outputDir != null)
    _outputSeqs = outputSeqs
  }

  def cmdLine: String =
    executable +
      optional("-v", v) +
      optional("-m", m) +
      optional("-b", b) +
      conditional(storeQualScores, "--store_qual_scores") +
      (sampleIds match {
        case l: List[_] if l.nonEmpty => optional("--sample_ids", l.mkString(","))
        case _ => ""
      }) +
      conditional(storeDemultiplexedFastq, "--store_demultiplexed_fastq") +
      conditional(retainUnassignedReads, "--retain_unassigned_reads") +
      optional("-r", r) +
      optional("-p", p) +
      optional("-n", n) +
      optional("-s", s) +
      conditional(revCompBarcode, "--rev_comp_barcode") +
      conditional(revCompMappingBarcodes, "--rev_comp_mapping_barcodes") +
      conditional(revComp, "--rev_comp") +
      optional("-q", q) +
      optional("--last_bad_quality_char", lastBadQualityChar) +
      optional("--barcode_type", barcodeType) +
      optional("--max_barcode_errors", maxBarcodeErrors) +
      optional("--phred_offset", phredOffset) +
      (input match {
        case l: List[_] if l.nonEmpty => required("-i", l.mkString(","))
        case _ => ""
      }) +
      optional("-o", outputDir)
}
