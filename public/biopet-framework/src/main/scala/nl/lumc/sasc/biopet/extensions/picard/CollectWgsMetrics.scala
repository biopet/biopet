package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.summary.Summarizable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Output, Input }

/**
 * Created by pjvan_thof on 4/16/15.
 */
class CollectWgsMetrics(val root: Configurable) extends Picard with Summarizable with Reference {

  javaMainClass = new picard.analysis.CollectWgsMetrics().getClass.getName

  @Input(doc = "The input SAM or BAM files to analyze", required = true)
  var input: File = null

  @Output(doc = "Metrics file", required = true)
  var output: File = null

  @Input(doc = "Reference", required = true)
  var reference: File = null

  @Argument(doc = "MINIMUM_MAPPING_QUALITY", required = false)
  var minMapQ: Option[Int] = config("minimum_mapping_quality")

  @Argument(doc = "MINIMUM_BASE_QUALITY", required = false)
  var minBaseQ: Option[Int] = config("minimum_base_quality")

  @Argument(doc = "COVERAGE_CAP", required = false)
  var covCap: Option[Int] = config("coverage_cap")

  @Argument(doc = "STOP_AFTER", required = false)
  var stopAfter: Option[Long] = config("stop_after")

  @Argument(doc = "INCLUDE_BQ_HISTOGRAM", required = false)
  var includeBqHistogram: Boolean = config("include_bq_histogram", default = false)

  override def beforeGraph {
    super.beforeGraph
    if (reference == null) reference = referenceFasta()
  }

  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false) +
    required("REFERENCE_SEQUENCE=", reference, spaceSeparated = false) +
    optional("MINIMUM_MAPPING_QUALITY=", minMapQ, spaceSeparated = false) +
    optional("MINIMUM_BASE_QUALITY=", minBaseQ, spaceSeparated = false) +
    optional("COVERAGE_CAP=", covCap, spaceSeparated = false) +
    optional("STOP_AFTER=", stopAfter, spaceSeparated = false) +
    conditional(includeBqHistogram, "INCLUDE_BQ_HISTOGRAM=true")

  /** Returns files for summary */
  def summaryFiles: Map[String, File] = Map()

  /** Returns stats for summary */
  def summaryStats = Map(
    "metrics" -> Picard.getMetrics(output),
    "histogram" -> Picard.getHistogram(output)
  )
}
