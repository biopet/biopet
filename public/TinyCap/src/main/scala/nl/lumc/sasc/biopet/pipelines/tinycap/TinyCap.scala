package nl.lumc.sasc.biopet.pipelines.tinycap

import nl.lumc.sasc.biopet.core.{ PipelineCommand, Reference }
import nl.lumc.sasc.biopet.pipelines.mapping.MultisampleMappingTrait
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 12/29/15.
 */
class TinyCap(val root: Configurable) extends QScript with MultisampleMappingTrait with Reference {
  qscript =>
  def this() = this(null)

  override def defaults = Map(
    "merge_strategy" -> "preprocessmergesam",
    "mapping" -> Map("alinger" -> "bowtie"),
    "bowtie" -> Map(
      "chunkmbs" -> 256,
      "seedmms" -> 3,
      "seedlen" -> 25,
      "k" -> 5,
      "best" -> true)
  )

  override def makeSample(id: String) = new Sample(id)

  class Sample(sampleId: String) extends super.Sample(sampleId) {
    override def addJobs(): Unit = {
      super.addJobs()

      bamFile // Merged bam file
      //TODO: count job on small rna
    }
  }

  override def summaryFile = new File(outputDir, "tinycap.summary.json")
}

object TinyCap extends PipelineCommand