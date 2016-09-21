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
package nl.lumc.sasc.biopet.core

import java.io.File

import nl.lumc.sasc.biopet.core.MultiSampleQScript.Gender
import nl.lumc.sasc.biopet.core.summary.{ Summarizable, SummaryQScript }
import nl.lumc.sasc.biopet.utils.{ Logging, ConfigUtils }
import org.broadinstitute.gatk.queue.QScript

/** This trait creates a structured way of use multisample pipelines */
trait MultiSampleQScript extends SummaryQScript { qscript: QScript =>

  @Argument(doc = "Only Process This Sample", shortName = "s", required = false, fullName = "sample")
  private[core] val onlySamples: List[String] = Nil

  if (!globalConfig.map.contains("samples")) Logging.addError("No Samples found in config")

  /** Sample class with basic functions build in */
  abstract class AbstractSample(val sampleId: String) extends Summarizable { sample =>
    /** Overrules config of qscript with default sample */
    val config = new ConfigFunctions(defaultSample = sampleId)

    /** Sample specific settings */
    def summarySettings: Map[String, Any] = Map()

    /** Library class with basic functions build in */
    abstract class AbstractLibrary(val libId: String) extends Summarizable { lib =>
      /** Overrules config of qscript with default sample and default library */
      val config = new ConfigFunctions(defaultSample = sampleId, defaultLibrary = libId)

      /** Name overules the one from qscript */
      def addSummarizable(summarizable: Summarizable, name: String): Unit = {
        qscript.addSummarizable(summarizable, name, Some(sampleId), Some(libId))
      }

      /** Library specific settings */
      def summarySettings: Map[String, Any] = Map()

      /** Adds the library jobs */
      final def addAndTrackJobs(): Unit = {
        if (nameRegex.findFirstIn(libId) == None)
          Logging.addError(s"Library '$libId' $nameError")
        currentSample = Some(sampleId)
        currentLib = Some(libId)
        addJobs()
        qscript.addSummarizable(this, "pipeline", Some(sampleId), Some(libId))
        currentLib = None
        currentSample = None
      }

      /** Creates a library file with given suffix */
      def createFile(suffix: String): File = new File(libDir, s"$sampleId-$libId.$suffix")

      /** Returns library directory */
      def libDir = new File(sampleDir, "lib_" + libId)

      lazy val libTags: Map[String, Any] =
        config("tags", default = Map(), freeVar = false, namespace = libId, path = List("samples", sampleId, "libraries"))

      def sampleId = sample.sampleId

      lazy val libGroups: List[String] = libTags.get("groups") match {
        case Some(g: List[_]) => g.map(_.toString)
        case Some(g: String)  => List(g)
        case _                => Nil
      }

      /** Function that add library jobs */
      protected def addJobs()
    }

    /** Library type, need implementation in pipeline */
    type Library <: AbstractLibrary

    /** Stores all libraries */
    val libraries: Map[String, Library] = libIds.map(id => id -> makeLibrary(id)).toMap

    lazy val sampleTags: Map[String, Any] =
      config("tags", default = Map(), freeVar = false, namespace = sampleId, path = List("samples"))

    lazy val gender = {
      val g: Option[String] = sampleTags.get("gender").map(_.toString)
      g.map(_.toLowerCase) match {
        case Some("male")   => Gender.Male
        case Some("female") => Gender.Female
        case Some(s) =>
          logger.warn(s"Could not convert '$g' to a gender")
          Gender.Unknown
        case _ => Gender.Unknown
      }
    }

    lazy val father = {
      val g: Option[String] = sampleTags.get("father").map(_.toString)
      g.foreach { father =>
        if (sampleId != father) Logging.addError(s"Father for $sampleId can not be itself")
        if (samples.contains(father)) if (samples(father).gender == Gender.Male)
          Logging.addError(s"Father of $sampleId is not a female")
        else logger.warn(s"For sample '$sampleId' is father '$father' not found in config")
      }
      g
    }

    lazy val mother = {
      val g: Option[String] = sampleTags.get("mother").map(_.toString)
      g.foreach { mother =>
        if (sampleId != mother) Logging.addError(s"mother for $sampleId can not be itself")
        if (samples.contains(mother)) if (samples(mother).gender == Gender.Female)
          Logging.addError(s"Mother of $sampleId is not a female")
        else logger.warn(s"For sample '$sampleId' is mother '$mother' not found in config")
      }
      g
    }

    lazy val sampleGroups: List[String] = sampleTags.get("groups") match {
      case Some(g: List[_]) => g.map(_.toString)
      case Some(g: String)  => List(g)
      case _                => Nil
    }

    /**
     * Factory method for Library class
     * @param id SampleId
     * @return Sample class
     */
    def makeLibrary(id: String): Library

    /** returns a set with library names */
    protected def libIds: Set[String] = {
      ConfigUtils.getMapFromPath(globalConfig.map, List("samples", sampleId, "libraries")).getOrElse(Map()).keySet
    }

    /** Name overules the one from qscript */
    def addSummarizable(summarizable: Summarizable, name: String): Unit = {
      qscript.addSummarizable(summarizable, name, Some(sampleId))
    }

    /** Adds sample jobs */
    final def addAndTrackJobs(): Unit = {
      if (nameRegex.findFirstIn(sampleId) == None)
        Logging.addError(s"Sample '$sampleId' $nameError")
      currentSample = Some(sampleId)
      addJobs()
      qscript.addSummarizable(this, "pipeline", Some(sampleId))
      currentSample = None
    }

    /** Function to add sample jobs */
    protected def addJobs()

    /** function add all libraries in one call */
    protected final def addPerLibJobs(): Unit = {
      for ((libId, library) <- libraries) {
        library.addAndTrackJobs()
      }
    }

    /** Creates a sample file with given suffix */
    def createFile(suffix: String) = new File(sampleDir, s"$sampleId.$suffix")

    /** Returns sample directory */
    def sampleDir = new File(outputDir, "samples" + File.separator + sampleId)
  }

  /** Sample type, need implementation in pipeline */
  type Sample <: AbstractSample

  /**
   * Factory method for Sample class
   * @param id SampleId
   * @return Sample class
   */
  def makeSample(id: String): Sample

  /** Stores all samples */
  val samples: Map[String, Sample] = sampleIds.map(id => id -> makeSample(id)).toMap

  /** Returns a list of all sampleIDs */
  protected def sampleIds: Set[String] = ConfigUtils.any2map(globalConfig.map.getOrElse("samples", Map())).keySet

  protected lazy val nameRegex = """^[a-zA-Z0-9][a-zA-Z0-9-_]+[a-zA-Z0-9]$""".r
  protected lazy val nameError = "has an invalid name. " +
    "Sample names must have at least 3 characters, " +
    "must begin and end with an alphanumeric character, " +
    "and must not have whitespace and special characters. " +
    "Dash (-) and underscore (_) are permitted."

  /** Runs addAndTrackJobs method for each sample */
  final def addSamplesJobs() {
    logger.info(s"Starting script for ${samples.size} samples")
    var count = 0
    if (onlySamples.isEmpty || samples.forall(x => onlySamples.contains(x._1))) {
      samples.foreach {
        case (sampleId, sample) =>
          logger.info(s"Starting script sample '$sampleId'")
          sample.addAndTrackJobs()
          count += 1
          logger.info(s"Finish script for '$sampleId', samples done: $count / ${samples.size}")
      }
      logger.info("Starting script for multisample jobs")
      addMultiSampleJobs()
    } else onlySamples.foreach(sampleId => samples.get(sampleId) match {
      case Some(sample) => sample.addAndTrackJobs()
      case None         => logger.warn("sampleId '" + sampleId + "' not found")
    })
  }

  /**
   * Method where the multisample jobs should be added, this will be executed only when running the -sample argument is not given.
   */
  def addMultiSampleJobs()

  /** Stores sample state */
  private var currentSample: Option[String] = None

  /** Stores library state */
  private var currentLib: Option[String] = None

  /** Prefix full path with sample and library for jobs that's are created in current state */
  override def configFullPath: List[String] = {
    val sample = currentSample match {
      case Some(s) => "samples" :: s :: Nil
      case _       => Nil
    }
    val lib = currentLib match {
      case Some(l) => "libraries" :: l :: Nil
      case _       => Nil
    }
    sample ::: lib ::: super.configFullPath
  }
}

object MultiSampleQScript {
  object Gender extends Enumeration {
    val Male, Female, Unknown = Value
  }

}