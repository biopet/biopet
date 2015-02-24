import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.{ MultiSampleQScript, PipelineCommand, BiopetQScript }
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.picard.MarkDuplicates
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 2/24/15.
 */
class Shiva(val root: Configurable) extends QScript with MultiSampleQScript with SummaryQScript {
  qscript =>
  def this() = this(null)

  def init: Unit = {

  }

  def biopetScript: Unit = {
    addSamplesJobs()

    addSummaryJobs
  }

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) {
    def makeLibrary(id: String) = new Library(id)

    class Library(libId: String) extends AbstractLibrary(libId) {

      def preProcess(input: File): Option[File] = None

      def makeMapping = {
        val mapping = new Mapping(qscript)
        mapping.sampleId = Some(sampleId)
        mapping.libId = Some(libId)
        mapping.outputDir = libDir
        (Some(mapping), Some(mapping.finalBamFile), preProcess(mapping.finalBamFile))
      }

      lazy val (mapping, bamFile, preProcessBam): (Option[Mapping], Option[File], Option[File]) =
        (config.contains("R1"), config.contains("bam")) match {
        case (true, _) => makeMapping // Default starting from fastq files
        case (false, true) => // Starting from bam file
          config("bam_to_fastq", default = false).asBoolean match {
            case true => makeMapping // bam file will be converted to fastq
            case false => {
              val file = new File(libDir, sampleId + "-" + libId + ".final.bam")
              (None, Some(file), preProcess(file))
            }
          }
        case _ => {
          logger.warn("Sample: " + sampleId + "  Library: " + libId + ", no reads found")
          (None, None, None)
        }
      }

      def addJobs(): Unit = {

      }
    }

    def doublePreProcess(input: List[File]): File = {
      val md = new MarkDuplicates(qscript)
      md.input = input
      md.output = new File(sampleDir, sampleId + ".dedup.bam")
      md.outputMetrics = new File(sampleDir, sampleId + ".dedup.bam")
      md.output
    }

    def addJobs(): Unit = {
      addPerLibJobs()
    }
  }

  def addMultiSampleJobs(): Unit = {

  }

  def summaryFile = new File(outputDir, "Shiva.summary.json")

  def summarySettings = Map()

  def summaryFiles = Map()
}

object Shiva extends PipelineCommand