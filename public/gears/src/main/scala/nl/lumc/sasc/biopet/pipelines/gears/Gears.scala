package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.{PipelineCommand, MultiSampleQScript}
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvanthof on 03/12/15.
  */
class Gears(val root: Configurable) extends QScript with MultiSampleQScript { qscript =>
  def this() = this(null)

  /** Init for pipeline */
  def init(): Unit = {
  }

  /** Name of summary output file */
  def summaryFile: File = new File(outputDir, "gears.summary.json")

  /** Pipeline itself */
  def biopetScript(): Unit = {
    addSamplesJobs()
    addSummaryJobs()
  }

  /**
    * Method where the multisample jobs should be added, this will be executed only when running the -sample argument is not given.
    */
  def addMultiSampleJobs(): Unit = {
  }

  /**
    * Factory method for Sample class
    * @param id SampleId
    * @return Sample class
    */
  def makeSample(id: String): Sample = new Sample(id)

  class Sample(sampleId: String) extends AbstractSample(sampleId) {
    /** Function to add sample jobs */
    protected def addJobs(): Unit = {
      addPerLibJobs()
    }

    /**
      * Factory method for Library class
      * @param id SampleId
      * @return Sample class
      */
    def makeLibrary(id: String): Library = new Library(id)

    class Library(libId: String) extends AbstractLibrary(libId) {
      lazy val gs = new GearsSingle(qscript)
      gs.sampleId = Some(sampleId)
      gs.libId = Some(libId)
      gs.outputDir = libDir
      gs.fastqR1 = config("R1")
      gs.fastqR2 = config("R2")
      gs.bamFile = config("bam")

      /** Function that add library jobs */
      protected def addJobs(): Unit = {
        if (gs.fastqR1.isDefined || gs.bamFile.isDefined) {
          gs.init()
          gs.biopetScript()
          addAll(gs.functions)
          addSummaryQScript(gs)
        } else Logging.addError(s"Sample: '$sampleId',  library: '$libId', No input files found")
      }

      /** Must return files to store into summary */
      def summaryFiles: Map[String, File] = Map()

      /** Must returns stats to store into summary */
      def summaryStats = Map()
    }

    /** Must return files to store into summary */
    def summaryFiles: Map[String, File] = Map()

    /** Must returns stats to store into summary */
    def summaryStats: Any = Map()
  }

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = Map()
}

object Gears extends PipelineCommand