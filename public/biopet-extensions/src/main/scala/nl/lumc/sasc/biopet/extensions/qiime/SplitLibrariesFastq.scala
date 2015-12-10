package nl.lumc.sasc.biopet.extensions.qiime

import java.io.File

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Created by pjvan_thof on 12/10/15.
 */
class SplitLibrariesFastq(val root: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "split_libraries_fastq.py")

  def versionCommand = executable + " --version"
  def versionRegex = """Version: (.*)""".r

  @Input
  var input: List[File] = Nil
  var outputDir: File = _

  var v: Option[String] = config("v")
  var m: Option[String] = config("m")
  var b: Option[String] = config("b")
  var store_qual_scores: Boolean = config("store_qual_scores", default = false)
  var sample_ids: List[String] = Nil
  var store_demultiplexed_fastq: Boolean = config("store_demultiplexed_fastq", default = false)
  var retain_unassigned_reads: Boolean = config("retain_unassigned_reads", default = false)
  var r: Option[Int] = config("r")
  var p: Option[Double] = config("p")
  var n: Option[Int] = config("n")
  var s: Option[Int] = config("s")
  var rev_comp_barcode: Boolean = config("rev_comp_barcode", default = false)
  var rev_comp_mapping_barcodes: Boolean = config("rev_comp_mapping_barcodes", default = false)
  var rev_comp: Boolean = config("rev_comp", default = false)
  var q: Option[Int] = config("q")
  var last_bad_quality_char: Option[String] = config("last_bad_quality_char")
  var barcode_type: Option[String] = config("barcode_type")
  var max_barcode_errors: Option[Double] = config("max_barcode_errors")
  var phred_offset: Option[String] = config("phred_offset")

  def outputSeqs = new File(outputDir, "seqs.fna")

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    require(input.nonEmpty)
    require(outputDir != null)
    outputFiles :+= outputSeqs
  }

  def cmdLine = executable +
    optional("-v", v) +
    optional("-m", m) +
    optional("-b", b) +
    conditional(store_qual_scores, "--store_qual_scores") +
    (sample_ids match {
      case l: List[_] if l.nonEmpty => optional("--sample_ids", l.mkString(","))
      case _                        => ""
    }) +
    conditional(store_demultiplexed_fastq, "--store_demultiplexed_fastq") +
    conditional(retain_unassigned_reads, "--retain_unassigned_reads") +
    optional("-r", r) +
    optional("-p", p) +
    optional("-n", n) +
    optional("-s", s) +
    conditional(rev_comp_barcode, "--rev_comp_barcode") +
    conditional(rev_comp_mapping_barcodes, "--rev_comp_mapping_barcodes") +
    conditional(rev_comp, "--rev_comp") +
    optional("-q", q) +
    optional("--last_bad_quality_char", last_bad_quality_char) +
    optional("--barcode_type", barcode_type) +
    optional("--max_barcode_errors", max_barcode_errors) +
    optional("--phred_offset", phred_offset) +
    optional("-i", input) +
    optional("-o", outputDir)
}