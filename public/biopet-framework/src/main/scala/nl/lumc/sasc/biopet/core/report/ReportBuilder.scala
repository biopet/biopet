package nl.lumc.sasc.biopet.core.report

import java.io._

import nl.lumc.sasc.biopet.core.summary.Summary
import nl.lumc.sasc.biopet.core.{ ToolCommand, ToolCommandFuntion }
import nl.lumc.sasc.biopet.utils.IoUtils
import org.broadinstitute.gatk.utils.commandline.Input
import org.fusesource.scalate.{ TemplateEngine, TemplateSource }

/**
 * Created by pjvan_thof on 3/27/15.
 */
trait ReportBuilderExtension extends ToolCommandFuntion {

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

    logger.info("Copy Base files")

    // Static files that will be copied to the output folder, then file is added to [resourceDir] it's need to be added here also
    val extOutputDir: File = new File(cmdArgs.outputDir, "ext")
    val resourceDir: String = "/nl/lumc/sasc/biopet/core/report/ext/"
    val extFiles = List(
      "css/bootstrap_dashboard.css",
      "css/bootstrap.min.css",
      "css/bootstrap-theme.min.css",
      "css/sortable-theme-bootstrap.css",
      "js/jquery.min.js",
      "js/sortable.min.js",
      "js/bootstrap.min.js",
      "fonts/glyphicons-halflings-regular.woff",
      "fonts/glyphicons-halflings-regular.ttf",
      "fonts/glyphicons-halflings-regular.woff2"
    )

    for (resource <- extFiles.par) {
      IoUtils.copyStreamToFile(getClass.getResourceAsStream(resourceDir + resource), new File(extOutputDir, resource), true)
    }

    logger.info("Parsing summary")
    setSummary = new Summary(cmdArgs.summary)

    total = countPages(indexPage)
    logger.info(total + " pages to be generated")

    logger.info("Generate pages")
    val jobs = generatePage(summary, indexPage, cmdArgs.outputDir,
      args = pageArgs ++ cmdArgs.pageArgs ++
        Map("summary" -> summary, "reportName" -> reportName, "indexPage" -> indexPage))

    logger.info(jobs + " Done")
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
                   args: Map[String, Any] = Map()): Int = {

    val pageOutputDir = new File(outputDir, path.mkString(File.separator))
    pageOutputDir.mkdirs()
    val rootPath = "./" + Array.fill(path.size)("../").mkString("")
    val pageArgs = args ++ page.args ++
      Map("page" -> page,
        "path" -> path,
        "outputDir" -> pageOutputDir,
        "rootPath" -> rootPath
      )

    // Generating subpages
    val jobs = for ((name, subPage) <- page.subPages.par) yield {
      generatePage(summary, subPage, outputDir, path ::: name :: Nil, pageArgs)
    }

    val output = ReportBuilder.renderTemplate("/nl/lumc/sasc/biopet/core/report/main.ssp",
      pageArgs ++ Map("args" -> pageArgs))

    val file = new File(pageOutputDir, "index.html")
    val writer = new PrintWriter(file)
    writer.println(output)
    writer.close()

    done += 1
    if (done % 100 == 0) logger.info(done + " Done, " + (done.toDouble / total * 100) + "%")
    jobs.fold(0)(_ + _) + 1
  }
}

object ReportBuilder {

  protected val engine = new TemplateEngine()

  private var templateCache: Map[String, File] = Map()

  /**
   * This method will render a template that is located in the classpath / jar
   * @param location location in the classpath / jar
   * @param args Additional arguments, not required
   * @return Rendered result of template
   */
  def renderTemplate(location: String, args: Map[String, Any] = Map()): String = {
    val templateFile: File = templateCache.get(location) match {
      case Some(template) => template
      case _ => {
        val tempFile = File.createTempFile("ssp-template", new File(location).getName)
        IoUtils.copyStreamToFile(getClass.getResourceAsStream(location), tempFile)
        templateCache += location -> tempFile
        tempFile
      }
    }
    engine.layout(TemplateSource.fromFile(templateFile), args)
  }
}