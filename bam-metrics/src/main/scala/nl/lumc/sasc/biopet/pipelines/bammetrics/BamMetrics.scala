package nl.lumc.sasc.biopet.pipelines.bammetrics

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.function._
import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.picard._
import org.broadinstitute.sting.queue.function._
import org.broadinstitute.sting.commandline._

class BamMetrics(val root:Configurable) extends QScript with BiopetQScript {
  def this() = this(null)
  
  @Input(doc="Bam File", shortName="BAM",required=true)
  var input_bam: File = _
  
  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
  }
  
  def biopetScript() {
  }
}

object BamMetrics extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/bammetrics/BamMetrics.class"
}
