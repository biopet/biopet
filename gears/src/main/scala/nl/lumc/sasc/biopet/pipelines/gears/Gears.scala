/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.BiopetQScript.InputFile
import nl.lumc.sasc.biopet.core.{ MultiSampleQScript, PipelineCommand }
import nl.lumc.sasc.biopet.extensions.tools.MergeOtuMaps
import nl.lumc.sasc.biopet.extensions.{ Gzip, Ln, Zcat }
import nl.lumc.sasc.biopet.extensions.qiime.MergeOtuTables
import nl.lumc.sasc.biopet.extensions.seqtk.SeqtkSample
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 03/12/15.
 */
class Gears(val parent: Configurable) extends QScript with MultiSampleQScript { qscript =>
  def this() = this(null)

  override def reportClass = {
    val gearsReport = new GearsReport(this)
    gearsReport.outputDir = new File(outputDir, "report")
    gearsReport.summaryDbFile = summaryDbFile
    Some(gearsReport)
  }

  override def defaults = Map("mergeotumaps" -> Map("skip_prefix" -> "New."))

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
    if (samples.values.flatMap(_.gearsSingle.qiimeClosed).nonEmpty) {
      Some(new File(outputDir, "qiime_closed_reference"))
    } else None
  }

  def qiimeOpenDir: Option[File] = {
    if (samples.values.flatMap(_.gearsSingle.qiimeOpen).nonEmpty) {
      Some(new File(outputDir, "qiime_open_reference"))
    } else None
  }

  def qiimeClosedOtuTable: Option[File] = qiimeClosedDir.map(new File(_, "otu_table.biom"))
  def qiimeClosedOtuMap: Option[File] = qiimeClosedDir.map(new File(_, "otu_map.txt"))

  def qiimeOpenOtuTable: Option[File] = qiimeOpenDir.map(new File(_, "otu_table.biom"))
  def qiimeOpenOtuMap: Option[File] = qiimeOpenDir.map(new File(_, "otu_map.txt"))

  /**
   * Method where the multisample jobs should be added, this will be executed only when running the -sample argument is not given.
   */
  def addMultiSampleJobs(): Unit = {
    val qiimeCloseds = samples.values.flatMap(_.gearsSingle.qiimeClosed).toList
    val closedOtuTables = qiimeCloseds.map(_.otuTable)
    val closedOtuMaps = qiimeCloseds.map(_.otuMap)
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
    }

    val qiimeOpens = samples.values.flatMap(_.gearsSingle.qiimeOpen).toList
    val openOtuTables = qiimeOpens.map(_.otuTable)
    val openOtuMaps = qiimeOpens.map(_.otuMap)
    require(openOtuTables.size == openOtuMaps.size)
    if (openOtuTables.nonEmpty) {
      if (openOtuTables.size > 1) {
        val mergeTables = new MergeOtuTables(qscript)
        mergeTables.input = openOtuTables
        mergeTables.outputFile = qiimeOpenOtuTable.get
        add(mergeTables)

        val mergeMaps = new MergeOtuMaps(qscript)
        mergeMaps.input = openOtuMaps
        mergeMaps.output = qiimeOpenOtuMap.get
        add(mergeMaps)

      } else {
        add(Ln(qscript, openOtuMaps.head, qiimeOpenOtuMap.get))
        add(Ln(qscript, openOtuTables.head, qiimeOpenOtuTable.get))
      }

    }
  }

  /**
   * Factory method for Sample class
   *
   * @param id SampleId
   * @return Sample class
   */
  def makeSample(id: String): Sample = new Sample(id)

  class Sample(sampleId: String) extends AbstractSample(sampleId) {
    /**
     * Factory method for Library class
     *
     * @param id SampleId
     * @return Sample class
     */
    def makeLibrary(id: String): Library = new Library(id)

    class Library(libId: String) extends AbstractLibrary(libId) {

      lazy val inputR1: File = config("R1")
      lazy val inputR2: Option[File] = config("R2")

      lazy val skipFlexiprep: Boolean = config("skip_flexiprep", default = false)

      lazy val flexiprep = if (skipFlexiprep) None else Some(new Flexiprep(qscript))
      flexiprep.foreach(_.sampleId = Some(sampleId))
      flexiprep.foreach(_.libId = Some(libId))
      flexiprep.foreach(_.inputR1 = inputR1)
      flexiprep.foreach(_.inputR2 = inputR2)
      flexiprep.foreach(_.outputDir = new File(libDir, "flexiprep"))

      lazy val qcR1: File = flexiprep.map(_.fastqR1Qc).getOrElse(inputR1)
      lazy val qcR2: Option[File] = flexiprep.map(_.fastqR2Qc).getOrElse(inputR2)

      val libraryGears: Boolean = config("library_gears", default = false)

      lazy val gearsSingle = if (libraryGears) Some(new GearsSingle(qscript)) else None

      /** Function that add library jobs */
      protected def addJobs(): Unit = {
        inputFiles :+= InputFile(inputR1, config("R1_md5"))
        inputR2.foreach(inputFiles :+= InputFile(_, config("R2_md5")))
        flexiprep.foreach(add(_))

        gearsSingle.foreach { gs =>
          gs.sampleId = Some(sampleId)
          gs.libId = Some(libId)
          gs.outputDir = libDir

          gs.fastqR1 = List(addDownsample(qcR1, gs.outputDir))
          gs.fastqR2 = qcR2.map(addDownsample(_, gs.outputDir)).toList
          add(gs)
        }
      }

      /** Must return files to store into summary */
      def summaryFiles: Map[String, File] = Map()

      /** Must returns stats to store into summary */
      def summaryStats = Map()
    }

    lazy val gearsSingle = new GearsSingle(qscript)
    gearsSingle.sampleId = Some(sampleId)
    gearsSingle.outputDir = sampleDir

    /** Function to add sample jobs */
    protected def addJobs(): Unit = {
      addPerLibJobs()

      val flexipreps = libraries.values.map(_.flexiprep).toList

      val mergeR1: File = new File(sampleDir, s"$sampleId.R1.fq.gz")
      add(Zcat(qscript, libraries.values.map(_.qcR1).toList) | new Gzip(qscript) > mergeR1)

      val mergeR2 = if (libraries.values.exists(_.inputR2.isDefined)) Some(new File(sampleDir, s"$sampleId.R2.fq.gz")) else None
      mergeR2.foreach { file =>
        add(Zcat(qscript, libraries.values.flatMap(_.qcR2).toList) | new Gzip(qscript) > file)
      }

      gearsSingle.fastqR1 = List(addDownsample(mergeR1, gearsSingle.outputDir))
      gearsSingle.fastqR2 = mergeR2.map(addDownsample(_, gearsSingle.outputDir)).toList
      add(gearsSingle)
    }

    /** Must return files to store into summary */
    def summaryFiles: Map[String, File] = Map()

    /** Must returns stats to store into summary */
    def summaryStats: Any = Map()
  }

  val downSample: Option[Double] = config("gears_downsample")

  def addDownsample(input: File, dir: File): File = {
    downSample match {
      case Some(x) =>
        val output = new File(dir, input.getName + ".fq.gz")
        val seqtk = new SeqtkSample(this)
        seqtk.input = input
        seqtk.sample = x
        add(seqtk | new Gzip(this) > output)
        output
      case _ => input
    }
  }

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map("gears_downsample" -> downSample)

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = (
    qiimeOpenOtuTable.map("qiime_open_otu_table" -> _) ++
    qiimeOpenOtuMap.map("qiime_open_otu_map" -> _) ++
    qiimeClosedOtuTable.map("qiime_closed_otu_table" -> _) ++
    qiimeClosedOtuMap.map("qiime_closed_otu_map" -> _)
  ).toMap
}

object Gears extends PipelineCommand