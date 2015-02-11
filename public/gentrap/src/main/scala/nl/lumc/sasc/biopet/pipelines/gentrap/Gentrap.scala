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

  import Gentrap._
  import Gentrap.ExpMeasures._

  // alternative constructor for initialization with empty configuration
  def this() = this(null)

  /** Split aligner to use */
  var aligner: String = config("aligner", default = "gsnap")

  /** Expression measurement rawName */
  // see the enumeration below for valid modes
  var expressionMeasures: List[String] = config("expression_measures", default = Nil)

  /** GTF reference file */
  var annotationGtf: Option[File] = config("annotation_gtf")

  /** BED reference file */
  var annotationBed: Option[File] = config("annotation_bed")

  /** refFlat reference file */
  var annotationRefFlat: Option[File] = config("annotation_refflat")

  /*
  /** Whether library is strand-specific (dUTP protocol) or not */
  @Argument(doc = "Whether input data was made using the dUTP strand-specific protocol", fullName = "strand_specific", shortName = "strandSpec", required = true)
  var strandSpec: Boolean = _

  /** Variant calling */
  @Argument(doc = "Variant caller", fullName = "variant_caller", shortName = "varCaller", required = false, validation = "varscan|snvmix")
  var varcaller: String = _
  */

  /** Default pipeline config */
  override def defaults = ConfigUtils.mergeMaps(
    Map(
      "gsnap" -> Map(
        "novelsplicing" -> 1,
        "batch" -> 4,
        "format" -> "sam"
      )
    ), super.defaults)

  /** Conversion from raw user-supplied expression measure string to enum value */
  private def makeExpMeasure(rawName: String): ExpMeasures.Value = {
    // process name so it is case insensitive and has camelCaps instead of underscores
    val readyName = rawName
      .split("_")
      .map(_.toLowerCase.capitalize)
      .mkString("")
    try {
      ExpMeasures.withName(readyName)
    } catch {
      case nse: NoSuchElementException => throw new IllegalArgumentException("Invalid expression measure: " + rawName)
      case e: Exception                => throw e
    }
  }

  /** Private container for active expression measures */
  private val expMeasures: Set[ExpMeasures.Value] = expressionMeasures
    .map { makeExpMeasure }.toSet

  /** Steps to run before biopetScript */
  def init(): Unit = {
    // validate required annotation files
    if (expMeasures.contains(GeneReads))
      require(annotationGtf.isDefined, "GTF file must be defined for counting reads per gene")

    if (expMeasures.contains(GeneBases))
      require(annotationBed.isDefined, "BED file must be defined for counting bases per gene")

    if (expMeasures.contains(ExonBases))
      require(annotationBed.isDefined, "BED file must be defined for counting bases per exon")
  }

  def biopetScript(): Unit = {
    addSamplesJobs()
  }

  def addMultiSampleJobs(): Unit = {}

  def makeSample(sampleId: String): Sample = new Sample(sampleId)

  class Sample(sampleId: String) extends AbstractSample(sampleId) {

    lazy val alnFile: File = createFile(".bam")

    lazy val geneReadsCount: Option[File] = expMeasures
      .contains(GeneReads)
      .option(createFile(".gene_reads_count"))

    def addJobs(): Unit = {
      // add per-library jobs
      addPerLibJobs()
      // merge or symlink per-library alignments
      addSampleAlnJob()
      // measure expression depending on modes set in expMeasures
      if (expMeasures.contains(GeneReads))
        addGeneReadsJob()
    }

    private def addSampleAlnJob(): Unit = libraries.values.map(_.alnFile).toList match {
      // library only has one file, then we symlink
      case file :: Nil =>
        val ln = new Ln(qscript)
        ln.in = file
        ln.out = alnFile
        add(ln)
      // library has multiple files, then we merge
      case files @ f :: fs =>
        val merge = new MergeSamFiles(qscript)
        merge.input = files
        merge.sortOrder = "coordinate"
        merge.output = alnFile
        add(merge)
      // library has 0 or less files, error!
      case Nil => throw new IllegalStateException("Per-library alignment files nonexistent.")
    }

    /** Add jobs for reads per gene counting using HTSeq */
    // We are forcing the sort order to be ID-sorted, since HTSeq-count often chokes when using position-sorting due
    // to its buffer not being large enough.
    private def addGeneReadsJob(): Unit = {
      require(annotationGtf.nonEmpty && geneReadsCount.nonEmpty)

      val idSortingJob = new SortSam(qscript)
      idSortingJob.input = alnFile
      idSortingJob.output = createFile(".idsorted.bam")
      idSortingJob.sortOrder = "queryname"
      add(idSortingJob)

      val geneReadJob = new HtseqCount(qscript)
      geneReadJob.format = Option("bam")
      geneReadJob.order = Option("name")
      geneReadJob.inputAnnotation = annotationGtf.get
      geneReadJob.inputAlignment = idSortingJob.output
      geneReadJob.output = geneReadsCount.get
      add(geneReadJob)
    }

    def makeLibrary(libId: String): Library = new Library(libId)

    class Library(libId: String) extends AbstractLibrary(libId) {

      val mapping: Mapping = new Mapping(qscript)

      /** Alignment results of this library ~ can only be accessed after addJobs is run! */
      def alnFile: File = mapping.outputFiles("finalBamFile")

      def addJobs(): Unit = {
        // create per-library alignment file
        mapping.sampleId = sampleId
        mapping.libId = libId
        mapping.outputDir = libDir
        mapping.input_R1 = config("R1")
        // R2 is optional here (input samples could be paired-end or single-end)
        mapping.input_R2 = config("R2")
        mapping.init()
        mapping.biopetScript()
        addAll(mapping.functions)
      }
    }
  }
}

object Gentrap extends PipelineCommand {

  /** implicit extension that allows to create option values based on boolean values */
  implicit class RichBoolean(val b: Boolean) extends AnyVal {
    final def option[A](a: => A): Option[A] = if (b) Some(a) else None
  }

  /** Enumeration of available expression measures */
  object ExpMeasures extends Enumeration {
    val GeneReads, GeneBases, ExonBases, CufflinksStrict, CufflinksGuided, CufflinksBlind, Cuffquant, Rsem = Value
  }

  /** Enumeration of available strandedness */
  object Strandedness extends Enumeration {
    // for now, only non-strand specific and dUTP stranded protocol is supported
    val NonSpecific, Dutp = Value
  }
}