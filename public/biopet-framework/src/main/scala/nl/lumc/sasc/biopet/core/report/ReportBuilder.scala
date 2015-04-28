package nl.lumc.sasc.biopet.core.report

import java.io.{ PrintWriter, File }

import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.summary.Summary
import org.fusesource.scalate.{ TemplateSource, TemplateEngine }

import scala.io.Source

/**
 * Created by pjvan_thof on 3/27/15.
 */
trait ReportBuilder extends ToolCommand {

  case class Args(summary: File = null, outputDir: File = null, pageArgs: Map[String, String] = Map()) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('s', "summary") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(summary = x)
    }
    opt[File]('o', "outputDir") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(outputDir = x)
    }
    opt[Map[String, String]]('a', "args") action { (x, c) =>
      c.copy(pageArgs = c.pageArgs ++ x)
    }
  }

  private var setSummary: Summary = _

  final def summary = setSummary

  def pageArgs: Map[String, Any] = Map()

  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    require(cmdArgs.outputDir.exists(), "Output dir does not exist")
    require(cmdArgs.outputDir.isDirectory, "Output dir is not a directory")

    logger.info("Write Base files")
    // Write css to output dir
    val cssDir = new File(cmdArgs.outputDir, "css")
    cssDir.mkdirs()
    val cssWriter = new PrintWriter(new File(cssDir, "biopet.css"))
    Source.fromInputStream(getClass.getResourceAsStream("/nl/lumc/sasc/biopet/core/report/biopet.css")).getLines().foreach(cssWriter.println(_))
    cssWriter.close()

    logger.info("Parsing summary")
    setSummary = new Summary(cmdArgs.summary)

    logger.info("Generate pages")
    generatePage(summary, indexPage, cmdArgs.outputDir,
      args = pageArgs ++ cmdArgs.pageArgs ++
        Map("summary" -> summary, "reportName" -> reportName, "indexPage" -> indexPage))

    logger.info("Done")
  }

  def indexPage: ReportPage

  def reportName: String

  def generatePage(summary: Summary,
                   page: ReportPage,
                   outputDir: File,
                   path: List[String] = Nil,
                   args: Map[String, Any] = Map()): Unit = {

    val pageOutputDir = new File(outputDir, path.mkString(File.separator))
    pageOutputDir.mkdirs()
    val rootPath = "./" + Array.fill(path.size)("../").mkString("")
    val pageArgs = args ++ page.args ++
      Map("page" -> page,
        "path" -> path,
        "outputDir" -> pageOutputDir,
        "rootPath" -> rootPath
      )

    val output = ReportBuilder.renderTemplate("/nl/lumc/sasc/biopet/core/report/main.ssp",
      pageArgs ++ Map("args" -> pageArgs))

    val file = new File(pageOutputDir, "index.html")
    val writer = new PrintWriter(file)
    writer.println(output)
    writer.close()

    // Generating subpages
    for ((name, subPage) <- page.subPages.par) {
      generatePage(summary, subPage, outputDir, path ::: name :: Nil, pageArgs)
    }
  }
}

object ReportBuilder {

  protected val engine = new TemplateEngine()

  def renderTemplate(location: String, args: Map[String, Any]): String = {
    engine.layout(TemplateSource.fromFile(getClass.getResource(location).getPath), args)
  }
}