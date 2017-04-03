package nl.lumc.sasc.biopet.pipelines.tarmac

import java.io.File

import nl.lumc.sasc.biopet.core.{PedigreeQscript, PipelineCommand, Reference}
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.gatk.DepthOfCoverage
import nl.lumc.sasc.biopet.extensions.wisecondor.WisecondorCount
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function.QFunction

import scalaz.{-\/, \/, \/-}

/**
 * Created by Sander Bollen on 23-3-17.
 */
class Tarmac(val parent: Configurable) extends QScript with PedigreeQscript with SummaryQScript with Reference {
  qscript =>

  private val targets: File = config("targets")
  def this() = this(null)

  def init() = {

  }

  def biopetScript() = {
    addSamplesJobs()
    addSummaryJobs()
  }

  def addMultiSampleJobs() = {

  }

  class Sample(name: String) extends AbstractSample(name) {

    val inputXhmmCountFile: Option[File] = config("xhmm_count_file")
    val inputWisecondorCountFile: Option[File] = config("wisecondor_count_file")
    val bamFile: Option[File] = config("bam")

    /**
     * Create XHMM count file or create link to input count file
     * Precedence is given to existing count files.
     * Returns a disjunction where right is the file, and left is
     * a potential error message
     */
    protected lazy val outputXhmmCountJob: String \/ QFunction = {
      val outFile = new File(sampleDir + File.separator + s"$name.dcov")
      (inputXhmmCountFile, bamFile) match {
        case (Some(f), _) => {
          val ln = new Ln(root)
          ln.input = f
          ln.output = outFile
          \/-(ln)
        }
        case (None, Some(bam)) => {
          val dcov = DepthOfCoverage(root, List(bam), outFile, List(targets))
          \/-(dcov)
        }
        case _ => -\/(s"Cannot find bam file or xhmm count file for sample" +
          s" $name in config. At least one must be given.")
      }

    }

    /* Get count file for Xhmm method */
    lazy val outputXhmmCountFile: String \/ File = {
      outputXhmmCountJob match {
        case \/-(ln: Ln) => \/-(ln.output)
        case \/-(doc: DepthOfCoverage) => \/-(doc.out)
        case _ => _
      }
    }

    /**
     * Create wisecondor count file or create link to input count file.
     * Precedence is given to existing count files.
     * Returns a disjunction where right is the file, and left is
     * a potential error message
     */
    protected lazy val outputWisecondorCountJob: String \/ QFunction = {
      val outFile = new File(sampleDir + File.separator + s"$name.wisecondor.bed")
      (inputWisecondorCountFile, bamFile) match {
        case (Some(f), _) => {
          val ln = new Ln(root)
          ln.input = f
          ln.output = outFile
          \/-(ln)
        }
        case (None, Some(bam)) => {
          val counter = new WisecondorCount(root)
          counter.inputBam = bam
          counter.output = outFile
          counter.binFile = Some(targets)
          \/-(counter)
        }
        case _ => -\/(s"Cannot find bam file or wisecondor count for sample" +
          s" $name. At least one must be given.")
      }

    }

    /* Get count file for wisecondor method */
    lazy val outputWisecondorCountFile: String \/ File = {
      outputWisecondorCountJob match {
        case \/-(ln: Ln) => \/-(ln.output)
        case \/-(count: WisecondorCount) => \/-(count.output)
        case _ => _
      }
    }

    /** Function to add sample jobs */
    def addJobs(): Unit = {
      (outputWisecondorCountJob :: outputXhmmCountJob :: Nil).foreach {
        case -\/(error) => Logging.addError(error)
        case \/-(function)          => add(function)
      }
    }

    /* This is necessary for compile reasons, but library does not in fact exist for this pipeline */
    def makeLibrary(id: String) = new Library(id)

    class Library(id: String) extends AbstractLibrary(id) {
      def addJobs(): Unit = {}
      def summaryFiles: Map[String, File] = Map()
      def summaryStats: Any = Map()
    }
    /** Must return files to store into summary */
    def summaryFiles: Map[String, File] = Map()

    /** Must returns stats to store into summary */
    def summaryStats: Any = Map()
  }

  def makeSample(sampleId: String) = new Sample(sampleId)

  def summarySettings: Map[String, Any] = Map()
  def summaryFiles: Map[String, File] = Map()

  def summaryFile: File = new File(outputDir, "tarmac.summary.json")
}

object Tarmac extends PipelineCommand