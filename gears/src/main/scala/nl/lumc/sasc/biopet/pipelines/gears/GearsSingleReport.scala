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

import nl.lumc.sasc.biopet.core.report._
import nl.lumc.sasc.biopet.utils.config.Configurable

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class GearsSingleReport(val parent: Configurable) extends ReportBuilderExtension {
  def builder = GearsSingleReport
}

object GearsSingleReport extends ReportBuilder {

  override def extFiles = super.extFiles ++ List("js/gears.js", "js/krona-2.0.js", "img/krona/loading.gif", "img/krona/hidden.png", "img/krona/favicon.ico")
    .map(x => ExtFile("/nl/lumc/sasc/biopet/pipelines/gears/report/ext/" + x, x))

  def indexPage = {
    val sampleName = sampleId.flatMap(x => Await.result(summary.getSampleName(x), Duration.Inf))
    val libraryName = libId.flatMap(x => Await.result(summary.getLibraryName(x), Duration.Inf))

    val krakenExecuted = summary.getStatsSize(runId, Right("gearskraken"), Some(Right("krakenreport")), sample = sampleId.map(Left(_)), library = libId.map(Left(_))) == 1
    val centrifugeExecuted = summary.getStatsSize(runId, Right("gearscentrifuge"), Some(Right("centrifuge_report")), sample = sampleId.map(Left(_)), library = libId.map(Left(_))) == 1

    ReportPage(
      List(
        "Versions" -> ReportPage(List(),
          List(("Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp"))), Map())
      ),
      List("Gears intro" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/gearsSingleFront.ssp")) ++
        (if (krakenExecuted) List("Kraken analysis" ->
          ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp"))
        else Nil) ++
        (if (centrifugeExecuted) List("Centrifuge analysis" ->
          ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/krakenKrona.ssp",
            Map("summaryModuleTag" -> "gearscentrifuge", "summaryStatsTag" -> "centrifuge_unique_report", "centrifugeTag" -> Some("centrifuge"))))
        else Nil),
      pageArgs
    )
  }

  def reportName = "Gears :: Metagenomics Report"

}
