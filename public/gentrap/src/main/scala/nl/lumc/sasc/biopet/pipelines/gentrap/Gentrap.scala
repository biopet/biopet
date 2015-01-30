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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.gentrap

import org.broadinstitute.gatk.queue.QScript

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.HtseqCount
import nl.lumc.sasc.biopet.extensions.picard.{ MergeSamFiles, SortSam }
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import nl.lumc.sasc.biopet.utils.ConfigUtils

/**
 * Gentrap pipeline
 * Generic transcriptome analysis pipeline
 */
class Gentrap(val root: Configurable) extends QScript with MultiSampleQScript { qscript =>

  // alternative constructor for initialization with empty configuration
  def this() = this(null)

  /*
  /** Read 1 input */
  @Input(doc = "FASTQ file input (single-end or pair 1)", fullName = "input_r1", shortName = "R1", required = true)
  var inputR1: File = _

  /** Read 2 input (optional) */
  @Input(doc = "FASTQ file input (pair 2)", fullName = "input_r2", shortName = "R2", required = false)
  var inputR2: File = _

  /** FASTQ trimming */
  @Argument(doc = "Whether to skip trimming input files", fullName = "skip_trim_input", shortName = "skipTrim", required = false)
  var skipTrim: Boolean = false

  /** FASTQ clipping */
  @Argument(doc = "Whether to skip clipping input files", fullName = "skip_clip_input", shortName = "skipClip", required = false)
  var skipClip: Boolean = false
  */

  /** Split aligner to use */
  var aligner: String = config("aligner", default = "gsnap")

  /** Gene-wise read count table output */
  var cGeneRead: Boolean = config("gene_read_counts", default = false)

  /*
  /** Whether library is strand-specific (dUTP protocol) or not */
  @Argument(doc = "Whether input data was made using the dUTP strand-specific protocol", fullName = "strand_specific", shortName = "strandSpec", required = true)
  var strandSpec: Boolean = _

  /** Variant calling */
  @Argument(doc = "Variant caller", fullName = "variant_caller", shortName = "varCaller", required = false, validation = "varscan|snvmix")
  var varcaller: String = _

  /** Cufflinks assembly type */
  @Argument(doc = "Cufflinks assembly type", fullName = "transcript_asm", shortName = "transAsm", required = false, validation = "none|strict|guided|blind")
  var asm: List[String] = List("none")

  /** Gene-wise base count table output */
  @Argument(doc = "Gene base count table output", fullName = "count_gene_base", shortName = "cGeneBase", required = false)
  var cGeneBase: Boolean = _

  /** Exon-wise read count table output */
  @Argument(doc = "Exon read count table output", fullName = "count_exon_read", shortName = "cExonRead", required = false)
  var cExonRead: Boolean = _

  /** Exon-wise base count table output */
  @Argument(doc = "Exon base count table output", fullName = "count_exon_base", shortName = "cExonBase", required = false)
  var cExonBase: Boolean = _
  */

  override def defaults = ConfigUtils.mergeMaps(
    Map(
      "gsnap" -> Map(
        "novelsplicing" -> 1,
        "batch" -> 4,
        "format" -> "sam"
      )
    ), super.defaults)

  def makeSample(sampleId: String): Sample = new Sample(sampleId)

  class Sample(sampleId: String) extends AbstractSample(sampleId) {

    val alnFile: File = createFile(".bam")

    def addJobs(): Unit = {

      // create per-sample alignment file
      val sampleAlignmentJob: Either[Ln, MergeSamFiles] = libraries.values.toList
        .map(lib => lib.alnFile) match {
          // library only has one file, then we symlink
          case file :: Nil =>
            val ln = new Ln(qscript)
            ln.in = file
            ln.out = alnFile
            Left(ln)
          // library has multiple files, then we merge
          case files @ f :: fs =>
            val merge = new MergeSamFiles(qscript)
            merge.input = files
            merge.sortOrder = "coordinate"
            merge.output = alnFile
            Right(merge)
          // library has 0 or less files, error!
          case Nil => throw new IllegalStateException("Per-library alignment files nonexistent.")
        }
      add(sampleAlignmentJob.merge)

      // do gene read counts if set ~ and use ID-sorted bam
      if (cGeneRead) {
        val idSortingJob = new SortSam(qscript)
        idSortingJob.input = alnFile
        idSortingJob.output = createFile(".idsorted.bam")
        idSortingJob.sortOrder = "queryname"
        add(idSortingJob)

        val geneReadJob = new HtseqCount(qscript)
        geneReadJob.format = "bam"
        geneReadJob.order = "name"
        geneReadJob.inputAlignment = idSortingJob.output
        add(geneReadJob)
      }

    }

    def makeLibrary(libId: String): Library = new Library(libId)

    class Library(libId: String) extends AbstractLibrary(libId) {

      val mapping: Mapping = new Mapping(qscript)

      /** Alignment results of this library ~ can only be accessed after addJobs is run! */
      def alnFile: File = mapping.outputFiles("finalBamFile")

      def addJobs(): Unit = {
        // create per-library alignment file
        mapping.input_R1 = config("R1", required = true)
        // TODO: update this once mapping.input_R2 becomes Option[File]
        mapping.input_R2 =
          if (config.contains("R2")) config("R2")
          else null
        mapping.outputDir = this.libDir
        mapping.init()
        mapping.biopetScript()
        addAll(mapping.functions)
      }
    }
  }

  // empty implementation
  def init() {}

  def biopetScript() {
    addSamplesJobs()
  }

}

object Gentrap extends PipelineCommand
