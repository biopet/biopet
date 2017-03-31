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
package nl.lumc.sasc.biopet.core.report

import java.io._

import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.utils.summary.db.Schema.{ Library, Module, Pipeline, Sample }
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import nl.lumc.sasc.biopet.utils.{ IoUtils, Logging, ToolCommand }
import org.broadinstitute.gatk.utils.commandline.Input
import org.fusesource.scalate.TemplateEngine

import scala.collection.mutable
import scala.concurrent.{ Await, ExecutionContextExecutor, Future }
import scala.concurrent.duration.Duration
import scala.language.postfixOps
import scala.language.implicitConversions

/**
 * This trait is meant to make an extension for a report object
 *
 * @author pjvan_thof
 */
trait ReportBuilderExtension extends ToolCommandFunction {

  /** Report builder object */
  def builder: ReportBuilder

  def toolObject = builder

  @Input(required = true)
  var summaryDbFile: File = _

  var runId: Option[Int] = None

  /** OutputDir for the report  */
  var outputDir: File = _

  /** Arguments that are passed on the commandline */
  var args: Map[String, String] = Map()

  override def defaultCoreMemory = 4.0
  override def defaultThreads = 3

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    jobOutputFile = new File(outputDir, ".report.log.out")
    javaMainClass = builder.getClass.getName.takeWhile(_ != '$')
  }

  /** Command to generate the report */
  override def cmdLine: String = {
    super.cmdLine +
      required("--summaryDb", summaryDbFile) +
      optional("--runId", runId) +
      required("--outputDir", outputDir) +
      args.map(x => required("-a", x._1 + "=" + x._2)).mkString
  }
}

trait ReportBuilder extends ToolCommand {

  implicit lazy val global: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit def toOption[T](x: T): Option[T] = Option(x)
  implicit def autoWait[T](x: Future[T]): T = Await.result(x, Duration.Inf)

  case class Args(summaryDbFile: File = null,
                  outputDir: File = null,
                  runId: Int = 0,
                  pageArgs: mutable.Map[String, Any] = mutable.Map()) extends AbstractArgs

  class OptParser extends AbstractOptParser {

    head(
      s"""
         |$commandName - Generate HTML formatted report from a biopet summary.json
       """.stripMargin
    )

    opt[File]('s', "summaryDb") unbounded () required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(summaryDbFile = x)
    } validate {
      x => if (x.exists) success else failure("Summary JSON file not found!")
    } text "Biopet summary JSON file"

    opt[File]('o', "outputDir") unbounded () required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputDir = x)
    } text "Output HTML report files to this directory"

    opt[Int]("runId") unbounded () maxOccurs 1 valueName "<int>" action { (x, c) =>
      c.copy(runId = x)
    }

    opt[Map[String, String]]('a', "args") unbounded () action { (x, c) =>
      c.copy(pageArgs = c.pageArgs ++ x)
    }
  }

  /** summary object internaly */
  private var setSummary: SummaryDb = _

  /** Retrival of summary, read only */
  final def summary = setSummary

  private var setRunId: Int = 0

  final def runId = setRunId

  private var _setPipelines = Seq[Pipeline]()
  final def pipelines = _setPipelines
  private var _setModules = Seq[Module]()
  final def modules = _setModules
  private var _setSamples = Seq[Sample]()
  final def samples = _setSamples
  private var _setLibraries = Seq[Library]()
  final def libraries = _setLibraries

  /** default args that are passed to all page withing the report */
  def pageArgs: Map[String, Any] = Map()

  private var done = 0
  private var total = 0

  private var _sampleId: Option[Int] = None
  protected[report] def sampleId = _sampleId
  private var _libId: Option[Int] = None
  protected[report] def libId = _libId

  case class ExtFile(resourcePath: String, targetPath: String)

  def extFiles = List(
    "css/bootstrap_dashboard.css",
    "css/bootstrap.min.css",
    "css/bootstrap-theme.min.css",
    "css/sortable-theme-bootstrap.css",
    "js/jquery.min.js",
    "js/sortable.min.js",
    "js/bootstrap.min.js",
    "js/d3.v3.5.5.min.js",
    "fonts/glyphicons-halflings-regular.woff",
    "fonts/glyphicons-halflings-regular.ttf",
    "fonts/glyphicons-halflings-regular.woff2"
  ).map(x => ExtFile("/nl/lumc/sasc/biopet/core/report/ext/" + x, x))

  /** Main function to for building the report */
  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    require(cmdArgs.outputDir.exists(), "Output dir does not exist")
    require(cmdArgs.outputDir.isDirectory, "Output dir is not a directory")

    setSummary = SummaryDb.openReadOnlySqliteSummary(cmdArgs.summaryDbFile)
    setRunId = cmdArgs.runId

    cmdArgs.pageArgs.get("sampleId") match {
      case Some(s: String) =>
        _sampleId = Await.result(summary.getSampleId(runId, s), Duration.Inf)
        cmdArgs.pageArgs += "sampleId" -> sampleId
      case _ =>
    }

    cmdArgs.pageArgs.get("libId") match {
      case Some(l: String) =>
        _libId = Await.result(summary.getLibraryId(runId, sampleId.get, l), Duration.Inf)
        cmdArgs.pageArgs += "libId" -> libId
      case _ =>
    }

    _setPipelines = Await.result(summary.getPipelines(runId = Some(runId)), Duration.Inf)
    _setModules = Await.result(summary.getModules(runId = Some(runId)), Duration.Inf)
    _setSamples = Await.result(summary.getSamples(runId = Some(runId), sampleId = sampleId), Duration.Inf)
    _setLibraries = Await.result(summary.getLibraries(runId = Some(runId), sampleId = sampleId, libId = libId), Duration.Inf)

    val baseFilesFuture = Future {
      logger.info("Copy Base files")

      // Static files that will be copied to the output folder, then file is added to [resourceDir] it's need to be added here also
      val extOutputDir: File = new File(cmdArgs.outputDir, "ext")

      // Copy each resource files out to the report destination
      extFiles.foreach(
        resource =>
          IoUtils.copyStreamToFile(
            getClass.getResourceAsStream(resource.resourcePath),
            new File(extOutputDir, resource.targetPath),
            createDirs = true)
      )
    }

    val rootPage = indexPage

    //    total = ReportBuilder.countPages(rootPage)
    logger.info(total + " pages to be generated")

    done = 0

    logger.info("Generate pages")
    val jobs = generatePage(summary, rootPage, cmdArgs.outputDir,
      args = pageArgs ++ cmdArgs.pageArgs.toMap ++
        Map("summary" -> summary, "reportName" -> reportName, "indexPage" -> rootPage, "runId" -> cmdArgs.runId))

    Await.result(jobs, Duration.Inf)
    Await.result(baseFilesFuture, Duration.Inf)
  }

  /** This must be implemented, this will be the root page of the report */
  def indexPage: Future[ReportPage]

  /** This must be implemented, this will become the title of the report */
  def reportName: String

  /**
   * This method will render the page and the subpages recursivly
   *
   * @param summary The summary object
   * @param pageFuture Page to render
   * @param outputDir Root output dir of the report
   * @param path Path from root to current page
   * @param args Args to add to this sub page, are args from current page are passed automaticly
   * @return Number of pages including all subpages that are rendered
   */
  def generatePage(summary: SummaryDb,
                   pageFuture: Future[ReportPage],
                   outputDir: File,
                   path: List[String] = Nil,
                   args: Map[String, Any] = Map()): Future[_] = {
    pageFuture.flatMap { page =>
      val pageOutputDir = new File(outputDir, path.mkString(File.separator))
      pageOutputDir.mkdirs()
      val rootPath = "./" + Array.fill(path.size)("../").mkString
      val pageArgs = args ++ page.args ++
        Map("page" -> page,
          "path" -> path,
          "outputDir" -> pageOutputDir,
          "rootPath" -> rootPath,
          "allPipelines" -> pipelines,
          "allModules" -> modules,
          "allSamples" -> samples,
          "allLibraries" -> libraries
        )

      // Generating subpages
      val jobs = Future.sequence(page.subPages.map {
        case (name, subPage) => generatePage(summary, subPage, outputDir, path ::: name :: Nil, pageArgs)
      })

      val renderFuture = Future {
        val output = ReportBuilder.renderTemplate("/nl/lumc/sasc/biopet/core/report/main.ssp",
          pageArgs ++ Map("args" -> pageArgs))

        val file = new File(pageOutputDir, "index.html")
        val writer = new PrintWriter(file)
        writer.println(output)
        writer.close()
      }

      Future.sequence(renderFuture :: jobs :: Nil)
    }

  }
}

object ReportBuilder {

  /** Single template render engine, this will have a cache for all compile templates */
  protected val engine = new TemplateEngine()
  engine.allowReload = false

  /** This will give the total number of pages including all nested pages */
  //  def countPages(page: ReportPage): Int = {
  //    page.subPages.map(x => countPages(x._2)).fold(1)(_ + _)
  //  }

  /**
   * This method will render a template that is located in the classpath / jar
   * @param location location in the classpath / jar
   * @param args Additional arguments, not required
   * @return Rendered result of template
   */
  def renderTemplate(location: String, args: Map[String, Any] = Map()): String = {
    Logging.logger.info("Rendering: " + location)

    engine.layout(location, args)
  }
}