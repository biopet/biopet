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
package nl.lumc.sasc.biopet.pipelines.tinycap

import java.io.File

import nl.lumc.sasc.biopet.core.annotations.{AnnotationGff, AnnotationGtf, AnnotationRefFlat}
import nl.lumc.sasc.biopet.core.report.ReportBuilderExtension
import nl.lumc.sasc.biopet.core.{PipelineCommand, Reference}
import nl.lumc.sasc.biopet.pipelines.gentrap.measures.{BaseCounts, FragmentsPerGene}
import nl.lumc.sasc.biopet.pipelines.mapping.MultisampleMappingTrait
import nl.lumc.sasc.biopet.pipelines.tinycap.measures.FragmentsPerSmallRna
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript
import picard.analysis.directed.RnaSeqMetricsCollector.StrandSpecificity

/**
  * Created by pjvan_thof on 12/29/15.
  * Design based on work from Henk Buermans (e-Mir)
  * Implementation by wyleung started 19/01/16
  */
class TinyCap(val parent: Configurable)
    extends QScript
    with MultisampleMappingTrait
    with AnnotationRefFlat
    with AnnotationGff
    with AnnotationGtf
    with Reference { qscript =>
  def this() = this(null)

  var annotateSam: Boolean = config("annotate_sam", default = false)

  override def defaults = Map(
    "igvtoolscount" -> Map(
      "strands" -> "reads",
      "includeDuplicates" -> true
    ),
    "merge_strategy" -> "preprocessmergesam",
    "keep_merged_files" -> true,
    "mapping" -> Map(
      "aligner" -> "bowtie",
      "generate_wig" -> true,
      "skip_markduplicates" -> true
    ),
    "bammetrics" -> Map(
      "wgs_metrics" -> false,
      "rna_metrics" -> false,
      "collectrnaseqmetrics" -> Map(
        "strand_specificity" -> StrandSpecificity.SECOND_READ_TRANSCRIPTION_STRAND.toString
      )
    ),
    "bowtie" -> Map(
      "chunkmbs" -> 256,
      "seedmms" -> 3,
      "seedlen" -> 28,
      "k" -> 3,
      "best" -> true,
      "strata" -> true
    ),
    "sickle" -> Map(
      "lengthThreshold" -> 15
    ),
    "fastqc" -> Map(
      "sensitiveAdapterSearch" -> false
    ),
    "cutadapt" -> Map(
      "error_rate" -> 0.1,
      "minimum_length" -> 15,
      "q" -> 30,
      "default_clip_mode" -> "3",
      "times" -> 1,
      "ignore_fastqc_adapters" -> true
    )
  )

  lazy val fragmentsPerGene = new FragmentsPerGene(this)
  lazy val fragmentsPerSmallRna = new FragmentsPerSmallRna(this)
  lazy val baseCounts = new BaseCounts(this)

  def executedMeasures = (fragmentsPerGene :: fragmentsPerSmallRna :: baseCounts :: Nil)

  override def init = {
    super.init()
    executedMeasures.foreach(x =>
      x.outputDir = new File(outputDir, "expression_measures" + File.separator + x.name))
  }

  override def makeSample(id: String) = new Sample(id)

  class Sample(sampleId: String) extends super.Sample(sampleId) {
    override def addJobs(): Unit = {
      super.addJobs()

      preProcessBam.foreach { file =>
        executedMeasures.foreach(_.addBamfile(sampleId, file))
      }
    }
  }

  override def summaryFiles: Map[String, File] = super.summaryFiles ++ Map(
    "annotation_refflat" -> annotationRefFlat(),
    "annotationGtf" -> annotationGtf,
    "annotationGff" -> annotationGff
  )

  override def reportClass: Option[ReportBuilderExtension] = {
    val report = new TinyCapReport(this)
    report.outputDir = new File(outputDir, "report")
    report.summaryDbFile = summaryDbFile
    Some(report)
  }

  override def addMultiSampleJobs = {
    super.addMultiSampleJobs
    executedMeasures.foreach(add)
  }
}

object TinyCap extends PipelineCommand
