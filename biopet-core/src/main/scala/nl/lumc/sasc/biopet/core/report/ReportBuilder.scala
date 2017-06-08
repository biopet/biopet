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
import java.util.concurrent.{ArrayBlockingQueue, ThreadPoolExecutor, TimeUnit, Executors}

import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.utils.summary.db.Schema.{Library, Module, Pipeline, Run, Sample}
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.{LibraryId, SampleId}
import nl.lumc.sasc.biopet.utils.{IoUtils, Logging, ToolCommand}
import org.broadinstitute.gatk.utils.commandline.Input
import org.fusesource.scalate.TemplateEngine

import scala.collection.mutable
import scala.concurrent._
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

  def toolObject: ReportBuilder = builder

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

  implicit lazy val ec = ReportBuilder.ec
  implicit def toOption[T](x: T): Option[T] = Option(x)
  implicit def autoWait[T](x: Future[T]): T = Await.result(x, Duration.Inf)

  case class Args(summaryDbFile: File = null,
                  outputDir: File = null,
                  runId: Int = 0,
                  pageArgs: mutable.Map[String, Any] = mutable.Map())
      extends AbstractArgs

  class OptParser extends AbstractOptParser {

    head(
      s"""
         |$commandName - Generate HTML formatted report from a biopet summary.json
       """.stripMargin
    )

    opt[File]('s', "summaryDb") unbounded () required () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
        c.copy(summaryDbFile = x)
    } validate { x =>
      if (x.exists) success else failure("Summary JSON file not found!")
    } text "Biopet summary JSON file"

    opt[File]('o', "outputDir") unbounded () required () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
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
  final def summary: SummaryDb = setSummary

  private var setRunId: Int = 0

  final def runId: Int = setRunId

  private var _setRun: Run = _

  final def run: Run = _setRun

  private var _setPipelines = Seq[Pipeline]()
  final def pipelines: Seq[Pipeline] = _setPipelines
  private var _setModules = Seq[Module]()
  final def modules: Seq[Module] = _setModules
  private var _setSamples = Seq[Sample]()
  final def samples: Seq[Sample] = _setSamples
  private var _setLibraries = Seq[Library]()
  final def libraries: Seq[Library] = _setLibraries

  /** default args that are passed to all page withing the report */
  def pageArgs: Map[String, Any] = Map()

  private var done = 0
  private var total = 0

  private var _sampleId: Option[Int] = None
  protected[report] def sampleId: Option[Int] = _sampleId
  private var _libId: Option[Int] = None
  protected[report] def libId: Option[Int] = _libId

  case class ExtFile(resourcePath: String, targetPath: String)

  def extFiles: List[ExtFile] =
    List(
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
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

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

    _setRun = Await.result(summary.getRuns(runId = Some(runId)), Duration.Inf).head
    _setPipelines = Await.result(summary.getPipelines(runId = Some(runId)), Duration.Inf)
    _setModules = Await.result(summary.getModules(runId = Some(runId)), Duration.Inf)
    _setSamples =
      Await.result(summary.getSamples(runId = Some(runId), sampleId = sampleId), Duration.Inf)
    _setLibraries = Await.result(
      summary.getLibraries(runId = Some(runId), sampleId = sampleId, libId = libId),
      Duration.Inf)

    val baseFilesFuture = Future {
      logger.info("Copy Base files")

      // Static files that will be copied to the output folder, then file is added to [resourceDir] it's need to be added here also
      val extOutputDir: File = new File(cmdArgs.outputDir, "ext")

      // Copy each resource files out to the report destination
      extFiles.foreach(
        resource =>
          IoUtils.copyStreamToFile(getClass.getResourceAsStream(resource.resourcePath),
                                   new File(extOutputDir, resource.targetPath),
                                   createDirs = true)
      )
    }

    val rootPage = indexPage.map { x =>
      x.copy(subPages = x.subPages ::: generalPages(sampleId, libId))
    }

    //    total = ReportBuilder.countPages(rootPage)
    done = 0

    logger.info("Generate pages")
    val jobsFutures = generatePage(
      summary,
      rootPage,
      cmdArgs.outputDir,
      args = pageArgs ++ cmdArgs.pageArgs.toMap ++
        Map("summary" -> summary,
            "reportName" -> reportName,
            "indexPage" -> rootPage,
            "runId" -> cmdArgs.runId)
    )

    total = jobsFutures.size
    logger.info(total + " pages to be generated")

    def wait(futures: List[Future[Any]]): Unit = {
      try {
        Await.result(Future.sequence(futures), Duration.fromNanos(30000000000L))
      } catch {
        case e: TimeoutException =>
      }
      val dones = futures.filter(_.isCompleted)
      val notDone = futures.filter(!_.isCompleted)
      done += futures.size - notDone.size
      if (notDone.nonEmpty) {
        logger.info(s"$done / $total pages are generated")
        wait(notDone)
      }
    }

    //jobsFutures.foreach(f => f.onFailure{ case e => throw new RuntimeException(e) })

    wait(jobsFutures)
    Await.result(Future.sequence(jobsFutures), Duration.Inf)
    Await.result(baseFilesFuture, Duration.Inf)

    logger.info(s"Done, $done pages generated")
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
                   args: Map[String, Any] = Map()): List[Future[ReportPage]] = {
    val pageOutputDir = new File(outputDir, path.mkString(File.separator))

    def pageArgs(page: ReportPage) = {
      val rootPath = "./" + Array.fill(path.size)("../").mkString
      args ++ page.args ++
        Map(
          "page" -> page,
          "run" -> run,
          "path" -> path,
          "outputDir" -> pageOutputDir,
          "rootPath" -> rootPath,
          "allPipelines" -> pipelines,
          "allModules" -> modules,
          "allSamples" -> samples,
          "allLibraries" -> libraries
        )
    }

    val subPageJobs = pageFuture.map { page =>
      // Generating subpages
      page.subPages.flatMap {
        case (name, subPage) =>
          generatePage(summary, subPage, outputDir, path ::: name :: Nil, pageArgs(page))
      }
    }

    val renderFuture = pageFuture.map { page =>
      pageOutputDir.mkdirs()

      val file = new File(pageOutputDir, "index.html")
      logger.info(s"Start rendering: $file")

      val output = ReportBuilder.renderTemplate("/nl/lumc/sasc/biopet/core/report/main.ssp",
                                                pageArgs(page) ++ Map("args" -> pageArgs(page)))

      val writer = new PrintWriter(file)
      writer.println(output)
      writer.close()
      logger.info(s"Done rendering: $file")

      page
    }

    renderFuture :: Await.result(subPageJobs, Duration.Inf)
  }

  def pipelineName: String

  /** Files page, can be used general or at sample level */
  def filesPage(sampleId: Option[Int] = None, libraryId: Option[Int] = None): Future[ReportPage] = {
    val dbFiles = summary
      .getFiles(runId, sample = sampleId.map(SampleId), library = libraryId.map(LibraryId))
      .map(_.groupBy(_.pipelineId))
    val modulePages = dbFiles.map(_.map {
      case (pipelineId, files) =>
        val moduleSections = files.groupBy(_.moduleId).map {
          case (moduleId, files) =>
            val moduleName: Future[String] = moduleId match {
              case Some(id) => summary.getModuleName(pipelineId, id).map(_.getOrElse("Pipeline"))
              case _ => Future.successful("Pipeline")
            }
            moduleName.map(_ -> ReportSection("/nl/lumc/sasc/biopet/core/report/files.ssp",
                                              Map("files" -> files)))
        }
        val moduleSectionsSorted = moduleSections.find(_._1 == "Pipeline") ++ moduleSections
          .filter(_._1 != "Pipeline")
        summary
          .getPipelineName(pipelineId = pipelineId)
          .map(
            _.get -> Future
              .sequence(moduleSectionsSorted)
              .map(sections => ReportPage(Nil, sections.toList, Map())))
    })

    val pipelineFiles = summary
      .getPipelineId(runId, pipelineName)
      .flatMap(
        pipelinelineId =>
          dbFiles
            .map(
              x =>
                x.get(pipelinelineId.get)
                  .getOrElse(Seq())
                  .filter(_.moduleId.isEmpty)))

    modulePages
      .flatMap(Future.sequence(_))
      .map(x =>
        ReportPage(
          x.toList,
          s"$pipelineName files" -> ReportSection(
            "/nl/lumc/sasc/biopet/core/report/files.ssp",
            Map("files" -> Await.result(pipelineFiles, Duration.Inf))) ::
            "Sub pipelines/modules" -> ReportSection(
            "/nl/lumc/sasc/biopet/core/report/fileModules.ssp",
            Map("pipelineIds" -> Await.result(dbFiles.map(_.keys.toList), Duration.Inf))) :: Nil,
          Map()
      ))
  }

  /** This generate general pages that all reports should have */
  def generalPages(sampleId: Option[Int], libId: Option[Int]): List[(String, Future[ReportPage])] =
    List(
      "Versions" -> Future.successful(
        ReportPage(
          List(),
          List("Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp")),
          Map())),
      "Files" -> filesPage(sampleId, libId)
    )

}

object ReportBuilder {

  /** Limiting the number of threads and instantiated futures */
  def maxThreads = Some(2)
  val numWorkers: Int = maxThreads.getOrElse(2)
  val queueCapacity = 100

  implicit lazy val ec = ExecutionContext.global

  /** New queueing execution context
  implicit lazy val ec = ExecutionContext.fromExecutorService(
    new ThreadPoolExecutor(
      numWorkers,
      numWorkers,
      0L,
      TimeUnit.SECONDS,
      new ArrayBlockingQueue[Runnable](queueCapacity) {
        override def offer(e: Runnable) = {
          put(e);
          true
        }
      }
    )
  )
    */
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
    Logging.logger.debug("Rendering: " + location)

    engine.layout(location, args)
  }
}
