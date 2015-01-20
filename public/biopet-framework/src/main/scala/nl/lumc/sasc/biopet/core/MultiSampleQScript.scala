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

import nl.lumc.sasc.biopet.core.config.{ ConfigValue, Config, Configurable }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.ConfigUtils._
import org.broadinstitute.gatk.utils.commandline.{ Argument }

trait MultiSampleQScript extends BiopetQScript {
  type LibraryOutput <: AbstractLibraryOutput
  type SampleOutput <: AbstractSampleOutput

  @Argument(doc = "Only Sample", shortName = "sample", required = false)
  val onlySample: List[String] = Nil

  abstract class AbstractLibraryOutput
  abstract class AbstractSampleOutput {
    var libraries: Map[String, LibraryOutput] = Map()
    def getAllLibraries = libraries
    def getLibrary(key: String) = libraries(key)
  }

  if (!Config.global.map.contains("samples")) logger.warn("No Samples found in config")

  /**
   * Returns a map with all sample configs
   */
  val getSamplesConfig: Map[String, Any] = ConfigUtils.any2map(Config.global.map.getOrElse("samples", Map()))

  /** Returns a list of all sampleIDs */
  def getSamples: Set[String] = if (onlySample == Nil) getSamplesConfig.keySet else onlySample.toSet

  def getLibraries(sample: String): Set[String] = {
    ConfigUtils.getMapFromPath(getSamplesConfig, List(sample, "libraries")).getOrElse(Map()).keySet
  }

  /**
   * Returns the global sample directory
   * @return global sample directory
   */
  def globalSampleDir: String = outputDir + "samples/"

  var samplesOutput: Map[String, SampleOutput] = Map()

  /** Runs runSingleSampleJobs method for each sample */
  final def runSamplesJobs() {
    for (sampleID <- getSamples) {
      currentSample = Some(sampleID)
      samplesOutput += sampleID -> runSingleSampleJobs(sampleID)
      currentSample = null
    }
  }

  /**
   * Run sample with only sampleID
   * @param sampleID sampleID
   * @return
   */
  def runSingleSampleJobs(sampleID: String): SampleOutput

  /**
   * Runs runSingleLibraryJobs method for each library found in sampleConfig
   * @param sampleID sampleID
   * @return Map with libraryID -> LibraryOutput object
   */
  final def runLibraryJobs(sampleID: String = null): Map[String, LibraryOutput] = {
    var output: Map[String, LibraryOutput] = Map()
    for (libraryID <- getLibraries(sampleID)) {
      currentLibrary = Some(libraryID)
      output += libraryID -> runSingleLibraryJobs(sampleID, libraryID)
      currentLibrary = None
    }
    return output
  }
  def runSingleLibraryJobs(sampleID: String, libraryID: String): LibraryOutput

  protected var currentSample: Option[String] = None
  protected var currentLibrary: Option[String] = None

  /**
   * Set current sample manual, only use this when not using runSamplesJobs method
   * @param sample
   */
  def setCurrentSample(sample: String) {
    logger.debug("Manual sample set to: " + sample)
    currentSample = Some(sample)
  }

  /**
   * Gets current sample
   * @return current sample
   */
  def getCurrentSample = currentSample

  /**
   * Reset current sample manual, only use this when not using runSamplesJobs method
   */
  def resetCurrentSample() {
    logger.debug("Manual sample reset")
    currentSample = None
  }

  /**
   * Set current library manual, only use this when not using runLibraryJobs method
   * @param library
   */
  def setCurrentLibrary(library: String) {
    logger.debug("Manual library set to: " + library)
    currentLibrary = Some(library)
  }

  /**
   * Gets current library
   * @return current library
   */
  def getCurrentLibrary = currentLibrary

  /** Reset current library manual, only use this when not using runLibraryJobs method */
  def resetCurrentLibrary() {
    logger.debug("Manual library reset")
    currentLibrary = None
  }

  override protected[core] def configFullPath: List[String] = {
    (if (currentSample.isDefined) "samples" :: currentSample.get :: Nil else Nil) :::
      (if (currentLibrary.isDefined) "libraries" :: currentLibrary.get :: Nil else Nil) :::
      super.configFullPath
  }

  override val config = new ConfigFunctionsExt

  protected class ConfigFunctionsExt extends super.ConfigFunctions {
    override def apply(key: String,
                       default: Any = null,
                       submodule: String = null,
                       required: Boolean = false,
                       freeVar: Boolean = true,
                       sample: String = null,
                       library: String = null): ConfigValue = {
      val s = if (sample == null) currentSample.getOrElse(null) else sample
      val l = if (library == null) currentLibrary.getOrElse(null) else library
      super.apply(key, default, submodule, required, freeVar, s, l)
    }

    override def contains(key: String,
                          submodule: String = null,
                          freeVar: Boolean = true,
                          sample: String = null,
                          library: String = null) = {
      val s = if (sample == null) currentSample.getOrElse(null) else sample
      val l = if (library == null) currentLibrary.getOrElse(null) else library
      super.contains(key, submodule, freeVar, s, l)
    }
  }
}
