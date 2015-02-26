package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.pipelines.shiva.{ShivaTrait, Shiva}
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 2/26/15.
 */
class ShivaGatk(val root: Configurable) extends QScript with ShivaTrait {
  def this() = this(null)

  override def makeSample(id: String) = new this.Sample(id)
  class Sample(sampleId: String) extends super.Sample(sampleId) {
    override def makeLibrary(id: String) = new this.Library(id)
    class Library(libId: String) extends super.Library(libId) {
      override def preProcess(input:File): Option[File] = {
        //TODO: add preproces
        None
      }
    }

    override def doublePreProcess(input: List[File], isIntermediate: Boolean = false): Option[File] = {
      if (input.size <= 1) super.doublePreProcess(input)
      else {
        super.doublePreProcess(input)
        //TODO: Add indel realignment
      }
    }
  }

}

object ShivaGatk extends PipelineCommand