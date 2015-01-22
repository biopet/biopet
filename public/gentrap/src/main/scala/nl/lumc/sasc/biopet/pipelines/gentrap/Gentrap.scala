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

import java.io.File

import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.HtseqCount
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import nl.lumc.sasc.biopet.extensions.picard.SortSam
import org.broadinstitute.gatk.queue.QScript

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping

/**
 * Gentrap pipeline
 * Generic transcriptome analysis pipeline
 */
// TODO: less vars, less mutable state
class Gentrap(val root: Configurable) extends QScript with MultiSampleQScript {

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
  @Argument(doc = "Split aligner (default: gsnap)", fullName = "aligner", shortName = "aln", required = false, validation = "gsnap|tophat|star|star-2pass")
  var aligner: String = config("aligner", default = "gsnap")

  /** Gene-wise read count table output */
  @Argument(doc = "Output read counts per gene (default: false)", fullName = "count_gene_read", shortName = "cGeneRead", required = false)
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

  defaults ++= Map(
    "gsnap" -> Map(
      "novelsplicing" -> 1,
      "batch" -> 4,
      "format" -> "sam"
    )
  )

  /** General function to get sample or library config ID */
  protected def getUnitId(unitConfig: Map[String, Any]): String = unitConfig("ID").toString

  /** Trait for object that contains input and/or output files */
  protected trait OutputContainer { this: { val unitConfig: Map[String, Any] } =>

    /** The container ID */
    val id: String = getUnitId(unitConfig)

    /** Run directory of the container */
    val runDirectory: File

    /** Function to return a file within the run directory given an extension */
    def makeFile(extension: String): File = new File(runDirectory, id + extension)
  }

  /** Per-sample output file container */
  class SampleOutput(val unitConfig: Map[String, Any]) extends AbstractSampleOutput with OutputContainer {

    /** Per-sample run directory */
    val runDirectory = new File(outputDir, "sample_" + id)

    /** Sample input alignment files */
    // lazy since we need to execute library jobs first
    lazy val inputAlignmentFiles: List[File] = libraries.values.map(x => x.alignmentFile).toList

    /** Sample output alignment file */
    val alignmentFile: File = makeFile(".bam")
  }

  /** Per-library output file container */
  // TODO: Use SampleOutput directly?
  class LibraryOutput(val unitConfig: Map[String, Any], sampleConfig: Map[String, Any]) extends AbstractLibraryOutput with OutputContainer {

    /** Sample ID of this library */
    val sampleId: String = getUnitId(sampleConfig)

    /** Per-library run directory */
    val runDirectory = new File(new File(outputDir, "sample_" + getUnitId(sampleConfig)), "lib_" + id)

    /** Library input read 1 */
    val inputRead1: File = unitConfig.get("R1") match {
      case Some(r1) =>
        val f1 = new File(r1.toString)
        require(f1.exists, "Read 1 file " + r1.toString + " not found")
        f1
      case None => throw new IllegalArgumentException("Missing read 1 for library " + id + " in sample " + sampleId)
    }

    /** Library input read 2 */
    val inputRead2: Option[File] = unitConfig.get("R2") match {
      case Some(r2) =>
        val f2 = new File(r2.toString)
        require(f2.exists, "Read 2 file " + r2.toString + " not found")
        Some(f2)
      case None => None
    }

    /** Final mapped alignment file per library*/
    var alignmentFile: File = _
  }

  // empty implementation
  def init() {}

  def biopetScript() {
    runSamplesJobs()
  }

  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput = {

    // setup output container
    val so = new SampleOutput(sampleConfig)
    so.libraries = runLibraryJobs(sampleConfig)

    // create per-sample alignment file
    val sampleAlignmentJob = so.inputAlignmentFiles match {
      // library only has one file, then we symlink
      case file :: Nil =>
        val ln = new Ln(this)
        ln.in = file
        ln.out = so.alignmentFile
        ln
      // library has multiple files, then we merge
      case files @ f :: fs =>
        val merge = new MergeSamFiles(this)
        merge.input = files
        merge.sortOrder = "coordinate"
        merge.output = so.alignmentFile
        merge
      // library has 0 or less files, error!
      case Nil => throw new IllegalStateException("Per-library alignment files nonexistent.")
    }
    add(sampleAlignmentJob)

    // do gene read counts if set ~ and use ID-sorted bam
    if (cGeneRead) {
      val idSortingJob = new SortSam(this)
      idSortingJob.input = so.alignmentFile
      idSortingJob.output = so.makeFile(".idsorted.bam")
      idSortingJob.sortOrder = "queryname"
      add(idSortingJob)

      val geneReadJob = new HtseqCount(this)
      geneReadJob.format = "bam"
      geneReadJob.order = "name"
      geneReadJob.inputAlignment = idSortingJob.output
      add(geneReadJob)
    }

    so
  }

  def runSingleLibraryJobs(libConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput = {

    // setup output container
    val lo = new LibraryOutput(libConfig, sampleConfig)

    // create per-library alignment file
    val mapping = new Mapping(this)
    mapping.input_R1 = lo.inputRead1
    // input_R2 may or may not exist (pipeline can handle single-end or paired-end inputs)
    mapping.input_R2 = lo.inputRead2 match {
      case Some(f) => f
      case None    => null
    }
    mapping.aligner = aligner
    mapping.RGSM = lo.sampleId
    mapping.RGLB = lo.id
    mapping.outputDir = lo.runDirectory
    mapping.skipFlexiprep = config("skipFlexiprep", default = false)
    mapping.skipMetrics = config("skipMetrics", default = true)
    mapping.skipMarkduplicates = config("skipMarkDuplicates", default = true)
    mapping.init()
    mapping.biopetScript()
    addAll(mapping.functions)
    lo.alignmentFile = mapping.outputFiles("finalBamFile")

    lo
  }

}

object Gentrap extends PipelineCommand
