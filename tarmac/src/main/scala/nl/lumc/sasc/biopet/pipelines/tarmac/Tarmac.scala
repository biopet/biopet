package nl.lumc.sasc.biopet.pipelines.tarmac

import java.io.File

import nl.lumc.sasc.biopet.core.{ PedigreeQscript, PipelineCommand, Reference }
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by Sander Bollen on 23-3-17.
 */
class Tarmac(val root: Configurable) extends QScript with PedigreeQscript with SummaryQScript with Reference {
  qscript =>
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

    val inputCountFile: Option[File] = config("count_file")
    val bamFile: Option[File] = config("bam")

    /** Function to add sample jobs */
    def addJobs(): Unit = {}

    /* This is necesary for compile reasons, but library does not in fact exist for this pipeline */
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