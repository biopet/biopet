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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.core.{Version, BiopetJavaCommandLineFunction}
import nl.lumc.sasc.biopet.utils.{Logging, tryToParseNumber}
import org.broadinstitute.gatk.utils.commandline.Argument

import scala.io.Source

/**
  * General picard extension
  *
  * This is based on using class files directly from the jar, if needed other picard jar can be used
  */
abstract class Picard extends BiopetJavaCommandLineFunction with Version {
  override def subPath = "picard" :: super.subPath

  javaMainClass = new picard.cmdline.PicardCommandLine().getClass.getName

  if (config.contains("picard_jar")) jarFile = config("picard_jar")

  @Argument(doc = "VERBOSITY", required = false)
  var verbosity: Option[String] = config("verbosity")

  @Argument(doc = "QUIET", required = false)
  var quiet: Boolean = config("quiet", default = false)

  @Argument(doc = "VALIDATION_STRINGENCY", required = false)
  var stringency: Option[String] = config("validationstringency")

  @Argument(doc = "COMPRESSION_LEVEL", required = false)
  var compression: Option[Int] = config("compressionlevel")

  @Argument(doc = "MAX_RECORDS_IN_RAM", required = false)
  var maxRecordsInRam: Option[Int] = config("maxrecordsinram")

  @Argument(doc = "CREATE_INDEX", required = false)
  val createIndex: Boolean = config("createindex", default = true)

  @Argument(doc = "CREATE_MD5_FILE", required = false)
  var createMd5: Boolean = config("createmd5", default = false)

  def picardToolName = getClass.getSimpleName

  def versionCommand = {
    if (jarFile != null)
      executable + " -cp " + jarFile + " " + javaMainClass + s" $picardToolName -h"
    else null
  }
  def versionRegex = """Version: (.*)""".r
  override def versionExitcode = List(0, 1)

  override def defaultCoreMemory = 4.0

  override def getVersion = {
    if (jarFile == null) Picard.getBiopetPicardVersion
    else super.getVersion
  }

  override def cmdLine =
    super.cmdLine +
      required(picardToolName) +
      required("TMP_DIR=" + jobTempDir) +
      optional("VERBOSITY=", verbosity, spaceSeparated = false) +
      conditional(quiet, "QUIET=TRUE") +
      optional("VALIDATION_STRINGENCY=", stringency, spaceSeparated = false) +
      optional("COMPRESSION_LEVEL=", compression, spaceSeparated = false) +
      optional("MAX_RECORDS_IN_RAM=", maxRecordsInRam, spaceSeparated = false) +
      conditional(createIndex, "CREATE_INDEX=TRUE") +
      conditional(createMd5, "CREATE_MD5_FILE=TRUE")
}

object Picard extends Logging {

  lazy val getBiopetPicardVersion: Option[String] = {
    Option(getClass.getResourceAsStream("/dependency_list.txt")) match {
      case Some(src) =>
        val dependencies = Source
          .fromInputStream(src)
          .getLines()
          .map(_.trim.split(":"))
          .filter(_.size == 5)
          .map(
            line =>
              Map(
                "groupId" -> line(0),
                "artifactId" -> line(1),
                "type" -> line(2),
                "version" -> line(3),
                "scope" -> line(4)
            ))
          .toList

        logger.debug("dependencies: " + dependencies)

        val htsjdk = dependencies
          .find(
            dep =>
              (dep("groupId") == "com.github.samtools" || dep("groupId") == "samtools") &&
                dep("artifactId") == "htsjdk")
          .collect { case dep => "samtools htsjdk " + dep("version") }

        dependencies
          .find(
            dep =>
              (dep("groupId") == "com.github.broadinstitute" || dep("groupId") == "picard") &&
                dep("artifactId") == "picard")
          .collect {
            case dep => "Picard " + dep("version") + " using " + htsjdk.getOrElse("unknown htsjdk")
          }
      case otherwise => None
    }
  }

  def getMetrics(file: File,
                 tag: String = "METRICS CLASS",
                 groupBy: Option[String] = None): Option[Any] = {
    getMetricsContent(file, tag) match {
      case Some((header, content)) =>
        (content.size, groupBy) match {
          case (_, Some(group)) =>
            val groupId = header.indexOf(group)
            if (groupId == -1)
              throw new IllegalArgumentException(group + " not existing in header of: " + file)
            if (header.count(_ == group) > 1)
              logger.warn(group + " multiple times seen in header of: " + file)
            Some(
              (for (c <- content)
                yield
                  c(groupId).toString -> {
                    header
                      .filter(_ != group)
                      .zip(c.take(groupId) ::: c.takeRight(c.size - groupId - 1))
                      .toMap
                  }).toMap)
          case (1, _) => Some(header.zip(content.head).toMap)
          case _ => Some(header :: content)
        }
      case _ => None
    }
  }

  /**
    * This function parse the metrics but transpose for table
    * @param file metrics file
    * @param tag default to "HISTOGRAM"
    * @return
    */
  def getHistogram(file: File, tag: String = "HISTOGRAM") = {
    getMetricsContent(file, tag) match {
      case Some((header, content)) =>
        val colums = header.zipWithIndex.map(x => x._1 -> content.map(_.lift(x._2))).toMap
        Some(colums)
      case _ => None
    }
  }

  /**
    * This function parse a metrics file in separated values
    * @param file input metrics file
    * @return (header, content)
    */
  def getMetricsContent(file: File, tag: String) = {
    if (!file.exists) None
    else {
      val lines = Source.fromFile(file).getLines().toArray

      val start = lines.indexWhere(_.startsWith("## " + tag)) + 1
      val end = lines.indexOf("", start)

      val header = lines(start).split("\t").toList
      val content = (for (i <- (start + 1) until end) yield {
        lines(i).split("\t").map(v => tryToParseNumber(v, fallBack = true).getOrElse(v)).toList
      }).toList

      Some(header, content)
    }
  }
}
