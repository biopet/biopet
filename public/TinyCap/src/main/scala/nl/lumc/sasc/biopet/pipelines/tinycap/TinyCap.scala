package nl.lumc.sasc.biopet.pipelines.tinycap

import nl.lumc.sasc.biopet.core.{ PipelineCommand, Reference }
import nl.lumc.sasc.biopet.extensions.HtseqCount
import nl.lumc.sasc.biopet.pipelines.mapping.MultisampleMappingTrait
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 12/29/15.
 * Design based on work from Henk Buermans (e-Mir)
 * Implementation by wyleung started 19/01/16
 */
class TinyCap(val root: Configurable) extends QScript with MultisampleMappingTrait with Reference {
  qscript =>
  def this() = this(null)

  var annotationGff: File = config("annotation_gff")

  override def defaults = Map(
    "merge_strategy" -> "preprocessmergesam",
    "mapping" -> Map("aligner" -> "bowtie"),
    "bowtie" -> Map(
      "chunkmbs" -> 256,
      "seedmms" -> 3,
      "seedlen" -> 25,
      "k" -> 5,
      "best" -> true),
    "sickle" -> Map(
      "lengthThreshold" -> 15
    ),
    "cutadapt" -> Map(
      "minimum_length" -> 15,
      "q" -> 30,
      "default_clip_mode" -> "both",
      "times" -> 2
    )
  )

  override def makeSample(id: String) = new Sample(id)

  class Sample(sampleId: String) extends super.Sample(sampleId) {
    override def addJobs(): Unit = {
      super.addJobs()

      // Do expression counting for miRNA and siRNA
      val htseqCount = new HtseqCount(qscript)
      htseqCount.inputAlignment = bamFile.get
      htseqCount.inputAnnotation = annotationGff
      htseqCount.format = Option("bam")
      htseqCount.stranded = Option("yes")
      htseqCount.output = createFile(".exprcount.tsv")
      add(htseqCount)
    }
  }

  override def summaryFile = new File(outputDir, "tinycap.summary.json")

  override def summaryFiles: Map[String, File] = super.summaryFiles ++ Map(
    "annotationGff" -> annotationGff
  )

  override def addMultiSampleJobs = {
    super.addMultiSampleJobs
  }
}

object TinyCap extends PipelineCommand