package nl.lumc.sasc.biopet.extensions.gatk.broad.gather

import java.io.{ File, FileNotFoundException, PrintStream }
import java.util

import org.apache.commons.collections.CollectionUtils
import org.apache.log4j.Logger
import org.broadinstitute.gatk.engine.recalibration.RecalibrationReport
import org.broadinstitute.gatk.utils.commandline.Gatherer
import org.broadinstitute.gatk.utils.exceptions.{ ReviewedGATKException, UserException }
import org.broadinstitute.gatk.utils.report.GATKReport

import scala.collection.JavaConversions._

/**
 * Created by pjvanthof on 26/04/16.
 */
class BQSRGatherer extends Gatherer {
  private val logger: Logger = Logger.getLogger(classOf[BQSRGatherer])
  private val EMPTY_INPUT_LIST: String = "list of inputs files is empty or there is no usable data in any input file"
  private val MISSING_OUTPUT_FILE: String = "missing output file name"
  private val MISSING_READ_GROUPS: String = "Missing read group(s)"

  def gather(inputs: util.List[File], output: File) {
    val outputFile: PrintStream = try {
      new PrintStream(output)
    } catch {
      case e: FileNotFoundException => {
        throw new UserException.MissingArgument("output", MISSING_OUTPUT_FILE)
      }
    }
    val report: GATKReport = gatherReport(inputs)
    report.print(outputFile)
  }

  def gatherReport(inputs: util.List[File]): GATKReport = {
    val allReadGroups: util.SortedSet[String] = new util.TreeSet[String]
    val inputReadGroups: util.LinkedHashMap[File, util.Set[String]] = new util.LinkedHashMap[File, util.Set[String]]
    for (input <- inputs) {
      val readGroups: util.Set[String] = RecalibrationReport.getReadGroups(input)
      inputReadGroups.put(input, readGroups)
      allReadGroups.addAll(readGroups)
    }
    import scala.collection.JavaConversions._
    for (entry <- inputReadGroups.entrySet) {
      val input: File = entry.getKey
      val readGroups: util.Set[String] = entry.getValue
      if (allReadGroups.size != readGroups.size) {
        logger.info(MISSING_READ_GROUPS + ": " + input.getAbsolutePath)
        import scala.collection.JavaConversions._
        for (readGroup <- CollectionUtils.subtract(allReadGroups, readGroups)) {
          logger.info("  " + readGroup)
        }
      }
    }
    var generalReport: RecalibrationReport = null
    import scala.collection.JavaConversions._
    for (input <- inputs if !new RecalibrationReport(input, allReadGroups).isEmpty) {
      val inputReport: RecalibrationReport = new RecalibrationReport(input, allReadGroups)
      if (generalReport == null) generalReport = inputReport
      else generalReport.combine(inputReport)
    }
    if (generalReport == null) throw new ReviewedGATKException(EMPTY_INPUT_LIST)
    generalReport.calculateQuantizedQualities
    return generalReport.createGATKReport
  }
}
