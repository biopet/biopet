package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.BiopetQScript.InputFile
import nl.lumc.sasc.biopet.core.{ PipelineCommand, MultiSampleQScript }
import nl.lumc.sasc.biopet.extensions.tools.MergeOtuMaps
import nl.lumc.sasc.biopet.extensions.{ Gzip, Zcat, Ln }
import nl.lumc.sasc.biopet.extensions.qiime.MergeOtuTables
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
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

  override def fixedValues = Map("gearssingle" -> Map("skip_flexiprep" -> true))

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

  def qiimeClosedDir: Option[File] = {
    if (samples.values.flatMap(_.gs.qiimeClosed).nonEmpty) {
      Some(new File(outputDir, "qiime_closed_reference"))
    } else None

  }

  def qiimeClosedOtuTable: Option[File] = qiimeClosedDir.map(new File(_, "otu_table.biom"))
  def qiimeClosedOtuMap: Option[File] = qiimeClosedDir.map(new File(_, "otu_map.txt"))

  /**
   * Method where the multisample jobs should be added, this will be executed only when running the -sample argument is not given.
   */
  def addMultiSampleJobs(): Unit = {
    val gss = samples.values.flatMap(_.gs.qiimeClosed).toList
    val closedOtuTables = gss.map(_.otuTable)
    val closedOtuMaps = gss.map(_.otuMap)
    require(closedOtuTables.size == closedOtuMaps.size)
    if (closedOtuTables.nonEmpty) {
      if (closedOtuTables.size > 1) {
        val mergeTables = new MergeOtuTables(qscript)
        mergeTables.input = closedOtuTables
        mergeTables.outputFile = qiimeClosedOtuTable.get
        add(mergeTables)

        val mergeMaps = new MergeOtuMaps(qscript)
        mergeMaps.input = closedOtuMaps
        mergeMaps.output = qiimeClosedOtuMap.get
        add(mergeMaps)

      } else {
        add(Ln(qscript, closedOtuMaps.head, qiimeClosedOtuMap.get))
        add(Ln(qscript, closedOtuTables.head, qiimeClosedOtuTable.get))
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

      lazy val flexiprep = new Flexiprep(qscript)
      flexiprep.sampleId = Some(sampleId)
      flexiprep.libId = Some(libId)
      flexiprep.input_R1 = config("R1")
      flexiprep.input_R2 = config("R2")
      flexiprep.outputDir = new File(libDir, "flexiprep")

      lazy val gs = new GearsSingle(qscript)
      gs.sampleId = Some(sampleId)
      gs.libId = Some(libId)
      gs.outputDir = libDir

      /** Function that add library jobs */
      protected def addJobs(): Unit = {
        inputFiles :+= InputFile(flexiprep.input_R1, config("R1_md5"))
        flexiprep.input_R2.foreach(inputFiles :+= InputFile(_, config("R2_md5")))
        add(flexiprep)

        gs.fastqR1 = Some(flexiprep.fastqR1Qc)
        gs.fastqR2 = flexiprep.fastqR2Qc
        add(gs)
      }

      /** Must return files to store into summary */
      def summaryFiles: Map[String, File] = Map()

      /** Must returns stats to store into summary */
      def summaryStats = Map()
    }

    lazy val gs = new GearsSingle(qscript)
    gs.sampleId = Some(sampleId)
    gs.outputDir = sampleDir

    /** Function to add sample jobs */
    protected def addJobs(): Unit = {
      addPerLibJobs()

      val flexipreps = libraries.values.map(_.flexiprep).toList

      val mergeR1: File = new File(sampleDir, s"$sampleId.R1.fq.gz")
      add(Zcat(qscript, flexipreps.map(_.fastqR1Qc)) | new Gzip(qscript) > mergeR1)

      val mergeR2 = if (flexipreps.exists(_.paired)) Some(new File(sampleDir, s"$sampleId.R2.fq.gz")) else None
      mergeR2.foreach { file =>
        add(Zcat(qscript, flexipreps.flatMap(_.fastqR2Qc)) | new Gzip(qscript) > file)
      }

      gs.fastqR1 = Some(mergeR1)
      gs.fastqR2 = mergeR2
      add(gs)
    }

    /** Must return files to store into summary */
    def summaryFiles: Map[String, File] = Map()

    /** Must returns stats to store into summary */
    def summaryStats: Any = Map()
  }

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = (
    qiimeClosedOtuTable.map("qiime_closed_otu_table" -> _) ++
    qiimeClosedOtuMap.map("qiime_closed_otu_map" -> _)
  ).toMap
}

object Gears extends PipelineCommand