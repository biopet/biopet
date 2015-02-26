package nl.lumc.sasc.biopet.pipelines.shiva

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.{MultiSampleQScript, PipelineCommand}
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.picard.{AddOrReplaceReadGroups, SamToFastq, MarkDuplicates}
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import org.broadinstitute.gatk.queue.QScript
import scala.collection.JavaConversions._

/**
 * Created by pjvan_thof on 2/24/15.
 */
class Shiva(val root: Configurable) extends QScript with ShivaTrait {
  def this() = this(null)
}

object Shiva extends PipelineCommand