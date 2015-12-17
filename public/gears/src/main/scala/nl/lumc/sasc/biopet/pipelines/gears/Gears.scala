package nl.lumc.sasc.biopet.pipelines.gears

import java.io.File

import nl.lumc.sasc.biopet.core.{ PipelineCommand, MultiSampleQScript }
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.qiime.MergeOtuTables
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 03/12/15.
 */
class Gears(val root: Configurable) extends QScript with MultiSampleQScript { qscript =>
  def this() = this(null)

  override def reportClass = {
    val gearsReport = new GearsReport(this)
    gearsReport.outputDir = new File(outputDir, "report")
    gearsReport.summaryFile = summaryFile
    Some(gearsReport)
  }

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
    val closedOtuTables = samples.values.flatMap(_.closedOtuTable).toList
    val closedOtuMaps = samples.values.flatMap(_.closedOtuMap).toList
    require(closedOtuTables.size == closedOtuMaps.size)
    if (closedOtuTables.nonEmpty) {
      val closedDir = new File(outputDir, "qiime_closed_reference")
      val closedOtuTable = new File(closedDir, "closed.biom")
      val closedOtuMap = new File(closedDir, "closed.map.txt")

      if (closedOtuTables.size > 1) {
        val mergeTables = new MergeOtuTables(qscript)
        mergeTables.input = closedOtuTables
        mergeTables.outputFile = closedOtuTable
        add(mergeTables)

        val mergeMaps = new MergeOtuTables(qscript)
        mergeMaps.input = closedOtuMaps
        mergeMaps.outputFile = closedOtuMap
        add(mergeMaps)

      } else {
        add(Ln(qscript, closedOtuMaps.head, closedOtuMap))
        add(Ln(qscript, closedOtuTables.head, closedOtuTable))
      }

      //TODO: Plots

    }
  }

  /**
   * Factory method for Sample class
   * @param id SampleId
   * @return Sample class
   */
  def makeSample(id: String): Sample = new Sample(id)

  class Sample(sampleId: String) extends AbstractSample(sampleId) {
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

    private var _closedOtuTable: Option[File] = _
    def closedOtuTable = _closedOtuTable

    private var _closedOtuMap: Option[File] = _
    def closedOtuMap = _closedOtuMap

    /** Function to add sample jobs */
    protected def addJobs(): Unit = {
      addPerLibJobs()
      val qiimeClosed = libraries.values.flatMap(_.gs.qiimeClosed).toList
      if (qiimeClosed.nonEmpty) {
        _closedOtuTable = Some(new File(sampleDir, "closed.biom"))
        _closedOtuMap = Some(new File(sampleDir, "closed.map.txt"))
        if (qiimeClosed.size > 1) {
          val mergeTables = new MergeOtuTables(qscript)
          mergeTables.input = qiimeClosed.map(_.otuTable).toList
          mergeTables.outputFile = _closedOtuTable.get
          add(mergeTables)

          val mergeMaps = new MergeOtuTables(qscript)
          mergeMaps.input = qiimeClosed.map(_.otuMap).toList
          mergeMaps.outputFile = _closedOtuMap.get
          add(mergeMaps)

        } else {
          add(Ln(qscript, qiimeClosed.head.otuMap, _closedOtuMap.get))
          add(Ln(qscript, qiimeClosed.head.otuTable, _closedOtuTable.get))
        }

        //TODO: Plots
      } else {
        _closedOtuTable = None
        _closedOtuMap = None
      }
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