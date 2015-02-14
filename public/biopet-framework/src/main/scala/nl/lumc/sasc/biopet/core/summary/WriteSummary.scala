package nl.lumc.sasc.biopet.core.summary

import java.io.{PrintWriter, File}

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.function.{ QFunction, InProcessFunction }
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by pjvan_thof on 2/14/15.
 */
class WriteSummary(val root: Configurable) extends InProcessFunction with Configurable {
  this.analysisName = getClass.getSimpleName

  require(root.isInstanceOf[SummaryQScript], "root is not a SummaryQScript")

  val summaryQScript = root.asInstanceOf[SummaryQScript]

  @Input(doc = "deps", required = false)
  var deps: List[File] = Nil

  @Output(doc = "Summary output", required = true)
  var out: File = summaryQScript.summaryFile

  var md5sum: Boolean = config("summary_md5", default = true)
  //TODO: add more checksums types

  override def freezeFieldValues(): Unit = {
    for (q <- summaryQScript.summaryQScripts) deps :+= q.summaryFile
    for ((_, l) <- summaryQScript.summarizables; s <- l) s match {
      case f: QFunction => deps :+= f.firstOutput
      case _            =>
    }
    super.freezeFieldValues()
  }

  def run(): Unit = {
    val writer = new PrintWriter(out)

    writer.close()
  }
}
