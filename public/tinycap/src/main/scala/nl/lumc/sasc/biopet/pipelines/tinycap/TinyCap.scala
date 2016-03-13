package nl.lumc.sasc.biopet.pipelines.tinycap

import java.io.File

import nl.lumc.sasc.biopet.core.annotations.{ AnnotationGff, AnnotationGtf, AnnotationRefFlat }
import nl.lumc.sasc.biopet.core.report.ReportBuilderExtension
import nl.lumc.sasc.biopet.core.{ PipelineCommand, Reference }
import nl.lumc.sasc.biopet.pipelines.gentrap.measures.{ BaseCounts, FragmentsPerGene }
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
class TinyCap(val root: Configurable) extends QScript
  with MultisampleMappingTrait
  with AnnotationRefFlat
  with AnnotationGff
  with AnnotationGtf
  with Reference {
  qscript =>
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
      "seedlen" -> 25,
      "k" -> 5,
      "best" -> true
    ),
    "sickle" -> Map(
      "lengthThreshold" -> 15
    ),
    "fastqc" -> Map(
      "sensitiveAdapterSearch" -> true
    ),
    "cutadapt" -> Map(
      "error_rate" -> 0.2,
      "minimum_length" -> 15,
      "q" -> 30,
      "default_clip_mode" -> "both",
      "times" -> 2
    )
  )

  lazy val fragmentsPerGene = new FragmentsPerGene(this)
  lazy val fragmentsPerSmallRna = new FragmentsPerSmallRna(this)
  lazy val baseCounts = new BaseCounts(this)

  def executedMeasures = (fragmentsPerGene :: fragmentsPerSmallRna :: baseCounts :: Nil)

  override def init = {
    super.init()
    executedMeasures.foreach(x => x.outputDir = new File(outputDir, "expression_measures" + File.separator + x.name))
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

  override def summaryFile = new File(outputDir, "tinycap.summary.json")

  override def summaryFiles: Map[String, File] = super.summaryFiles ++ Map(
    "annotation_refflat" -> annotationRefFlat(),
    "annotationGtf" -> annotationGtf,
    "annotationGff" -> annotationGff
  )

  override def reportClass: Option[ReportBuilderExtension] = {
    val report = new TinyCapReport(this)
    report.outputDir = new File(outputDir, "report")
    report.summaryFile = summaryFile
    Some(report)
  }

  override def addMultiSampleJobs = {
    super.addMultiSampleJobs
    executedMeasures.foreach(add)
  }
}

object TinyCap extends PipelineCommand