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
import java.util.Properties
import scala.language.reflectiveCalls

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function.QFunction
import picard.analysis.directed.RnaSeqMetricsCollector.StrandSpecificity
import scalaz._, Scalaz._

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.core.summary._
import nl.lumc.sasc.biopet.extensions.{ HtseqCount, Ln }
import nl.lumc.sasc.biopet.extensions.picard.{ CollectRnaSeqMetrics, SortSam, MergeSamFiles }
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsView
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.bamtobigwig.Bam2Wig
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import nl.lumc.sasc.biopet.pipelines.gentrap.extensions.{ CustomVarScan, Pdflatex, RawBaseCounter }
import nl.lumc.sasc.biopet.pipelines.gentrap.scripts.{ AggrBaseCount, PdfReportTemplateWriter, PlotHeatmap }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.tools.{ MergeTables, WipeReads }

/**
 * Gentrap pipeline
 * Generic transcriptome analysis pipeline
 *
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
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
  var expMeasures: Set[ExpMeasures.Value] = config("expression_measures")
    .asStringList
    .map { makeExpMeasure }
    .toSet

  /** Strandedness modes */
  var strandProtocol: StrandProtocol.Value = makeStrandProtocol(config("strand_protocol").asString)

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

  /** Whether to do simple variant calling on RNA or not */
  var callVariants: Boolean = config("call_variants", default = false)

  /** Default pipeline config */
  override def defaults = ConfigUtils.mergeMaps(
    Map(
      "gsnap" -> Map(
        "novelsplicing" -> 1,
        "batch" -> 4,
        "format" -> "sam"
      ),
      "cutadapt" -> Map("minimum_length" -> 20),
      // avoid conflicts when merging since the MarkDuplicate tags often cause merges to fail
      "picard" -> Map(
        "programrecordid" -> "null"
      ),
      // disable markduplicates since it may not play well with all aligners (this can still be overriden via config)
      "mapping" -> Map("skip_markduplicates" -> true)
    ), super.defaults)

  /** Adds output merge jobs for the given expression mode */
  // TODO: can we combine the enum with the file extension (to reduce duplication and potential errors)
  private def makeMergeTableJob(inFunc: (Sample => Option[File]), ext: String, idCols: List[Int], valCol: Int,
                                outBaseName: String = "all_samples", fallback: String = "-"): Option[MergeTables] = {
    val tables = samples.values.map { inFunc }.toList.flatten
    tables.nonEmpty
      .option {
        val job = new MergeTables(qscript)
        job.inputTables = tables
        job.output = new File(outputDir, "expression_estimates" + File.separator + outBaseName + ext)
        job.idColumnIndices = idCols.map(_.toString)
        job.valueColumnIndex = valCol
        job.fileExtension = Option(ext)
        job.fallbackString = Option(fallback)
        // TODO: separate the addition into another function?
        job
      }
  }

  /** Expression measures which are subject to TMM normalization during correlation calculation */
  protected lazy val forTmmNormalization: Set[ExpMeasures.Value] =
    Set(FragmentsPerGene, FragmentsPerExon, BasesPerGene, BasesPerExon)

  /** Returns a QFunction to generate heatmaps */
  private def makeHeatmapJob(mergeJob: Option[MergeTables], outName: String,
                             expMeasure: ExpMeasures.Value, isCuffIsoform: Boolean = false): Option[PlotHeatmap] =
    (mergeJob.isDefined && samples.size > 2)
      .option {
        val job = new PlotHeatmap(qscript)
        job.input = mergeJob.get.output
        job.output = new File(outputDir, "heatmaps" + File.separator + s"heatmap_$outName.png")
        job.tmmNormalize = forTmmNormalization.contains(expMeasure)
        job.useLog = job.tmmNormalize
        job.countType =
          if (expMeasure.toString.startsWith("Cufflinks")) {
            if (isCuffIsoform) Option("CufflinksIsoform")
            else Option("CufflinksGene")
          } else Option(expMeasure.toString)
        job
      }

  /** Merged gene fragment count table */
  private lazy val geneFragmentsCountJob =
    makeMergeTableJob((s: Sample) => s.geneFragmentsCount, ".fragments_per_gene", List(1), 2, fallback = "0")

  /** Heatmap job for gene fragment count */
  private lazy val geneFragmentsCountHeatmapJob =
    makeHeatmapJob(geneFragmentsCountJob, "fragments_per_gene", FragmentsPerGene)

  /** Merged exon fragment count table */
  private lazy val exonFragmentsCountJob =
    makeMergeTableJob((s: Sample) => s.exonFragmentsCount, ".fragments_per_exon", List(1), 2, fallback = "0")

  /** Heatmap job for exon fragment count */
  private lazy val exonFragmentsCountHeatmapJob =
    makeHeatmapJob(exonFragmentsCountJob, "fragments_per_exon", FragmentsPerExon)

  /** Merged gene base count table */
  private lazy val geneBasesCountJob =
    makeMergeTableJob((s: Sample) => s.geneBasesCount, ".bases_per_gene", List(1), 2, fallback = "0")

  /** Heatmap job for gene base count */
  private lazy val geneBasesCountHeatmapJob =
    makeHeatmapJob(geneBasesCountJob, "bases_per_gene", BasesPerGene)

  /** Merged exon base count table */
  private lazy val exonBasesCountJob =
    makeMergeTableJob((s: Sample) => s.exonBasesCount, ".bases_per_exon", List(1), 2, fallback = "0")

  /** Heatmap job for exon base count */
  private lazy val exonBasesCountHeatmapJob =
    makeHeatmapJob(exonBasesCountJob, "bases_per_exon", BasesPerExon)

  /** Merged gene FPKM table for Cufflinks, strict mode */
  private lazy val geneFpkmCufflinksStrictJob =
    makeMergeTableJob((s: Sample) => s.geneFpkmCufflinksStrict, ".genes_fpkm_cufflinks_strict", List(1, 7), 10, fallback = "0.0")

  /** Heatmap job for gene FPKM Cufflinks, strict mode */
  private lazy val geneFpkmCufflinksStrictHeatmapJob =
    makeHeatmapJob(geneFpkmCufflinksStrictJob, "genes_fpkm_cufflinks_strict", CufflinksStrict)

  /** Merged exon FPKM table for Cufflinks, strict mode */
  private lazy val isoFpkmCufflinksStrictJob =
    makeMergeTableJob((s: Sample) => s.isoformFpkmCufflinksStrict, ".isoforms_fpkm_cufflinks_strict", List(1, 7), 10, fallback = "0.0")

  /** Heatmap job for isoform FPKM Cufflinks, strict mode */
  private lazy val isoFpkmCufflinksStrictHeatmapJob =
    makeHeatmapJob(isoFpkmCufflinksStrictJob, "isoforms_fpkm_cufflinks_strict", CufflinksStrict, true)

  /** Merged gene FPKM table for Cufflinks, guided mode */
  private lazy val geneFpkmCufflinksGuidedJob =
    makeMergeTableJob((s: Sample) => s.geneFpkmCufflinksGuided, ".genes_fpkm_cufflinks_guided", List(1, 7), 10, fallback = "0.0")

  /** Heatmap job for gene FPKM Cufflinks, guided mode */
  private lazy val geneFpkmCufflinksGuidedHeatmapJob =
    makeHeatmapJob(geneFpkmCufflinksGuidedJob, "genes_fpkm_cufflinks_guided", CufflinksGuided)

  /** Merged isoforms FPKM table for Cufflinks, guided mode */
  private lazy val isoFpkmCufflinksGuidedJob =
    makeMergeTableJob((s: Sample) => s.isoformFpkmCufflinksGuided, ".isoforms_fpkm_cufflinks_guided", List(1, 7), 10, fallback = "0.0")

  /** Heatmap job for isoform FPKM Cufflinks, guided mode */
  private lazy val isoFpkmCufflinksGuidedHeatmapJob =
    makeHeatmapJob(isoFpkmCufflinksGuidedJob, "isoforms_fpkm_cufflinks_guided", CufflinksGuided, true)

  /** Merged gene FPKM table for Cufflinks, blind mode */
  private lazy val geneFpkmCufflinksBlindJob =
    makeMergeTableJob((s: Sample) => s.geneFpkmCufflinksBlind, ".genes_fpkm_cufflinks_blind", List(1, 7), 10, fallback = "0.0")

  /** Heatmap job for gene FPKM Cufflinks, blind mode */
  private lazy val geneFpkmCufflinksBlindHeatmapJob =
    makeHeatmapJob(geneFpkmCufflinksBlindJob, "genes_fpkm_cufflinks_blind", CufflinksBlind)

  /** Merged isoforms FPKM table for Cufflinks, blind mode */
  private lazy val isoFpkmCufflinksBlindJob =
    makeMergeTableJob((s: Sample) => s.isoformFpkmCufflinksBlind, ".isoforms_fpkm_cufflinks_blind", List(1, 7), 10, fallback = "0.0")

  /** Heatmap job for isoform FPKM Cufflinks, blind mode */
  private lazy val isoFpkmCufflinksBlindHeatmapJob =
    makeHeatmapJob(isoFpkmCufflinksBlindJob, "isoforms_fpkm_cufflinks_blind", CufflinksBlind, true)

  /** Container for merge table jobs */
  private lazy val mergeTableJobs: Map[String, Option[MergeTables]] = Map(
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

  /** Container for heatmap jobs */
  private lazy val heatmapJobs: Map[String, Option[PlotHeatmap]] = Map(
    "gene_fragments_count_heatmap" -> geneFragmentsCountHeatmapJob,
    "exon_fragments_count_heatmap" -> exonFragmentsCountHeatmapJob,
    "gene_bases_count_heatmap" -> geneBasesCountHeatmapJob,
    "exon_bases_count_heatmap" -> exonBasesCountHeatmapJob,
    "gene_fpkm_cufflinks_strict_heatmap" -> geneFpkmCufflinksStrictHeatmapJob,
    "isoform_fpkm_cufflinks_strict_heatmap" -> isoFpkmCufflinksStrictHeatmapJob,
    "gene_fpkm_cufflinks_guided_heatmap" -> geneFpkmCufflinksGuidedHeatmapJob,
    "isoform_fpkm_cufflinks_guided_heatmap" -> isoFpkmCufflinksGuidedHeatmapJob,
    "gene_fpkm_cufflinks_blind_heatmap" -> geneFpkmCufflinksBlindHeatmapJob,
    "isoform_fpkm_cufflinks_blind_heatmap" -> isoFpkmCufflinksBlindHeatmapJob
  )

  private def version: String = {
    val baseVersion = getClass.getPackage.getImplementationVersion
    val commitHash = {
      val prop = new Properties()
      prop.load(getClass.getClassLoader.getResourceAsStream("git.properties"))
      prop.getProperty("git.commit.id.abbrev")
    }
    s"$baseVersion ($commitHash)"
  }

  /** Output summary file */
  def summaryFile: File = new File(outputDir, "gentrap.summary.json")

  /** Files that will be listed in the summary file */
  def summaryFiles: Map[String, File] = Map(
    "annotation_refflat" -> annotationRefFlat
  ) ++ Map(
      "annotation_gtf" -> annotationGtf,
      "annotation_bed" -> annotationBed,
      "ribosome_refflat" -> ribosomalRefFlat
    ).collect { case (key, Some(value)) => key -> value } ++
      mergeTableJobs.collect { case (key, Some(value)) => key -> value.output } ++
      heatmapJobs.collect { case (key, Some(value)) => key -> value.output }

  /** Statistics shown in the summary file */
  def summaryStats: Map[String, Any] = Map()

  /** Pipeline settings shown in the summary file */
  def summarySettings: Map[String, Any] = Map(
    "aligner" -> aligner,
    "expression_measures" -> expMeasures.toList.map(_.toString),
    "strand_protocol" -> strandProtocol.toString,
    "call_variants" -> callVariants,
    "remove_ribosomal_reads" -> removeRibosomalReads,
    "version" -> version
  )

  /** Job for writing PDF report template */
  protected lazy val pdfTemplateJob: PdfReportTemplateWriter = {
    val job = new PdfReportTemplateWriter(qscript)
    job.summaryFile = summaryFile
    job.output = new File(outputDir, "gentrap_report.tex")
    job
  }

  /** Job for writing PDF report */
  protected def pdfReportJob: Pdflatex = {
    val job = new Pdflatex(qscript)
    job.input = pdfTemplateJob.output
    job.outputDir = new File(outputDir, "report")
    job.name = "gentrap_report"
    job
  }

  /** General function to create CollectRnaSeqMetrics job, for per-sample and per-library runs */
  protected def makeCollectRnaSeqMetricsJob(alnFile: File, outMetrics: File,
                                            outChart: Option[File] = None): CollectRnaSeqMetrics = {
    val job = new CollectRnaSeqMetrics(qscript)
    job.input = alnFile
    job.output = outMetrics
    job.refFlat = annotationRefFlat
    job.chartOutput = outChart
    job.assumeSorted = true
    job.strandSpecificity = strandProtocol match {
      case NonSpecific => Option(StrandSpecificity.NONE.toString)
      case Dutp        => Option(StrandSpecificity.SECOND_READ_TRANSCRIPTION_STRAND.toString)
      case _           => throw new IllegalStateException
    }
    job.ribosomalIntervals = ribosomalRefFlat
    job
  }

  // used to ensure that the required .dict file is present before the run starts
  // can not store it in config since the tools that use it (Picard) have this value based on the reference file name
  protected def checkDictFile(): Unit = {
    val refFile: File = config("reference")
    val refName: String = refFile.getName
    require(refName.contains('.'), "Reference file must have an extension")
    val dictFile = new File(Option(refFile.getParentFile).getOrElse(new File(".")),
      refName.take(refName.lastIndexOf('.')) + ".dict")
    require(dictFile.exists, s"Dict file '$dictFile' must exist")
  }

  /** Steps to run before biopetScript */
  def init(): Unit = {
    checkDictFile()

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

  /** Pipeline run for each sample */
  def biopetScript(): Unit = {
    addSamplesJobs()
  }

  /** Pipeline run for multiple samples */
  def addMultiSampleJobs(): Unit = {
    // merge expression tables
    mergeTableJobs.values.foreach { case maybeJob => maybeJob.foreach(add(_)) }
    // add heatmap jobs
    heatmapJobs.values.foreach { case maybeJob => maybeJob.foreach(add(_)) }
    // plot heatmap for each expression measure if sample is > 1
    if (samples.size > 1) {
      geneFragmentsCountJob
    }
    // TODO: use proper notation
    addSummaryJobs
    add(pdfTemplateJob)
    add(pdfReportJob)
  }

  /** Returns a [[Sample]] object */
  def makeSample(sampleId: String): Sample = new Sample(sampleId)

  /**
   * Gentrap sample
   *
   * @param sampleId Unique identifier of the sample
   */
  class Sample(sampleId: String) extends AbstractSample(sampleId) with CufflinksProducer {

    /** Shortcut to qscript object */
    protected def pipeline: Gentrap = qscript

    /** Sample output directory */
    override def sampleDir: File = new File(outputDir, "sample_" + sampleId)

    /** Summary stats of the sample */
    def summaryStats: Map[String, Any] = Map(
      "all_paired" -> allPaired,
      "all_single" -> allSingle
    )

    /** Summary files of the sample */
    def summaryFiles: Map[String, File] = Map(
      "alignment" -> alnFile
    ) ++ Map(
        "gene_fragments_count" -> geneFragmentsCount,
        "exon_fragments_count" -> exonFragmentsCount,
        "gene_bases_count" -> geneBasesCount,
        "exon_bases_count" -> exonBasesCount,
        "gene_fpkm_cufflinks_strict" -> cufflinksStrictJobSet.collect { case js => js.geneFpkmJob.output },
        "isoform_fpkm_cufflinks_strict" -> cufflinksStrictJobSet.collect { case js => js.isoformFpkmJob.output },
        "gene_fpkm_cufflinks_guided" -> cufflinksGuidedJobSet.collect { case js => js.geneFpkmJob.output },
        "isoform_fpkm_cufflinks_guided" -> cufflinksGuidedJobSet.collect { case js => js.isoformFpkmJob.output },
        "gene_fpkm_cufflinks_blind" -> cufflinksBlindJobSet.collect { case js => js.geneFpkmJob.output },
        "isoform_fpkm_cufflinks_blind" -> cufflinksBlindJobSet.collect { case js => js.isoformFpkmJob.output },
        "variant_calls" -> variantCalls
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

    /** JobSet for Cufflinks strict mode */
    protected lazy val cufflinksStrictJobSet: Option[CufflinksJobSet] = expMeasures
      .find(_ == CufflinksStrict)
      .collect { case found => new CufflinksJobSet(found) }

    /** Gene tracking file from Cufflinks strict mode */
    def geneFpkmCufflinksStrict: Option[File] = cufflinksStrictJobSet
      .collect { case jobSet => jobSet.geneFpkmJob.output }

    /** Isoforms tracking file from Cufflinks strict mode */
    def isoformFpkmCufflinksStrict: Option[File] = cufflinksStrictJobSet
      .collect { case jobSet => jobSet.isoformFpkmJob.output }

    /** JobSet for Cufflinks strict mode */
    protected lazy val cufflinksGuidedJobSet: Option[CufflinksJobSet] = expMeasures
      .find(_ == CufflinksGuided)
      .collect { case found => new CufflinksJobSet(found) }

    /** Gene tracking file from Cufflinks guided mode */
    def geneFpkmCufflinksGuided: Option[File] = cufflinksGuidedJobSet
      .collect { case jobSet => jobSet.geneFpkmJob.output }

    /** Isoforms tracking file from Cufflinks guided mode */
    def isoformFpkmCufflinksGuided: Option[File] = cufflinksGuidedJobSet
      .collect { case jobSet => jobSet.isoformFpkmJob.output }

    /** JobSet for Cufflinks blind mode */
    protected lazy val cufflinksBlindJobSet: Option[CufflinksJobSet] = expMeasures
      .find(_ == CufflinksBlind)
      .collect { case found => new CufflinksJobSet(found) }

    /** Gene tracking file from Cufflinks guided mode */
    def geneFpkmCufflinksBlind: Option[File] = cufflinksBlindJobSet
      .collect { case jobSet => jobSet.geneFpkmJob.output }

    /** Isoforms tracking file from Cufflinks blind mode */
    def isoformFpkmCufflinksBlind: Option[File] = cufflinksBlindJobSet
      .collect { case jobSet => jobSet.isoformFpkmJob.output }

    /** Raw variant calling file */
    def variantCalls: Option[File] = varCallJob
      .collect { case job => job.output }

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
        // We are forcing the sort order to be ID-sorted, since HTSeq-count often chokes when using position-sorting due
        // to its buffer not being large enough.
        job.order = Option("name")
        job.stranded = strandProtocol match {
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
        job.stranded = strandProtocol match {
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

    /** Alignment file of reads from the plus strand, only defined when run is strand-specific */
    def alnFilePlusStrand: Option[File] = alnPlusStrandJobs
      .collect { case jobSet => jobSet.combineJob.output }

    /** Jobs for generating reads from the plus strand, only defined when run is strand-specific */
    private def alnPlusStrandJobs: Option[StrandSeparationJobSet] = strandProtocol match {
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
          case Some(r2j) => List(f1Job.output, r2j.output)
          case None      => List(f1Job.output)
        }
        val combineJob = makeCombineJob(perStrandFiles, createFile(".plus_strand.bam"))

        Option(StrandSeparationJobSet(f1Job, r2Job, combineJob))

      case NonSpecific => None
      case _           => throw new IllegalStateException
    }

    /** Alignment file of reads from the minus strand, only defined when run is strand-specific */
    def alnFileMinusStrand: Option[File] = alnMinusStrandJobs
      .collect { case jobSet => jobSet.combineJob.output }

    /** Jobs for generating reads from the minus, only defined when run is strand-specific */
    private def alnMinusStrandJobs: Option[StrandSeparationJobSet] = strandProtocol match {
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
          case Some(r1j) => List(f2Job.output, r1j.output)
          case None      => List(f2Job.output)
        }
        val combineJob = makeCombineJob(perStrandFiles, createFile(".minus_strand.bam"))

        Option(StrandSeparationJobSet(f2Job, r1Job, combineJob))

      case NonSpecific => None
      case _           => throw new IllegalStateException
    }
    /** Raw base counting job */
    private def rawBaseCountJob: Option[RawBaseCounter] = strandProtocol match {
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

    /** Variant calling job */
    private def varCallJob: Option[CustomVarScan] = callVariants
      .option {
        val job = new CustomVarScan(qscript)
        job.input = alnFile
        job.output = createFile(".raw.vcf.gz")
        job
      }

    /** General metrics job, only when library > 1 */
    private lazy val bamMetricsModule: Option[BamMetrics] = (libraries.size > 1)
      .option {
        val mod = new BamMetrics(qscript)
        mod.inputBam = alnFile
        mod.outputDir = new File(sampleDir, "metrics")
        mod.sampleId = Option(sampleId)
        mod
      }

    /** Picard CollectRnaSeqMetrics job, only when library > 1 */
    private lazy val collectRnaSeqMetricsJob: Option[CollectRnaSeqMetrics] = (libraries.size > 1)
      .option {
        makeCollectRnaSeqMetricsJob(alnFileDirty, createFile(".rna_metrics"), Option(createFile(".coverage_bias.pdf")))
      }

    /** Job for removing ribosomal reads */
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

    /** Super type of Ln and MergeSamFiles */
    private type CombineFileFunction = QFunction { def output: File }

    /** Ln or MergeSamFile job, depending on how many inputs are supplied */
    private def makeCombineJob(inFiles: List[File], outFile: File,
                               mergeSortOrder: String = "coordinate"): CombineFileFunction = {
      require(inFiles.nonEmpty, "At least one input files for combine job")
      if (inFiles.size == 1) {
        val job = new Ln(qscript)
        job.input = inFiles.head
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
    /** Adds all jobs for the sample */
    def addJobs(): Unit = {
      // TODO: this is our requirement since it's easier to calculate base counts when all libraries are either paired or single
      require(allPaired || allSingle, s"Sample $sampleId contains only single-end or paired-end libraries")
      // add per-library jobs
      addPerLibJobs()
      // merge or symlink per-library alignments
      add(sampleAlnJob)
      // general RNA-seq metrics, if there are > 1 library
      collectRnaSeqMetricsJob match {
        case Some(j) =>
          add(j); addSummarizable(j, "rna_metrics")
        case None => ;
      }
      bamMetricsModule match {
        case Some(m) =>
          m.init()
          m.biopetScript()
          addAll(m.functions)
          addSummaryQScript(m)
        case None => ;
      }
      // add bigwig output, also per-strand when possible
      addAll(Bam2Wig(qscript, alnFile).functions)
      alnFilePlusStrand.collect { case f => addAll(Bam2Wig(qscript, f).functions) }
      alnFileMinusStrand.collect { case f => addAll(Bam2Wig(qscript, f).functions) }
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
      cufflinksStrictJobSet.foreach(_.jobs.foreach(add(_)))
      cufflinksGuidedJobSet.foreach(_.jobs.foreach(add(_)))
      cufflinksBlindJobSet.foreach(_.jobs.foreach(add(_)))
      // add variant calling job if requested
      varCallJob.foreach(add(_))
    }

    /** Returns a [[Library]] object */
    def makeLibrary(libId: String): Library = new Library(libId)

    /**
     * Gentrap library
     *
     * @param libId Unique identifier of the library
     */
    class Library(libId: String) extends AbstractLibrary(libId) {

      /** Summary stats of the library */
      def summaryStats: Map[String, Any] = Map()

      /** Summary files of the library */
      def summaryFiles: Map[String, File] = Map(
        "alignment" -> mappingJob.outputFiles("finalBamFile")
      )

      /** Convenience method to check whether the library is paired or not */
      def paired: Boolean = config.contains("R2")

      /** Alignment results of this library ~ can only be accessed after addJobs is run! */
      def alnFile: File = mappingJob.outputFiles("finalBamFile")

      /** Library-level RNA-seq metrics job, only when we have more than 1 library in the sample */
      def collectRnaSeqMetricsJob: CollectRnaSeqMetrics =
        makeCollectRnaSeqMetricsJob(alnFile, createFile(".rna_metrics"), Option(createFile(".coverage_bias.pdf")))

      /** Wiggle track job */
      private lazy val bam2wigModule: Bam2Wig = Bam2Wig(qscript, alnFile)

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

      /** Adds all jobs for the library */
      def addJobs(): Unit = {
        // create per-library alignment file
        addAll(mappingJob.functions)
        // add bigwig track
        addAll(bam2wigModule.functions)
        // create RNA metrics job
        add(collectRnaSeqMetricsJob)
        addSummarizable(collectRnaSeqMetricsJob, "rna_metrics")
        qscript.addSummaryQScript(mappingJob)
      }

    }
  }
}

object Gentrap extends PipelineCommand {

  /** Enumeration of available expression measures */
  object ExpMeasures extends Enumeration {
    val FragmentsPerGene, FragmentsPerExon, BasesPerGene, BasesPerExon, CufflinksStrict, CufflinksGuided, CufflinksBlind = Value
    //Cuffquant,
    //Rsem = Value
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