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
package nl.lumc.sasc.biopet.pipelines.gentrap

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.annotations.{AnnotationRefFlat, RibosomalRefFlat}
import nl.lumc.sasc.biopet.core.report.ReportBuilderExtension
import nl.lumc.sasc.biopet.extensions.tools.{
  CheckValidateAnnotation,
  RefflatStats,
  ValidateAnnotation,
  WipeReads
}
import nl.lumc.sasc.biopet.pipelines.gentrap.Gentrap.{ExpMeasures, StrandProtocol}
import nl.lumc.sasc.biopet.pipelines.gentrap.measures._
import nl.lumc.sasc.biopet.pipelines.mapping.MultisampleMappingTrait
import nl.lumc.sasc.biopet.pipelines.shiva.ShivaVariantcalling
import nl.lumc.sasc.biopet.utils.{LazyCheck, Logging}
import nl.lumc.sasc.biopet.utils.config._
import nl.lumc.sasc.biopet.utils.camelize
import org.broadinstitute.gatk.queue.QScript
import picard.analysis.directed.RnaSeqMetricsCollector.StrandSpecificity
import java.io.File

import scala.language.reflectiveCalls

/**
  * Gentrap pipeline
  * Generic transcriptome analysis pipeline
  *
  * @author Peter van 't Hof <p.j.van_t_hof@lumc.nl>
  * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
  */
class Gentrap(val parent: Configurable)
    extends QScript
    with MultisampleMappingTrait
    with AnnotationRefFlat
    with RibosomalRefFlat { qscript =>

  // alternative constructor for initialization with empty configuration
  def this() = this(null)

  override def reportClass: Option[ReportBuilderExtension] = {
    val report = new GentrapReport(this)
    report.outputDir = new File(outputDir, "report")
    report.summaryDbFile = summaryDbFile
    Some(report)
  }

  /** Expression measurement modes */
  // see the enumeration below for valid modes
  lazy val expMeasures = new LazyCheck({
    config("expression_measures", default = Nil).asStringList
      .map(value =>
        ExpMeasures.values.find(_.toString == camelize(value)) match {
          case Some(v) => v
          case _ =>
            throw new IllegalArgumentException(s"'$value' is not a valid Expression measurement")
      })
      .toSet
  })

  /** Strandedness modes */
  lazy val strandProtocol = new LazyCheck({
    val value: String = config("strand_protocol")
    StrandProtocol.values.find(_.toString == camelize(value)) match {
      case Some(v) => v
      case other =>
        Logging.addError(s"'$other' is no strand_protocol or strand_protocol is not given")
        StrandProtocol.NonSpecific
    }
  })

  /** Whether to remove rRNA regions or not */
  lazy val removeRibosomalReads: Boolean = config("remove_ribosomal_reads", default = false)

  /** Default pipeline config */
  override def defaults: Map[String, Any] = super.defaults ++ Map(
    "htseqcount" -> (if (strandProtocol.isSet) Map("stranded" -> (strandProtocol() match {
                       case StrandProtocol.NonSpecific => "no"
                       case StrandProtocol.Dutp => "reverse"
                       case otherwise => throw new IllegalStateException(otherwise.toString)
                     }))
                     else Map()),
    "cufflinks" -> (if (strandProtocol.isSet) Map("library_type" -> (strandProtocol() match {
                      case StrandProtocol.NonSpecific => "fr-unstranded"
                      case StrandProtocol.Dutp => "fr-firststrand"
                      case otherwise => throw new IllegalStateException(otherwise.toString)
                    }))
                    else Map()),
    "merge_strategy" -> "preprocessmergesam",
    "gsnap" -> Map(
      "novelsplicing" -> 1,
      "batch" -> 4
    ),
    "shivavariantcalling" -> Map(
      "variantcallers" -> List("varscan_cns_singlesample"),
      "name_prefix" -> "multisample"
    ),
    "bammetrics" -> Map(
      "wgs_metrics" -> false,
      "rna_metrics" -> true,
      "collectrnaseqmetrics" -> (if (strandProtocol.isSet)
                                   Map(
                                     "strand_specificity" -> (strandProtocol() match {
                                       case StrandProtocol.NonSpecific =>
                                         StrandSpecificity.NONE.toString
                                       case StrandProtocol.Dutp =>
                                         StrandSpecificity.SECOND_READ_TRANSCRIPTION_STRAND.toString
                                       case otherwise =>
                                         throw new IllegalStateException(otherwise.toString)
                                     })
                                   )
                                 else Map())
    ),
    "cutadapt" -> Map("minimum_length" -> 20),
    // avoid conflicts when merging since the MarkDuplicate tags often cause merges to fail
    "picard" -> Map(
      "programrecordid" -> "null"
    ),
    // disable markduplicates since it may not play well with all aligners (this can still be overriden via config)
    "mapping" -> Map(
      "aligner" -> "gsnap",
      "skip_markduplicates" -> true
    ),
    "wipereads" -> Map(
      "limit_removal" -> true,
      "no_make_index" -> false
    )
  )

  override def biopetScript(): Unit = {
    val validate = new ValidateAnnotation(this)
    validate.refflatFile = Some(annotationRefFlat.get)
    fragmentsPerGene.foreach(validate.gtfFile :+= _.annotationGtf)
    cufflinksBlind.foreach(validate.gtfFile :+= _.annotationGtf)
    cufflinksGuided.foreach(validate.gtfFile :+= _.annotationGtf)
    cufflinksStrict.foreach(validate.gtfFile :+= _.annotationGtf)
    stringtie.foreach(validate.gtfFile :+= _.annotationGtf)
    validate.jobOutputFile = new File(outputDir, ".validate.annotation.out")
    add(validate)

    val check = new CheckValidateAnnotation(this)
    check.inputLogFile = validate.jobOutputFile
    check.jobOutputFile = new File(outputDir, ".check.validate.out")
    add(check)

    super.biopetScript()
  }

  lazy val fragmentsPerGene: Option[FragmentsPerGene] =
    if (expMeasures().contains(ExpMeasures.FragmentsPerGene))
      Some(new FragmentsPerGene(this))
    else None

  lazy val baseCounts: Option[BaseCounts] =
    if (expMeasures().contains(ExpMeasures.BaseCounts))
      Some(new BaseCounts(this))
    else None

  lazy val stringtie: Option[Stringtie] =
    if (expMeasures().contains(ExpMeasures.Stringtie))
      Some(new Stringtie(this))
    else None

  lazy val cufflinksBlind: Option[CufflinksBlind] =
    if (expMeasures().contains(ExpMeasures.CufflinksBlind))
      Some(new CufflinksBlind(this))
    else None

  lazy val cufflinksGuided: Option[CufflinksGuided] =
    if (expMeasures().contains(ExpMeasures.CufflinksGuided))
      Some(new CufflinksGuided(this))
    else None

  lazy val cufflinksStrict: Option[CufflinksStrict] =
    if (expMeasures().contains(ExpMeasures.CufflinksStrict))
      Some(new CufflinksStrict(this))
    else None

  def executedMeasures: List[QScript with Measurement] =
    (fragmentsPerGene :: baseCounts :: cufflinksBlind ::
      cufflinksGuided :: cufflinksStrict :: stringtie :: Nil).flatten

  /** Whether to do simple variant calling on RNA or not */
  lazy val shivaVariantcalling: Option[ShivaVariantcalling] =
    if (config("call_variants", default = false)) {
      val pipeline = new ShivaVariantcalling(this)
      pipeline.outputDir = new File(outputDir, "variantcalling")
      Some(pipeline)
    } else None

  /** Files that will be listed in the summary file */
  override def summaryFiles: Map[String, File] =
    super.summaryFiles ++ Map(
      "annotation_refflat" -> annotationRefFlat()
    ) ++ Map(
      "ribosome_refflat" -> ribosomalRefFlat()
    ).collect { case (key, Some(value)) => key -> value }

  /** Pipeline settings shown in the summary file */
  override def summarySettings: Map[String, Any] = super.summarySettings ++ Map(
    "expression_measures" -> expMeasures().toList.map(_.toString),
    "strand_protocol" -> strandProtocol().toString,
    "call_variants" -> shivaVariantcalling.isDefined,
    "remove_ribosomal_reads" -> removeRibosomalReads
  )

  /** Steps to run before biopetScript */
  override def init(): Unit = {
    super.init()

    if (expMeasures().isEmpty) Logging.addError("'expression_measures' is missing in the config")
    require(Gentrap.StrandProtocol.values.contains(strandProtocol()))
    if (removeRibosomalReads && ribosomalRefFlat().isEmpty)
      Logging.addError("removeRibosomalReads is enabled but no ribosomalRefFlat is given")

    executedMeasures.foreach(x =>
      x.outputDir = new File(outputDir, "expression_measures" + File.separator + x.name))
  }

  /** Pipeline run for multiple samples */
  override def addMultiSampleJobs(): Unit = {
    super.addMultiSampleJobs()

    val refflatStats = new RefflatStats(this)
    refflatStats.refflatFile = this.annotationRefFlat()
    refflatStats.geneOutput =
      new File(outputDir, "expression_measures" + File.separator + "gene.stats")
    refflatStats.transcriptOutput =
      new File(outputDir, "expression_measures" + File.separator + "transcript.stats")
    refflatStats.exonOutput =
      new File(outputDir, "expression_measures" + File.separator + "exon.stats")
    refflatStats.intronOutput =
      new File(outputDir, "expression_measures" + File.separator + "intron.stats")
    refflatStats.jobOutputFile = new File(outputDir, ".reflatstats.out")
    add(refflatStats)

    // merge expression tables
    executedMeasures.foreach(add)
    shivaVariantcalling.foreach(add)
  }

  /** Returns a [[Sample]] object */
  override def makeSample(sampleId: String): Sample = new Sample(sampleId)

  /**
    * Gentrap sample
    *
    * @param sampleId Unique identifier of the sample
    */
  class Sample(sampleId: String) extends super.Sample(sampleId) {

    /** Summary stats of the sample */
    override def summaryStats: Map[String, Any] = super.summaryStats ++ Map(
      "all_paired" -> allPaired,
      "all_single" -> allSingle
    )

    override lazy val preProcessBam: Option[File] = if (removeRibosomalReads) {
      val job = new WipeReads(qscript)
      job.inputBam = bamFile.get
      ribosomalRefFlat().foreach(job.intervalFile = _)
      job.outputBam = createFile("cleaned.bam")
      job.discardedBam = Some(createFile("rrna.bam"))
      add(job)
      Some(job.outputBam)
    } else bamFile

    /** Whether all libraries are paired or not */
    def allPaired: Boolean = libraries.values.forall(_.mapping.forall(_.inputR2.isDefined))

    /** Whether all libraries are single or not */
    def allSingle: Boolean = libraries.values.forall(_.mapping.forall(_.inputR2.isEmpty))

    /** Adds all jobs for the sample */
    override def addJobs(): Unit = {
      super.addJobs()
      // TODO: this is our requirement since it's easier to calculate base counts when all libraries are either paired or single
      require(allPaired || allSingle,
              s"Sample $sampleId contains only single-end or paired-end libraries")
      // add bigwig output, also per-strand when possible

      preProcessBam.foreach { file =>
        executedMeasures.foreach(_.addBamfile(sampleId, file))
        shivaVariantcalling.foreach(_.inputBams += sampleId -> file)
      }
    }
  }
}

object Gentrap extends PipelineCommand {

  /** Enumeration of available expression measures */
  object ExpMeasures extends Enumeration {
    val FragmentsPerGene, BaseCounts, CufflinksStrict, CufflinksGuided, CufflinksBlind, Stringtie = Value
  }

  /** Enumeration of available strandedness */
  object StrandProtocol extends Enumeration {
    // for now, only non-strand specific and dUTP stranded protocol is supported
    val NonSpecific, Dutp = Value
  }
}
