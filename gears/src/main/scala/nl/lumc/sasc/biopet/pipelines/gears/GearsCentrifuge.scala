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

import nl.lumc.sasc.biopet.core.{ BiopetFifoPipe, SampleLibraryTag }
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.{ Gzip, Zcat }
import nl.lumc.sasc.biopet.extensions.centrifuge.{ Centrifuge, CentrifugeKreport }
import nl.lumc.sasc.biopet.extensions.tools.KrakenReportToJson
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 19/09/16.
 */
class GearsCentrifuge(val parent: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {

  var fastqR1: File = _

  var fastqR2: Option[File] = None

  var outputName: String = _

  override def fixedValues = Map("centrifugekreport" -> Map("only_unique" -> false))

  def init(): Unit = {
    require(fastqR1 != null)
    require(outputName != null)
  }

  def centrifugeOutput = new File(outputDir, s"$outputName.centrifuge.gz")
  def centrifugeMetOutput = new File(outputDir, s"$outputName.centrifuge.met")

  def biopetScript(): Unit = {
    val centrifuge = new Centrifuge(this)
    centrifuge.inputR1 = fastqR1
    centrifuge.inputR2 = fastqR2
    centrifuge.report = Some(new File(outputDir, s"$outputName.centrifuge.report"))
    centrifuge.metFile = Some(centrifugeMetOutput)
    val centrifugeCmd = centrifuge | new Gzip(this) > centrifugeOutput
    centrifugeCmd.threadsCorrection = -1
    add(centrifugeCmd)
    addSummarizable(centrifuge, "centrifuge")

    makeKreport("centrifuge", unique = false)
    makeKreport("centrifuge_unique", unique = true)

    addSummaryJobs()
  }

  protected def makeKreport(name: String, unique: Boolean): Unit = {
    val fifo = new File(outputDir, s"$outputName.$name.fifo")
    val centrifugeKreport = new CentrifugeKreport(this)
    centrifugeKreport.centrifugeOutputFiles :+= fifo
    centrifugeKreport.output = new File(outputDir, s"$outputName.$name.kreport")
    centrifugeKreport.onlyUnique = unique
    val pipe = new BiopetFifoPipe(this, List(centrifugeKreport, Zcat(this, centrifugeOutput, fifo)))
    add(pipe)

    val krakenReportJSON = new KrakenReportToJson(this)
    krakenReportJSON.inputReport = centrifugeKreport.output
    krakenReportJSON.output = new File(outputDir, s"$outputName.$name.krkn.json")
    krakenReportJSON.skipNames = config("skipNames", default = false)
    add(krakenReportJSON)
    addSummarizable(krakenReportJSON, s"${name}_report")
  }

  /** Pipeline settings shown in the summary file */
  def summarySettings: Map[String, Any] = Map()

  /** Statistics shown in the summary file */
  def summaryFiles: Map[String, File] = outputFiles + ("input_R1" -> fastqR1, "centrifuge_output" -> centrifugeOutput) ++
    (fastqR2 match {
      case Some(file) => Map("input_R2" -> file)
      case _          => Map()
    })
}
