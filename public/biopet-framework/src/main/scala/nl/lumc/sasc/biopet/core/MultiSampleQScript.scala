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
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.utils.commandline.{ Argument }

/**
 * This trait creates a structured way of use multisample pipelines
 */
trait MultiSampleQScript extends BiopetQScript {
  @Argument(doc = "Only Sample", shortName = "sample", required = false)
  val onlySample: List[String] = Nil

  require(Config.global.map.contains("samples"), "No Samples found in config")

  /**
   * Sample class with basic functions build in
   * @param sampleId
   */
  abstract class AbstractSample(val sampleId: String) {
    /** Overrules config of qscript with default sample */
    val config = new ConfigFunctions(defaultSample = sampleId)

    /**
     * Library class with basic functions build in
     * @param libraryId
     */
    abstract class AbstractLibrary(val libraryId: String) {
      /** Overrules config of qscript with default sample and default library */
      val config = new ConfigFunctions(defaultSample = sampleId, defaultLibrary = libraryId)

      /** Adds the library jobs */
      final def addAndTrackJobs(): Unit = {
        currentSample = Some(sampleId)
        currentLib = Some(libraryId)
        addJobs()
        currentLib = None
        currentSample = None
      }

      /** Creates a library file with given suffix */
      def createFile(suffix: String): File = new File(libDir, sampleId + "-" + libraryId + suffix)

      /** Returns library directory */
      def libDir = sampleDir + "lib_" + libraryId + File.separator

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
      ConfigUtils.getMapFromPath(Config.global.map, List("samples", sampleId, "libraries")).getOrElse(Map()).keySet
    }

    /** Adds sample jobs */
    final def addAndTrackJobs(): Unit = {
      currentSample = Some(sampleId)
      addJobs()
      currentSample = None
    }

    /** Function to add sample jobs */
    protected def addJobs()

    /** function add all libraries in one call */
    protected final def addLibsJobs(): Unit = {
      for ((libraryId, library) <- libraries) {
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
    def sampleDir = outputDir + "samples" + File.separator + sampleId + File.separator
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
  protected def sampleIds: Set[String] = ConfigUtils.any2map(Config.global.map("samples")).keySet

  /** Runs addAndTrackJobs method for each sample */
  final def addSamplesJobs() {
    if (onlySample.isEmpty) samples.foreach { case (sampleId, sample) => sample.addAndTrackJobs() }
    else onlySample.foreach(sampleId => samples.get(sampleId) match {
      case Some(sample) => sample.addAndTrackJobs()
      case None         => logger.warn("sampleId '" + sampleId + "' not found")
    })
  }

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
