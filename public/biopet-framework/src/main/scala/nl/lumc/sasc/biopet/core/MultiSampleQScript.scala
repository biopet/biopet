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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.core

import java.io.File

import nl.lumc.sasc.biopet.core.config.{ Config }
import nl.lumc.sasc.biopet.core.summary.{ SummaryQScript, Summarizable }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.utils.commandline.{ Argument }

/**
 * This trait creates a structured way of use multisample pipelines
 */
trait MultiSampleQScript extends SummaryQScript {
  qscript =>

  @Argument(doc = "Only Sample", shortName = "sample", required = false)
  private val onlySamples: List[String] = Nil

  require(globalConfig.map.contains("samples"), "No Samples found in config")

  /**
   * Sample class with basic functions build in
   * @param sampleId
   */
  abstract class AbstractSample(val sampleId: String) extends Summarizable {
    /** Overrules config of qscript with default sample */
    val config = new ConfigFunctions(defaultSample = sampleId)

    /**
     * Library class with basic functions build in
     * @param libId
     */
    abstract class AbstractLibrary(val libId: String) extends Summarizable {
      /** Overrules config of qscript with default sample and default library */
      val config = new ConfigFunctions(defaultSample = sampleId, defaultLibrary = libId)

      /**
       * Name overules the one from qscript
       * @param summarizable
       * @param name
       */
      def addSummarizable(summarizable: Summarizable, name: String): Unit = {
        qscript.addSummarizable(summarizable, name, Some(sampleId), Some(libId))
      }

      /** Adds the library jobs */
      final def addAndTrackJobs(): Unit = {
        currentSample = Some(sampleId)
        currentLib = Some(libId)
        addJobs()
        qscript.addSummarizable(this, "pipeline", Some(sampleId), Some(libId))
        currentLib = None
        currentSample = None
      }

      /** Creates a library file with given suffix */
      def createFile(suffix: String): File = new File(libDir, sampleId + "-" + libId + suffix)

      /** Returns library directory */
      def libDir = new File(sampleDir, "lib_" + libId)

      /** Function that add library jobs */
      protected def addJobs()
    }

    /** Library type, need implementation in pipeline */
    type Library <: AbstractLibrary

    /** Stores all libraries */
    val libraries: Map[String, Library] = libIds.map(id => id -> makeLibrary(id)).toMap

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

    /**
     * Name overules the one from qscript
     * @param summarizable
     * @param name
     */
    def addSummarizable(summarizable: Summarizable, name: String): Unit = {
      qscript.addSummarizable(summarizable, name, Some(sampleId))
    }

    /** Adds sample jobs */
    final def addAndTrackJobs(): Unit = {
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

    /**
     * Creates a sample file with given suffix
     * @param suffix
     * @return
     */
    def createFile(suffix: String) = new File(sampleDir, sampleId + suffix)

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
  protected def sampleIds: Set[String] = ConfigUtils.any2map(globalConfig.map("samples")).keySet

  /** Runs addAndTrackJobs method for each sample */
  final def addSamplesJobs() {
    if (onlySamples.isEmpty) {
      samples.foreach { case (sampleId, sample) => sample.addAndTrackJobs() }
      addMultiSampleJobs()
    } else onlySamples.foreach(sampleId => samples.get(sampleId) match {
      case Some(sample) => sample.addAndTrackJobs()
      case None         => logger.warn("sampleId '" + sampleId + "' not found")
    })
  }

  /**
   * Method where the multisample jobs should be added, this will be executed only when running the -sample argument is not given
   */
  def addMultiSampleJobs()

  /** Stores sample state */
  private var currentSample: Option[String] = None

  /** Stores library state */
  private var currentLib: Option[String] = None

  /** Prefix full path with sample and library for jobs that's are created in current state */
  override protected[core] def configFullPath: List[String] = {
    val s = currentSample match {
      case Some(s) => "samples" :: s :: Nil
      case _       => Nil
    }
    val l = currentLib match {
      case Some(l) => "libraries" :: l :: Nil
      case _       => Nil
    }
    s ::: l ::: super.configFullPath
  }
}
