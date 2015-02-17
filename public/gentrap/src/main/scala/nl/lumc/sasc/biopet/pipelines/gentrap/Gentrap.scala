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
  import Gentrap.StrandProtocol._

  // alternative constructor for initialization with empty configuration
  def this() = this(null)

  /** Split aligner to use */
  var aligner: String = config("aligner", default = "gsnap")

  /** Expression measurement modes */
  // see the enumeration below for valid modes
  var expressionMeasures: List[String] = config("expression_measures", default = Nil)

  /** Strandedness modes */
  var strandProtocol: String = config("strand_protocol", default = "non_specific")

  /** GTF reference file */
  var annotationGtf: Option[File] = config("annotation_gtf")

  /** BED reference file */
  var annotationBed: Option[File] = config("annotation_bed")

  /** refFlat reference file */
  var annotationRefFlat: Option[File] = config("annotation_refflat")

  /*
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

  /** Private container for expression modes */
  private val expMeasures: Set[ExpMeasures.Value] = expressionMeasures
    .map { makeExpMeasure }.toSet

  /** Private value for strand protocol */
  private val strProtocol: StrandProtocol.Value = makeStrandProtocol(strandProtocol)

  /** Steps to run before biopetScript */
  def init(): Unit = {
    // TODO: validate that exons are flattened or not (depending on another option flag?)
    // validate required annotation files
    if (expMeasures.contains(GeneReads))
      require(annotationGtf.isDefined, "GTF file must be defined for counting reads per gene")

    if (expMeasures.contains(ExonReads))
      // TODO: validate that GTF file contains exon features
      require(annotationGtf.isDefined, "GTF file must be defined for counting reads per exon")

    if (expMeasures.contains(GeneBases))
      require(annotationBed.isDefined, "BED file must be defined for counting bases per gene")

    if (expMeasures.contains(ExonBases))
      require(annotationBed.isDefined, "BED file must be defined for counting bases per exon")
  }

  def biopetScript(): Unit = {
    addSamplesJobs()
  }

  def addMultiSampleJobs(): Unit = {
  }

  def makeSample(sampleId: String): Sample = new Sample(sampleId)

  class Sample(sampleId: String) extends AbstractSample(sampleId) {

    /** Per-sample alignment file */
    lazy val alnFile: File = createFile(".bam")

    /** Read count per gene file */
    lazy val geneReadsCount: Option[File] = geneReadsJob
      .collect { case job => job.output }

    /** Read count per exon file */
    lazy val exonReadsCount: Option[File] = exonReadsJob
      .collect { case job => job.output }

    /** ID-sorting job for HTseq-count jobs */
    private lazy val idSortingJob: SortSam = {
      val job = new SortSam(qscript)
      job.input = alnFile
      job.output = createFile(".idsorted.bam")
      job.sortOrder = "queryname"
      job
    }

    /** Read counting job per gene */
    private lazy val geneReadsJob: Option[HtseqCount] = expMeasures
      .contains(GeneReads)
      .option { require(functions.contains(idSortingJob))
        val job = new HtseqCount(qscript)
        job.inputAnnotation = annotationGtf.get
        job.inputAlignment = idSortingJob.output
        job.output = createFile(".gene_reads_count")
        job.format = Option("bam")
        job.order = Option("name")
        job.stranded = strProtocol match {
          case NonSpecific => Option("no")
          case Dutp        => Option("reverse")
          case _           => throw new IllegalStateException
        }
        job
      }

    /** Read counting job per exon */
    private lazy val exonReadsJob: Option[HtseqCount] = expMeasures
      .contains(ExonReads)
      .option { require(functions.contains(idSortingJob))
        val job = new HtseqCount(qscript)
        job.inputAnnotation = annotationGtf.get
        job.inputAlignment = idSortingJob.output
        job.output = createFile(".exon_reads_count")
        job.format = Option("bam")
        job.order = Option("name")
        job.stranded = strProtocol match {
          case NonSpecific => Option("no")
          case Dutp        => Option("reverse")
          case _           => throw new IllegalStateException
        }
        // TODO: ensure that the "exon_id" attributes exist for all exons in the GTF
        job.idattr = Option("exon_id")
        job
    }

    // TODO: add warnings or other messages for config values that are hard-coded by the pipeline
    def addJobs(): Unit = {
      // add per-library jobs
      addPerLibJobs()
      // merge or symlink per-library alignments
      addSampleAlnJob()
      // measure expression depending on modes set in expMeasures
      if (expMeasures.contains(GeneReads) || expMeasures.contains(ExonReads)) {
        add(idSortingJob)
        geneReadsJob.foreach(add(_))
        exonReadsJob.foreach(add(_))
      }
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
    val GeneReads, ExonReads, GeneBases, ExonBases, CufflinksStrict, CufflinksGuided, CufflinksBlind, Cuffquant, Rsem = Value
  }

  /** Enumeration of available strandedness */
  object StrandProtocol extends Enumeration {
    // for now, only non-strand specific and dUTP stranded protocol is supported
    // TODO: other strandedness protocol?
    val NonSpecific, Dutp = Value
  }

  /** Converts string with underscores into camel-case strings */
  private def camelize(ustring: String): String = ustring
    .split("_")
    .map(_.toLowerCase.capitalize)
    .mkString("")

  /** Conversion from raw user-supplied expression measure string to enum value */
  private def makeExpMeasure(rawName: String): ExpMeasures.Value = {
    try {
      ExpMeasures.withName(camelize(rawName))
    } catch {
      case nse: NoSuchElementException => throw new IllegalArgumentException("Invalid expression measure: " + rawName)
      case e: Exception                => throw e
    }
  }

  /** Conversion from raw user-supplied expression measure string to enum value */
  private def makeStrandProtocol(rawName: String): StrandProtocol.Value = {
    try {
      StrandProtocol.withName(camelize(rawName))
    } catch {
      case nse: NoSuchElementException => throw new IllegalArgumentException("Invalid strand protocol: " + rawName)
      case e: Exception                => throw e
    }
  }
}