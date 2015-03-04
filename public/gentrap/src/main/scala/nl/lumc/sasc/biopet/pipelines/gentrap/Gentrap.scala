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
import scala.language.reflectiveCalls

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function.QFunction
import picard.analysis.directed.RnaSeqMetricsCollector.StrandSpecificity

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.core.summary._
import nl.lumc.sasc.biopet.extensions.{ Cufflinks, HtseqCount, Ln }
import nl.lumc.sasc.biopet.extensions.picard.{ CollectRnaSeqMetrics, GatherBamFiles, MergeSamFiles, SortSam }
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsView
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import nl.lumc.sasc.biopet.pipelines.gentrap.extensions.RawBaseCounter
import nl.lumc.sasc.biopet.pipelines.gentrap.scripts.AggrBaseCount
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.tools.{ MergeTables, WipeReads }

/**
 * Gentrap pipeline
 * Generic transcriptome analysis pipeline
 */
class Gentrap(val root: Configurable) extends QScript with MultiSampleQScript with SummaryQScript { qscript =>

  import Gentrap._
  import Gentrap.ExpMeasures._
  import Gentrap.StrandProtocol._

  // alternative constructor for initialization with empty configuration
  def this() = this(null)

  /** Split aligner to use */
  var aligner: String = config("aligner", default = "gsnap")

  /** Expression measurement modes */
  // see the enumeration below for valid modes
  var expressionMeasures: List[String] = config("expression_measures")

  /** Strandedness modes */
  var strandProtocol: String = config("strand_protocol")

  /** GTF reference file */
  var annotationGtf: Option[File] = config("annotation_gtf")

  /** BED reference file */
  var annotationBed: Option[File] = config("annotation_bed")

  /** refFlat reference file */
  var annotationRefFlat: File = config("annotation_refflat")

  /** rRNA refFlat annotation */
  var ribosomalRefFlat: Option[File] = config("ribosome_refflat")

  /** Whether to remove rRNA regions or not */
  var removeRibosomalReads: Boolean = config("remove_ribosomal_reads", default = false)

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

  /** Adds output merge jobs for the given expression mode */
  // TODO: can we combine the enum with the file extension (to reduce duplication and potential errors)
  private def makeMergeTableJob(inFunc: (Sample => Option[File]), ext: String, idCols: List[Int], valCol: Int,
                                outBaseName: String = "all_samples", fallback: String = "-"): Option[MergeTables] = {
    val tables = samples.values.map { inFunc }.toList.flatten
    tables.nonEmpty
      .option {
        val job = new MergeTables(qscript)
        job.inputTables = tables
        job.output = new File(outputDir, outBaseName + ext)
        job.idColumnIndices = idCols.map(_.toString)
        job.valueColumnIndex = valCol
        job.fileExtension = Option(ext)
        job.fallbackString = Option(fallback)
        // TODO: separate the addition into another function?
        job
      }
  }

  /** Merged gene fragment count table */
  private def geneFragmentsCountJob =
    makeMergeTableJob((s: Sample) => s.geneFragmentsCount, ".fragments_per_gene", List(1), 2, fallback = "0")

  /** Merged exon fragment count table */
  private def exonFragmentsCountJob =
    makeMergeTableJob((s: Sample) => s.exonFragmentsCount, ".fragments_per_exon", List(1), 2, fallback = "0")

  /** Merged gene base count table */
  private def geneBasesCountJob =
    makeMergeTableJob((s: Sample) => s.geneBasesCount, ".bases_per_gene", List(1), 2, fallback = "0")

  /** Merged exon base count table */
  private def exonBasesCountJob =
    makeMergeTableJob((s: Sample) => s.exonBasesCount, ".bases_per_exon", List(1), 2, fallback = "0")

  /** Merged gene FPKM table for Cufflinks, strict mode */
  private def geneFpkmCufflinksStrictJob =
    makeMergeTableJob((s: Sample) => s.geneFpkmCufflinksStrict, ".genes_fpkm_cufflinks_strict", List(1, 7), 10)

  /** Merged exon FPKM table for Cufflinks, strict mode */
  private def isoFpkmCufflinksStrictJob =
    makeMergeTableJob((s: Sample) => s.isoformFpkmCufflinksStrict, ".isoforms_fpkm_cufflinks_strict", List(1, 7), 10)

  /** Merged gene FPKM table for Cufflinks, guided mode */
  private def geneFpkmCufflinksGuidedJob =
    makeMergeTableJob((s: Sample) => s.geneFpkmCufflinksGuided, ".genes_fpkm_cufflinks_guided", List(1, 7), 10)

  /** Merged isoforms FPKM table for Cufflinks, guided mode */
  private def isoFpkmCufflinksGuidedJob =
    makeMergeTableJob((s: Sample) => s.isoformFpkmCufflinksGuided, ".isoforms_fpkm_cufflinks_guided", List(1, 7), 10)

  /** Merged gene FPKM table for Cufflinks, blind mode */
  private def geneFpkmCufflinksBlindJob =
    makeMergeTableJob((s: Sample) => s.geneFpkmCufflinksBlind, ".genes_fpkm_cufflinks_blind", List(1, 7), 10)

  /** Merged isoforms FPKM table for Cufflinks, blind mode */
  private def isoFpkmCufflinksBlindJob =
    makeMergeTableJob((s: Sample) => s.isoformFpkmCufflinksBlind, ".isoforms_fpkm_cufflinks_blind", List(1, 7), 10)

  private lazy val mergedTables: Map[String, Option[MergeTables]] = Map(
    "gene_fragments_count" -> geneFragmentsCountJob,
    "exon_fragments_count" -> exonFragmentsCountJob,
    "gene_bases_count" -> geneBasesCountJob,
    "exon_bases_count" -> exonBasesCountJob,
    "gene_fpkm_cufflinks_strict" -> geneFpkmCufflinksStrictJob,
    "isoform_fpkm_cufflinks_strict" -> isoFpkmCufflinksStrictJob,
    "gene_fpkm_cufflinks_guided" -> geneFpkmCufflinksGuidedJob,
    "isoform_fpkm_cufflinks_guided" -> isoFpkmCufflinksGuidedJob,
    "gene_fpkm_cufflinks_blind" -> geneFpkmCufflinksBlindJob,
    "isoform_fpkm_cufflinks_blind" -> isoFpkmCufflinksBlindJob
  )

  /** Output summary file */
  def summaryFile: File = new File(outputDir, "gentrap.summary.json")

  /** Files that will be listed in the summary file */
  def summaryFiles: Map[String, File] =
    mergedTables.collect { case (key, Some(value)) => key -> value.output }

  /** Statistics shown in the summary file */
  def summaryStats: Map[String, Any] = Map()

  /** Pipeline settings shown in the summary file */
  def summarySettings: Map[String, Any] = Map(
    "aligner" -> aligner,
    "expression_measures" -> expressionMeasures,
    "strand_protocol" -> strandProtocol,
    "annotation_refflat" -> annotationRefFlat
  ) ++ Map(
      "annotation_gtf" -> annotationGtf,
      "annotation_bed" -> annotationBed
    ).collect { case (key, Some(value)) => key -> value }

  /** Steps to run before biopetScript */
  def init(): Unit = {
    // TODO: validate that exons are flattened or not (depending on another option flag?)
    // validate required annotation files
    if (expMeasures.contains(FragmentsPerGene))
      require(annotationGtf.isDefined, "GTF file must be defined for counting fragments per gene")

    if (expMeasures.contains(FragmentsPerExon))
      // TODO: validate that GTF file contains exon features
      require(annotationGtf.isDefined, "GTF file must be defined for counting fragments per exon")

    if (expMeasures.contains(BasesPerGene))
      require(annotationBed.isDefined, "BED file must be defined for counting bases per gene")

    if (expMeasures.contains(BasesPerExon))
      require(annotationBed.isDefined, "BED file must be defined for counting bases per exon")

    if (expMeasures.contains(CufflinksBlind) || expMeasures.contains(CufflinksGuided) || expMeasures.contains(CufflinksStrict))
      require(annotationGtf.isDefined, "GTF file must be defined for Cufflinks-based modes")

    if (removeRibosomalReads)
      require(ribosomalRefFlat.isDefined, "rRNA intervals must be supplied if removeRibosomalReads is set")
  }

  def biopetScript(): Unit = {
    addSamplesJobs()
  }

  def addMultiSampleJobs(): Unit = {
    // merge expression tables
    mergedTables.values.foreach { case maybeJob => maybeJob.foreach(add(_)) }
    // TODO: use proper notation
    addSummaryJobs
  }

  def makeSample(sampleId: String): Sample = new Sample(sampleId)

  class Sample(sampleId: String) extends AbstractSample(sampleId) {

    /** Sample output directory */
    override def sampleDir: File = new File(outputDir, "sample_" + sampleId)

    /** Summary stats of the sample */
    def summaryStats: Map[String, Any] = Map()

    /** Summary files of the sample */
    def summaryFiles: Map[String, File] = Map(
      "alignment" -> alnFile,
      "metrics" -> collectRnaSeqMetricsJob.output
    ) ++ Map(
        "gene_fragments_count" -> geneFragmentsCount,
        "exon_fragments_count" -> exonFragmentsCount,
        "gene_bases_count" -> geneBasesCount,
        "exon_bases_count" -> exonBasesCount,
        "gene_fpkm_cufflinks_strict" -> geneFpkmCufflinksStrict,
        "isoform_fpkm_cufflinks_strict" -> isoformFpkmCufflinksStrict,
        "gene_fpkm_cufflinks_guided" -> geneFpkmCufflinksGuided,
        "isoform_fpkm_cufflinks_guided" -> isoformFpkmCufflinksGuided,
        "gene_fpkm_cufflinks_blind" -> geneFpkmCufflinksBlind,
        "isoform_fpkm_cufflinks_blind" -> isoformFpkmCufflinksBlind
      ).collect { case (key, Some(value)) => key -> value }

    /** Per-sample alignment file, pre rRNA cleanup (if chosen) */
    lazy val alnFileDirty: File = sampleAlnJob.output

    /** Per-sample alignment file, post rRNA cleanup (if chosen) */
    lazy val alnFile: File = wipeJob match {
      case Some(j) => j.outputBam
      case None    => alnFileDirty
    }

    /** Read count per gene file */
    def geneFragmentsCount: Option[File] = fragmentsPerGeneJob
      .collect { case job => job.output }

    /** Read count per exon file */
    def exonFragmentsCount: Option[File] = fragmentsPerExonJob
      .collect { case job => job.output }

    /** Base count per gene file */
    def geneBasesCount: Option[File] = basesPerGeneJob
      .collect { case job => job.output }

    /** Base count per exon file */
    def exonBasesCount: Option[File] = basesPerExonJob
      .collect { case job => job.output }

    /** Gene tracking file from Cufflinks strict mode */
    def geneFpkmCufflinksStrict: Option[File] = cufflinksStrictJobSet
      .collect { case jobSet => jobSet.geneJob.output }

    /** Isoforms tracking file from Cufflinks strict mode */
    def isoformFpkmCufflinksStrict: Option[File] = cufflinksStrictJobSet
      .collect { case jobSet => jobSet.isoformJob.output }

    /** Gene tracking file from Cufflinks guided mode */
    def geneFpkmCufflinksGuided: Option[File] = cufflinksGuidedJob
      .collect { case jobSet => jobSet.geneJob.output }

    /** Isoforms tracking file from Cufflinks guided mode */
    def isoformFpkmCufflinksGuided: Option[File] = cufflinksGuidedJob
      .collect { case jobSet => jobSet.isoformJob.output }

    /** Gene tracking file from Cufflinks guided mode */
    def geneFpkmCufflinksBlind: Option[File] = cufflinksBlindJob
      .collect { case jobSet => jobSet.geneJob.output }

    /** Isoforms tracking file from Cufflinks blind mode */
    def isoformFpkmCufflinksBlind: Option[File] = cufflinksBlindJob
      .collect { case jobSet => jobSet.isoformJob.output }

    /** ID-sorting job for HTseq-count jobs */
    private def idSortingJob: Option[SortSam] = (expMeasures.contains(FragmentsPerExon) || expMeasures.contains(FragmentsPerGene))
      .option {
        val job = new SortSam(qscript)
        job.input = alnFile
        job.output = createFile(".idsorted.bam")
        job.sortOrder = "queryname"
        job
      }

    /** Read counting job per gene */
    private def fragmentsPerGeneJob: Option[HtseqCount] = expMeasures
      .contains(FragmentsPerGene)
      .option {
        require(idSortingJob.nonEmpty)
        val job = new HtseqCount(qscript)
        job.inputAnnotation = annotationGtf.get
        job.inputAlignment = idSortingJob.get.output
        job.output = createFile(".fragments_per_gene")
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
    private def fragmentsPerExonJob: Option[HtseqCount] = expMeasures
      .contains(FragmentsPerExon)
      .option {
        require(idSortingJob.nonEmpty)
        val job = new HtseqCount(qscript)
        job.inputAnnotation = annotationGtf.get
        job.inputAlignment = idSortingJob.get.output
        job.output = createFile(".fragments_per_exon")
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

    /** Container for strand-separation jobs */
    private case class StrandSeparationJobSet(pair1Job: SamtoolsView, pair2Job: Option[SamtoolsView],
                                              combineJob: QFunction { def output: File }) {
      def addAllJobs(): Unit = {
        add(pair1Job); pair2Job.foreach(add(_)); add(combineJob)
      }
    }

    def alnFilePlusStrand: Option[File] = alnPlusStrandJobs
      .collect { case jobSet => jobSet.combineJob.output }

    private def alnPlusStrandJobs: Option[StrandSeparationJobSet] = strProtocol match {
      case Dutp =>
        val r2Job = this.allPaired
          .option {
            val job = new SamtoolsView(qscript)
            job.input = alnFile
            job.b = true
            job.h = true
            job.f = List("0x40")
            job.F = List("0x10")
            job.output = createFile(".r2.bam")
            job.isIntermediate = true
            job
          }

        val f1Job = new SamtoolsView(qscript)
        f1Job.input = alnFile
        f1Job.b = true
        f1Job.h = true
        f1Job.f = if (this.allSingle) List("0x10") else List("0x50")
        f1Job.output = createFile(".f1.bam")
        // since we are symlinking if the other pair does not exist,
        // we want to keep this job as non-intermediate as well
        f1Job.isIntermediate = r2Job.isDefined

        val perStrandFiles = r2Job match {
          case Some(r2j)  => List(f1Job.output, r2j.output)
          case None       => List(f1Job.output)
        }
        val combineJob = makeCombineJob(perStrandFiles, createFile(".plus_strand.bam"), gather = true)

        Option(StrandSeparationJobSet(f1Job, r2Job, combineJob))

      case NonSpecific => None
      case _           => throw new IllegalStateException
    }

    def alnFileMinusStrand: Option[File] = alnMinusStrandJobs
      .collect { case jobSet => jobSet.combineJob.output }

    private def alnMinusStrandJobs: Option[StrandSeparationJobSet] = strProtocol match {
      case Dutp =>
        val r1Job = this.allPaired
          .option {
            val job = new SamtoolsView(qscript)
            job.input = alnFile
            job.b = true
            job.h = true
            job.f = List("0x80")
            job.F = List("0x10")
            job.output = createFile(".r1.bam")
            job.isIntermediate = true
            job
          }

        val f2Job = new SamtoolsView(qscript)
        f2Job.input = alnFile
        f2Job.b = true
        f2Job.h = true
        f2Job.output = createFile(".f2.bam")
        // since we are symlinking if the other pair does not exist,
        // we want to keep this job as non-intermediate as well
        f2Job.isIntermediate = r1Job.isDefined
        if (this.allSingle) f2Job.F = List("0x10")
        else f2Job.f = List("0x90")

        val perStrandFiles = r1Job match {
          case Some(r1j)  => List(f2Job.output, r1j.output)
          case None       => List(f2Job.output)
        }
        val combineJob = makeCombineJob(perStrandFiles, createFile(".minus_strand.bam"), gather = true)

        Option(StrandSeparationJobSet(f2Job, r1Job, combineJob))

      case NonSpecific => None
      case _           => throw new IllegalStateException
    }
    /** Raw base counting job */
    private def rawBaseCountJob: Option[RawBaseCounter] = strProtocol match {
      case NonSpecific =>
        (expMeasures.contains(BasesPerExon) || expMeasures.contains(BasesPerGene))
          .option {
            val job = new RawBaseCounter(qscript)
            job.inputBoth = alnFile
            job.annotationBed = annotationBed.get
            job.output = createFile(".raw_base_count")
            job
          }
      case Dutp => {
        (expMeasures.contains(BasesPerExon) || expMeasures.contains(BasesPerGene))
          .option {
            require(alnFilePlusStrand.isDefined && alnFileMinusStrand.isDefined)
            val job = new RawBaseCounter(qscript)
            job.inputPlus = alnFilePlusStrand.get
            job.inputMinus = alnFileMinusStrand.get
            job.annotationBed = annotationBed.get
            job.output = createFile(".raw_base_count")
            job
          }
      }
      case _ => throw new IllegalStateException
    }

    /** Base counting job per gene */
    private def basesPerGeneJob: Option[AggrBaseCount] = expMeasures
      .contains(BasesPerGene)
      .option {
        require(rawBaseCountJob.nonEmpty)
        val job = new AggrBaseCount(qscript)
        job.input = rawBaseCountJob.get.output
        job.output = createFile(".bases_per_gene")
        job.inputLabel = sampleId
        job.mode = "gene"
        job
      }

    /** Base counting job per exon */
    private def basesPerExonJob: Option[AggrBaseCount] = expMeasures
      .contains(BasesPerExon)
      .option {
        require(rawBaseCountJob.nonEmpty)
        val job = new AggrBaseCount(qscript)
        job.input = rawBaseCountJob.get.output
        job.output = createFile(".bases_per_exon")
        job.inputLabel = sampleId
        job.mode = "exon"
        job
      }

    /** Case class for containing cufflinks + its output symlink jobs */
    private case class CufflinksJobSet(cuffJob: Cufflinks, geneJob: Ln, isoformJob: Ln) {
      /** Adds all contained jobs to Queue */
      def addAllJobs(): Unit = { add(cuffJob); add(geneJob); add(isoformJob) }
    }

    /** Cufflinks strict job */
    private def cufflinksStrictJobSet: Option[CufflinksJobSet] = expMeasures
      .contains(CufflinksStrict)
      .option {
        val cuff = new Cufflinks(qscript) {
          override def configName = "cufflinks"
          override def configPath: List[String] = super.configPath ::: "cufflinks_strict" :: Nil
        }
        cuff.input = alnFile
        cuff.GTF = annotationGtf
        cuff.GTF_guide = None
        cuff.library_type = strProtocol match {
          case NonSpecific => Option("fr-unstranded")
          case Dutp        => Option("fr-firststrand")
          case _           => throw new IllegalStateException
        }
        cuff.output_dir = new File(sampleDir, "cufflinks_strict")

        val geneLn = new Ln(qscript)
        geneLn.input = cuff.outputGenesFpkm
        geneLn.output = createFile(".genes_fpkm_cufflinks_strict")

        val isoLn = new Ln(qscript)
        isoLn.input = cuff.outputIsoformsFpkm
        isoLn.output = createFile(".isoforms_fpkm_cufflinks_strict")

        CufflinksJobSet(cuff, geneLn, isoLn)
      }

    /** Cufflinks guided job */
    private def cufflinksGuidedJob: Option[CufflinksJobSet] = expMeasures
      .contains(CufflinksStrict)
      .option {
        val cuff = new Cufflinks(qscript) {
          override def configName = "cufflinks"
          override def configPath: List[String] = super.configPath ::: "cufflinks_guided" :: Nil
        }
        cuff.input = alnFile
        cuff.GTF = None
        cuff.GTF_guide = annotationGtf
        cuff.library_type = strProtocol match {
          case NonSpecific => Option("fr-unstranded")
          case Dutp        => Option("fr-firststrand")
          case _           => throw new IllegalStateException
        }
        cuff.output_dir = new File(sampleDir, "cufflinks_guided")

        val geneLn = new Ln(qscript)
        geneLn.input = cuff.outputGenesFpkm
        geneLn.output = createFile(".genes_fpkm_cufflinks_guided")

        val isoLn = new Ln(qscript)
        isoLn.input = cuff.outputIsoformsFpkm
        isoLn.output = createFile(".isoforms_fpkm_cufflinks_guided")

        CufflinksJobSet(cuff, geneLn, isoLn)
      }

    /** Cufflinks blind job */
    private def cufflinksBlindJob: Option[CufflinksJobSet] = expMeasures
      .contains(CufflinksStrict)
      .option {
        val cuff = new Cufflinks(qscript) {
          override def configName = "cufflinks"
          override def configPath: List[String] = super.configPath ::: "cufflinks_blind" :: Nil
        }
        cuff.input = alnFile
        cuff.GTF = None
        cuff.GTF_guide = None
        cuff.library_type = strProtocol match {
          case NonSpecific => Option("fr-unstranded")
          case Dutp        => Option("fr-firststrand")
          case _           => throw new IllegalStateException
        }
        cuff.output_dir = new File(sampleDir, "cufflinks_blind")

        val geneLn = new Ln(qscript)
        geneLn.input = cuff.outputGenesFpkm
        geneLn.output = createFile(".genes_fpkm_cufflinks_blind")

        val isoLn = new Ln(qscript)
        isoLn.input = cuff.outputIsoformsFpkm
        isoLn.output = createFile(".isoforms_fpkm_cufflinks_blind")

        CufflinksJobSet(cuff, geneLn, isoLn)
      }

    /** Picard CollectRnaSeqMetrics job */
    private def collectRnaSeqMetricsJob: CollectRnaSeqMetrics = {
      val job = new CollectRnaSeqMetrics(qscript)
      job.input = alnFileDirty
      job.output = createFile(".rna_metrics")
      job.refFlat = annotationRefFlat
      job.chartOutput = Option(createFile(".coverage_bias.pdf"))
      job.assumeSorted = true
      job.strandSpecificity = strProtocol match {
        case NonSpecific => Option(StrandSpecificity.NONE.toString)
        case Dutp        => Option(StrandSpecificity.SECOND_READ_TRANSCRIPTION_STRAND.toString)
        case _           => throw new IllegalStateException
      }
      job.ribosomalIntervals = ribosomalRefFlat
      job
    }

    private def wipeJob: Option[WipeReads] = removeRibosomalReads
      .option {
        require(ribosomalRefFlat.isDefined)
        val job = new WipeReads(qscript)
        job.inputBam = alnFileDirty
        job.intervalFile = ribosomalRefFlat.get
        job.outputBam = createFile(".cleaned.bam")
        job.discardedBam = createFile(".rrna.bam")
        job
      }

    /** Super type of Ln and MergeSamFile */
    private type CombineFileFunction = QFunction { def output: File }

    /** Ln or MergeSamFile job, depending on how many inputs are supplied */
    private def makeCombineJob(inFiles: List[File], outFile: File, gather: Boolean = false,
                               mergeSortOrder: String = "coordinate"): CombineFileFunction = {
      require(inFiles.nonEmpty, "At least one input files for combine job")
      if (inFiles.size == 1) {
        val job = new Ln(qscript)
        job.input = inFiles.head
        job.output = outFile
        job
      } else if (gather) {
        val job = new GatherBamFiles(qscript)
        job.input = inFiles
        job.output = outFile
        job
      } else {
        val job = new MergeSamFiles(qscript)
        job.input = inFiles
        job.output = outFile
        job.sortOrder = mergeSortOrder
        job
      }
    }

    /** Job for combining all library BAMs */
    private def sampleAlnJob: CombineFileFunction =
      makeCombineJob(libraries.values.map(_.alnFile).toList, createFile(".bam"))

    /** Whether all libraries are paired or not */
    def allPaired: Boolean = libraries.values.forall(_.paired)

    /** Whether all libraries are single or not */
    def allSingle: Boolean = libraries.values.forall(!_.paired)

    // TODO: add warnings or other messages for config values that are hard-coded by the pipeline
    def addJobs(): Unit = {
      // TODO: this is our requirement since it's easier to calculate base counts when all libraries are either paired or single
      require(allPaired || allSingle, s"Sample $sampleId contains only single-end or paired-end libraries")
      // add per-library jobs
      addPerLibJobs()
      // merge or symlink per-library alignments
      add(sampleAlnJob)
      // general RNA-seq metrics
      add(collectRnaSeqMetricsJob)
      // add strand-specific jobs if defined
      alnPlusStrandJobs.foreach(_.addAllJobs())
      alnMinusStrandJobs.foreach(_.addAllJobs())
      // add htseq-count jobs, if defined
      idSortingJob.foreach(add(_))
      fragmentsPerGeneJob.foreach(add(_))
      fragmentsPerExonJob.foreach(add(_))
      // add custom base count jobs, if defined
      rawBaseCountJob.foreach(add(_))
      basesPerGeneJob.foreach(add(_))
      basesPerExonJob.foreach(add(_))
      // symlink results with distinct extensions ~ actually to make it easier to use MergeTables on these as well
      // since the Queue argument parser doesn't play nice with Map[_, _] types
      cufflinksStrictJobSet.foreach { case jobSet => jobSet.addAllJobs() }
      cufflinksGuidedJob.foreach { case jobSet => jobSet.addAllJobs() }
      cufflinksBlindJob.foreach { case jobSet => jobSet.addAllJobs() }
    }

    /** Add jobs for fragments per gene counting using HTSeq */
    // We are forcing the sort order to be ID-sorted, since HTSeq-count often chokes when using position-sorting due
    // to its buffer not being large enough.

    def makeLibrary(libId: String): Library = new Library(libId)

    class Library(libId: String) extends AbstractLibrary(libId) {

      /** Summary stats of the library */
      def summaryStats: Map[String, Any] = Map()

      /** Summary files of the library */
      def summaryFiles: Map[String, File] = Map(
        "alignment" -> mappingJob.outputFiles("finalBamFile")
      )

      /** Convenience method to check whether the library is paired or not */
      def paired: Boolean = mappingJob.flexiprep.paired

      /** Alignment results of this library ~ can only be accessed after addJobs is run! */
      def alnFile: File = mappingJob.outputFiles("finalBamFile")

      /** Per-library mapping job */
      def mappingJob: Mapping = {
        val job = new Mapping(qscript)
        job.sampleId = Option(sampleId)
        job.libId = Option(libId)
        job.outputDir = libDir
        job.input_R1 = config("R1")
        job.input_R2 = config("R2")
        job.init()
        job.biopetScript()
        job
      }

      def addJobs(): Unit = {
        // create per-library alignment file
        addAll(mappingJob.functions)
        qscript.addSummaryQScript(mappingJob)
      }

    }
  }
}

object Gentrap extends PipelineCommand {

  /** Implicit extension that allows to create option values based on boolean values */
  implicit class RichBoolean(val b: Boolean) extends AnyVal {
    final def option[A](a: => A): Option[A] = if (b) Some(a) else None
  }

  /** Enumeration of available expression measures */
  object ExpMeasures extends Enumeration {
    val FragmentsPerGene, FragmentsPerExon, BasesPerGene, BasesPerExon, CufflinksStrict, CufflinksGuided, CufflinksBlind, Cuffquant, Rsem = Value
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