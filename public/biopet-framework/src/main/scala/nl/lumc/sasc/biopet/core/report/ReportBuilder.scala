package nl.lumc.sasc.biopet.core.report

import java.io.{ PrintWriter, File }

import nl.lumc.sasc.biopet.core.{ BiopetJavaCommandLineFunction, ToolCommand }
import nl.lumc.sasc.biopet.core.summary.Summary
import org.broadinstitute.gatk.utils.commandline.Input
import org.fusesource.scalate.{ TemplateSource, TemplateEngine }

import scala.io.Source

/**
 * Created by pjvan_thof on 3/27/15.
 */
trait ReportBuilderExtension extends BiopetJavaCommandLineFunction {

  val builder: ReportBuilder

  @Input(required = true)
  var summaryFile: File = _

  var outputDir: File = _

  var args: Map[String, String] = Map()

  override def beforeGraph: Unit = {
    super.beforeGraph
    jobOutputFile = new File(outputDir, ".report.log.out")
    javaMainClass = builder.getClass.getName.takeWhile(_ != '$')
  }

  override def commandLine: String = {
    super.commandLine +
      required("--summary", summaryFile) +
      required("--outputDir", outputDir) +
      args.map(x => required(x._1, x._2)).mkString
  }
}

trait ReportBuilder extends ToolCommand {

  case class Args(summary: File = null, outputDir: File = null, pageArgs: Map[String, String] = Map()) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('s', "summary") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(summary = x)
    }
    opt[File]('o', "outputDir") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputDir = x)
    }
    opt[Map[String, String]]('a', "args") action { (x, c) =>
      c.copy(pageArgs = c.pageArgs ++ x)
    }
  }

  private var setSummary: Summary = _

  final def summary = setSummary

  def pageArgs: Map[String, Any] = Map()

  private var done = 0
  private var total = 0

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
    Source.fromInputStream(getClass.getResourceAsStream("/nl/lumc/sasc/biopet/core/report/biopet.css")).getLines().foreach(cssWriter.println)
    cssWriter.close()

    logger.info("Parsing summary")
    setSummary = new Summary(cmdArgs.summary)

    total = countPages(indexPage)
    logger.info(total + " pages to be generated")

    logger.info("Generate pages")
    generatePage(summary, indexPage, cmdArgs.outputDir,
      args = pageArgs ++ cmdArgs.pageArgs ++
        Map("summary" -> summary, "reportName" -> reportName, "indexPage" -> indexPage))

    logger.info(done + " Done")
  }

  def indexPage: ReportPage

  def reportName: String

  def countPages(page: ReportPage): Int = {
    page.subPages.map(x => countPages(x._2)).fold(1)(_ + _)
  }

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
    done += 1
    if (done % 100 == 0) logger.info(done + " Done, " + (done.toDouble / total * 100) + "%")
  }
}

object ReportBuilder {

  protected val engine = new TemplateEngine()

  def renderTemplate(location: String, args: Map[String, Any]): String = {
    engine.layout(TemplateSource.fromFile(getClass.getResource(location).getPath), args)
  }
}