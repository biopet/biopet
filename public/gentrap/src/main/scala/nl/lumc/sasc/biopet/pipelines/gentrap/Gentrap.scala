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

import nl.lumc.sasc.biopet.FullVersion
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.report.ReportBuilderExtension
import nl.lumc.sasc.biopet.extensions.picard.{ MergeSamFiles, SortSam }
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsView
import nl.lumc.sasc.biopet.extensions.tools.{ MergeTables, WipeReads }
import nl.lumc.sasc.biopet.extensions.{ HtseqCount, Ln }
import nl.lumc.sasc.biopet.pipelines.bamtobigwig.Bam2Wig
import nl.lumc.sasc.biopet.pipelines.gentrap.extensions.{ CustomVarScan, Pdflatex, RawBaseCounter }
import nl.lumc.sasc.biopet.pipelines.gentrap.scripts.{ AggrBaseCount, PdfReportTemplateWriter, PlotHeatmap }
import nl.lumc.sasc.biopet.pipelines.mapping.MultisampleMappingTrait
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config._
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function.QFunction
import picard.analysis.directed.RnaSeqMetricsCollector.StrandSpecificity

import scala.language.reflectiveCalls
import scalaz.Scalaz._

/**
 * Gentrap pipeline
 * Generic transcriptome analysis pipeline
 *
 * @author Peter van 't Hof <p.j.van_t_hof@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
class Gentrap(val root: Configurable) extends QScript
  with MultisampleMappingTrait { qscript =>

  import Gentrap.ExpMeasures._
  import Gentrap.StrandProtocol._
  import Gentrap._

  // alternative constructor for initialization with empty configuration
  def this() = this(null)

  override def reportClass: Option[ReportBuilderExtension] = {
    val report = new GentrapReport(this)
    report.outputDir = new File(outputDir, "report")
    report.summaryFile = summaryFile
    Some(report)
  }

  /** Split aligner to use */
  var aligner: String = config("aligner", default = "gsnap")

  /** Expression measurement modes */
  // see the enumeration below for valid modes
  var expMeasures: Set[ExpMeasures.Value] = {
    if (config.contains("expression_measures"))
      config("expression_measures")
        .asStringList
        .flatMap { makeExpMeasure }
        .toSet
    else {
      Logging.addError("'expression_measures' is missing in the config")
      Set()
    }
  }

  /** Strandedness modes */
  var strandProtocol: StrandProtocol.Value = {
    if (config.contains("strand_protocol"))
      makeStrandProtocol(config("strand_protocol").asString).getOrElse(StrandProtocol.NonSpecific)
    else {
      Logging.addError("'strand_protocol' is missing in the config")
      StrandProtocol.NonSpecific
    }
  }

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
  override def defaults = Map(
    "merge_strategy" -> "preprocessmergesam",
    "gsnap" -> Map(
      "novelsplicing" -> 1,
      "batch" -> 4,
      "format" -> "sam"
    ),
    "bammetrics" -> Map(
      "transcript_refflat" -> annotationRefFlat,
      "collectrnaseqmetrics" -> ((if (strandProtocol != null) Map(
        "strand_specificity" -> (strandProtocol match {
          case NonSpecific => StrandSpecificity.NONE.toString
          case Dutp        => StrandSpecificity.SECOND_READ_TRANSCRIPTION_STRAND.toString
          case otherwise   => throw new IllegalStateException(otherwise.toString)
        })
      )
      else Map()) ++ (if (ribosomalRefFlat != null) ribosomalRefFlat.map("ribosomal_intervals" -> _.getAbsolutePath).toList else Nil))
    ),
    "cutadapt" -> Map("minimum_length" -> 20),
    // avoid conflicts when merging since the MarkDuplicate tags often cause merges to fail
    "picard" -> Map(
      "programrecordid" -> "null"
    ),
    // disable markduplicates since it may not play well with all aligners (this can still be overriden via config)
    "mapping" -> Map(
      "skip_markduplicates" -> true,
      "skip_metrics" -> true
    )
  )

  /** Adds output merge jobs for the given expression mode */
  // TODO: can we combine the enum with the file extension (to reduce duplication and potential errors)
  private def makeMergeTableJob(inFunc: (Sample => Option[File]), ext: String, idCols: List[Int], valCol: Int,
                                numHeaderLines: Int = 0, outBaseName: String = "all_samples",
                                fallback: String = "-"): Option[MergeTables] = {
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
        job.numHeaderLines = Option(numHeaderLines)
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
    makeMergeTableJob((s: Sample) => s.geneFragmentsCount, ".fragments_per_gene", List(1), 2, numHeaderLines = 0,
      fallback = "0")

  /** Heatmap job for gene fragment count */
  private lazy val geneFragmentsCountHeatmapJob =
    makeHeatmapJob(geneFragmentsCountJob, "fragments_per_gene", FragmentsPerGene)

  /** Merged exon fragment count table */
  private lazy val exonFragmentsCountJob =
    makeMergeTableJob((s: Sample) => s.exonFragmentsCount, ".fragments_per_exon", List(1), 2, numHeaderLines = 0,
      fallback = "0")

  /** Heatmap job for exon fragment count */
  private lazy val exonFragmentsCountHeatmapJob =
    makeHeatmapJob(exonFragmentsCountJob, "fragments_per_exon", FragmentsPerExon)

  /** Merged gene base count table */
  private lazy val geneBasesCountJob =
    makeMergeTableJob((s: Sample) => s.geneBasesCount, ".bases_per_gene", List(1), 2, numHeaderLines = 1,
      fallback = "0")

  /** Heatmap job for gene base count */
  private lazy val geneBasesCountHeatmapJob =
    makeHeatmapJob(geneBasesCountJob, "bases_per_gene", BasesPerGene)

  /** Merged exon base count table */
  private lazy val exonBasesCountJob =
    makeMergeTableJob((s: Sample) => s.exonBasesCount, ".bases_per_exon", List(1), 2, numHeaderLines = 1,
      fallback = "0")

  /** Heatmap job for exon base count */
  private lazy val exonBasesCountHeatmapJob =
    makeHeatmapJob(exonBasesCountJob, "bases_per_exon", BasesPerExon)

  /** Merged gene FPKM table for Cufflinks, strict mode */
  private lazy val geneFpkmCufflinksStrictJob =
    makeMergeTableJob((s: Sample) => s.geneFpkmCufflinksStrict, ".genes_fpkm_cufflinks_strict", List(1, 7), 10,
      numHeaderLines = 1, fallback = "0.0")

  /** Heatmap job for gene FPKM Cufflinks, strict mode */
  private lazy val geneFpkmCufflinksStrictHeatmapJob =
    makeHeatmapJob(geneFpkmCufflinksStrictJob, "genes_fpkm_cufflinks_strict", CufflinksStrict)

  /** Merged exon FPKM table for Cufflinks, strict mode */
  private lazy val isoFpkmCufflinksStrictJob =
    makeMergeTableJob((s: Sample) => s.isoformFpkmCufflinksStrict, ".isoforms_fpkm_cufflinks_strict", List(1, 7), 10,
      numHeaderLines = 1, fallback = "0.0")

  /** Heatmap job for isoform FPKM Cufflinks, strict mode */
  private lazy val isoFpkmCufflinksStrictHeatmapJob =
    makeHeatmapJob(isoFpkmCufflinksStrictJob, "isoforms_fpkm_cufflinks_strict", CufflinksStrict, isCuffIsoform = true)

  /** Merged gene FPKM table for Cufflinks, guided mode */
  private lazy val geneFpkmCufflinksGuidedJob =
    makeMergeTableJob((s: Sample) => s.geneFpkmCufflinksGuided, ".genes_fpkm_cufflinks_guided", List(1, 7), 10,
      numHeaderLines = 1, fallback = "0.0")

  /** Heatmap job for gene FPKM Cufflinks, guided mode */
  private lazy val geneFpkmCufflinksGuidedHeatmapJob =
    makeHeatmapJob(geneFpkmCufflinksGuidedJob, "genes_fpkm_cufflinks_guided", CufflinksGuided)

  /** Merged isoforms FPKM table for Cufflinks, guided mode */
  private lazy val isoFpkmCufflinksGuidedJob =
    makeMergeTableJob((s: Sample) => s.isoformFpkmCufflinksGuided, ".isoforms_fpkm_cufflinks_guided", List(1, 7), 10,
      numHeaderLines = 1, fallback = "0.0")

  /** Heatmap job for isoform FPKM Cufflinks, guided mode */
  private lazy val isoFpkmCufflinksGuidedHeatmapJob =
    makeHeatmapJob(isoFpkmCufflinksGuidedJob, "isoforms_fpkm_cufflinks_guided", CufflinksGuided, isCuffIsoform = true)

  /** Merged gene FPKM table for Cufflinks, blind mode */
  private lazy val geneFpkmCufflinksBlindJob =
    makeMergeTableJob((s: Sample) => s.geneFpkmCufflinksBlind, ".genes_fpkm_cufflinks_blind", List(1, 7), 10,
      numHeaderLines = 1, fallback = "0.0")

  /** Heatmap job for gene FPKM Cufflinks, blind mode */
  private lazy val geneFpkmCufflinksBlindHeatmapJob =
    makeHeatmapJob(geneFpkmCufflinksBlindJob, "genes_fpkm_cufflinks_blind", CufflinksBlind)

  /** Merged isoforms FPKM table for Cufflinks, blind mode */
  private lazy val isoFpkmCufflinksBlindJob =
    makeMergeTableJob((s: Sample) => s.isoformFpkmCufflinksBlind, ".isoforms_fpkm_cufflinks_blind", List(1, 7), 10,
      numHeaderLines = 1, fallback = "0.0")

  /** Heatmap job for isoform FPKM Cufflinks, blind mode */
  private lazy val isoFpkmCufflinksBlindHeatmapJob =
    makeHeatmapJob(isoFpkmCufflinksBlindJob, "isoforms_fpkm_cufflinks_blind", CufflinksBlind, isCuffIsoform = true)

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

  /** Output summary file */
  def summaryFile: File = new File(outputDir, "gentrap.summary.json")

  /** Files that will be listed in the summary file */
  override def summaryFiles: Map[String, File] = super.summaryFiles ++ Map(
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
  override def summarySettings: Map[String, Any] = super.summarySettings ++ Map(
    "aligner" -> aligner,
    "expression_measures" -> expMeasures.toList.map(_.toString),
    "strand_protocol" -> strandProtocol.toString,
    "call_variants" -> callVariants,
    "remove_ribosomal_reads" -> removeRibosomalReads,
    "version" -> FullVersion
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

  /** Steps to run before biopetScript */
  override def init(): Unit = {
    super.init()

    // TODO: validate that exons are flattened or not (depending on another option flag?)
    // validate required annotation files
    if (expMeasures.contains(FragmentsPerGene) && annotationGtf.isEmpty)
      Logging.addError("GTF file must be defined for counting fragments per gene, config key: 'annotation_gtf'")

    if (expMeasures.contains(FragmentsPerExon) && annotationGtf.isEmpty)
      Logging.addError("GTF file must be defined for counting fragments per exon, config key: 'annotation_gtf'")
    // TODO: validate that GTF file contains exon features

    if (expMeasures.contains(BasesPerGene) && annotationBed.isEmpty)
      Logging.addError("BED file must be defined for counting bases per gene, config key: 'annotation_bed'")

    if (expMeasures.contains(BasesPerExon) && annotationBed.isEmpty)
      Logging.addError("BED file must be defined for counting bases per exon, config key: 'annotation_bed'")

    if ((expMeasures.contains(CufflinksBlind) || expMeasures.contains(CufflinksGuided) || expMeasures.contains(CufflinksStrict)) && annotationGtf.isEmpty)
      Logging.addError("GTF file must be defined for Cufflinks-based modes, config key: 'annotation_gtf'")

    if (removeRibosomalReads && ribosomalRefFlat.isEmpty)
      Logging.addError("rRNA intervals must be supplied if removeRibosomalReads is set, config key: 'ribosome_refflat'")

    annotationGtf.foreach(inputFiles :+= new InputFile(_))
    annotationBed.foreach(inputFiles :+= new InputFile(_))
    ribosomalRefFlat.foreach(inputFiles :+= new InputFile(_))
    if (annotationRefFlat.getName.nonEmpty) inputFiles :+= new InputFile(annotationRefFlat)
  }

  /** Pipeline run for multiple samples */
  override def addMultiSampleJobs(): Unit = {
    super.addMultiSampleJobs
    // merge expression tables
    mergeTableJobs.values.foreach { case maybeJob => maybeJob.foreach(add(_)) }
    // add heatmap jobs
    heatmapJobs.values.foreach { case maybeJob => maybeJob.foreach(add(_)) }
    // plot heatmap for each expression measure if sample is > 1
    if (samples.size > 1) {
      geneFragmentsCountJob
    }
    // TODO: use proper notation
    //add(pdfTemplateJob)
    //add(pdfReportJob)
  }

  /** Returns a [[Sample]] object */
  override def makeSample(sampleId: String): Sample = new Sample(sampleId)

  /**
   * Gentrap sample
   *
   * @param sampleId Unique identifier of the sample
   */
  class Sample(sampleId: String) extends super.Sample(sampleId) with CufflinksProducer {

    /** Shortcut to qscript object */
    protected def pipeline: Gentrap = qscript

    /** Summary stats of the sample */
    override def summaryStats: Map[String, Any] = super.summaryStats ++ Map(
      "all_paired" -> allPaired,
      "all_single" -> allSingle
    )

    /** Summary files of the sample */
    override def summaryFiles: Map[String, File] = super.summaryFiles ++ Map(
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

    /** Per-sample alignment file, post rRNA cleanup (if chosen) */
    lazy val alnFile: File = wipeJob match {
      case Some(j) => j.outputBam
      case None    => preProcessBam.get
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
        job.isIntermediate = true
        job
      }

    /** Read counting job per gene */
    private def fragmentsPerGeneJob: Option[HtseqCount] = expMeasures
      .contains(FragmentsPerGene)
      .option {
        require(idSortingJob.nonEmpty)
        val job = new HtseqCount(qscript)
        annotationGtf.foreach(job.inputAnnotation = _)
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
            job.f = List("0x80")
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

        Option(StrandSeparationJobSet(f1Job, r2Job, combineJob.alnJob))

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
            job.f = List("0x40")
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

        Option(StrandSeparationJobSet(f2Job, r1Job, combineJob.alnJob))

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
            annotationBed.foreach(job.annotationBed = _)
            job.output = createFile(".raw_base_count")
            job
          }
      case Dutp =>
        (expMeasures.contains(BasesPerExon) || expMeasures.contains(BasesPerGene))
          .option {
            require(alnFilePlusStrand.isDefined && alnFileMinusStrand.isDefined)
            val job = new RawBaseCounter(qscript)
            job.inputPlus = alnFilePlusStrand.get
            job.inputMinus = alnFileMinusStrand.get
            annotationBed.foreach(job.annotationBed = _)
            job.output = createFile(".raw_base_count")
            job
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

    /** Job for removing ribosomal reads */
    private def wipeJob: Option[WipeReads] = removeRibosomalReads
      .option {
        //require(ribosomalRefFlat.isDefined)
        val job = new WipeReads(qscript)
        job.inputBam = bamFile.get
        ribosomalRefFlat.foreach(job.intervalFile = _)
        job.outputBam = createFile(".cleaned.bam")
        job.discardedBam = createFile(".rrna.bam")
        job
      }

    /** Super type of Ln and MergeSamFiles */
    case class CombineFileJobSet(alnJob: QFunction { def output: File }, idxJob: Option[Ln]) {
      /** Adds all jobs in this jobset */
      def addAll(): Unit = { add(alnJob); idxJob.foreach(add(_)) }
    }

    /** Ln or MergeSamFile job, depending on how many inputs are supplied */
    private def makeCombineJob(inFiles: List[File], outFile: File,
                               mergeSortOrder: String = "coordinate"): CombineFileJobSet = {
      require(inFiles.nonEmpty, "At least one input files required for combine job")
      if (inFiles.size == 1) {

        val jobBam = new Ln(qscript)
        jobBam.input = inFiles.head.getAbsoluteFile
        jobBam.output = outFile

        val jobIdx = new Ln(qscript)
        jobIdx.input = swapExt(libraries.values.head.libDir, jobBam.input, ".bam", ".bai")
        jobIdx.output = swapExt(sampleDir, jobBam.output, ".bam", ".bai")

        CombineFileJobSet(jobBam, Some(jobIdx))
      } else {
        val job = new MergeSamFiles(qscript)
        job.input = inFiles
        job.output = outFile
        job.sortOrder = mergeSortOrder
        CombineFileJobSet(job, None)
      }
    }

    /** Whether all libraries are paired or not */
    def allPaired: Boolean = libraries.values.forall(_.mapping.forall(_.input_R2.isDefined))

    /** Whether all libraries are single or not */
    def allSingle: Boolean = libraries.values.forall(_.mapping.forall(_.input_R2.isEmpty))

    // TODO: add warnings or other messages for config values that are hard-coded by the pipeline
    /** Adds all jobs for the sample */
    override def addJobs(): Unit = {
      super.addJobs()
      // TODO: this is our requirement since it's easier to calculate base counts when all libraries are either paired or single
      require(allPaired || allSingle, s"Sample $sampleId contains only single-end or paired-end libraries")
      // merge or symlink per-library alignments
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
  private def makeExpMeasure(rawName: String): Option[ExpMeasures.Value] = {
    try {
      Some(ExpMeasures.withName(camelize(rawName)))
    } catch {
      case nse: NoSuchElementException =>
        Logging.addError(s"Invalid expression measure: $rawName")
        None
      case e: Exception => throw e
    }
  }

  /** Conversion from raw user-supplied expression measure string to enum value */
  private def makeStrandProtocol(rawName: String): Option[StrandProtocol.Value] = {
    try {
      Some(StrandProtocol.withName(camelize(rawName)))
    } catch {
      case nse: NoSuchElementException =>
        Logging.addError(s"Invalid strand protocol: $rawName")
        None
      case e: Exception => throw e
    }
  }
}
