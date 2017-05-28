package nl.lumc.sasc.biopet.pipelines.generateindexes

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.core.report.ReportBuilder
import nl.lumc.sasc.biopet.utils.FastaUtils
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Created by pjvanthof on 26/05/2017.
  */
class RenderReadme extends InProcessFunction {

  var species: String = _
  var genomeName: String = _

  @Input(required = true)
  var fastaFile: File = _

  @Input(required = false)
  var deps: List[File] = Nil

  var indexes: Map[String, File] = Map()

  var downloadUrl: Option[String] = None
  var ncbiAssemblyReport: Option[File] = None

  var extraSections: Map[String, String] = Map()

  @Output(required = true)
  var outputFile: File = _

  def run(): Unit = {
    require(downloadUrl.isDefined || ncbiAssemblyReport.isDefined,
            "A download URL or a ncbiAssemblyReport is required to render a readme")

    val args = Map(
      "species" -> species,
      "genomeName" -> genomeName,
      "fastaFile" -> fastaFile,
      "dict" -> FastaUtils.getCachedDict(fastaFile),
      "indexes" -> indexes,
      "extraSections" -> extraSections,
      "downloadUrl" -> downloadUrl,
      "ncbiAssemblyReport" -> ncbiAssemblyReport
    )
    val content = ReportBuilder.renderTemplate(
      "/nl/lumc/sasc/biopet/pipelines/generateindexes/readme.ssp",
      args)
    val writer = new PrintWriter(outputFile)
    writer.println(content)
    writer.close()
  }
}
