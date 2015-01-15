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
import nl.lumc.sasc.biopet.utils.ConfigUtils._

trait MultiSampleQScript extends BiopetQScript {
  type LibraryOutput <: AbstractLibraryOutput
  type SampleOutput <: AbstractSampleOutput

  abstract class AbstractLibraryOutput
  abstract class AbstractSampleOutput {
    var libraries: Map[String, LibraryOutput] = Map()
    def getAllLibraries = libraries
    def getLibrary(key: String) = libraries(key)
  }

  if (!config.contains("samples")) logger.warn("No Samples found in config")

  /**
   * Returns a map with all sample configs
   */
  val getSamplesConfig: Map[String, Any] = config("samples", default = Map())

  /**
   * Returns a list of all sampleIDs
   */
  val getSamples: Set[String] = getSamplesConfig.keySet

  /**
   * Returns the global sample directory
   * @return global sample directory
   */
  def globalSampleDir: String = outputDir + "samples/"

  var samplesOutput: Map[String, SampleOutput] = Map()

  /**
   * Runs runSingleSampleJobs method for each sample
   */
  final def runSamplesJobs() {
    for ((key, value) <- getSamplesConfig) {
      var sample = any2map(value)
      if (!sample.contains("ID")) sample += ("ID" -> key)
      if (sample("ID") == key) {
        currentSample = key
        samplesOutput += key -> runSingleSampleJobs(sample)
        currentSample = null
      } else logger.warn("Key is not the same as ID on value for sample")
    }
  }

  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput

  /**
   * Run sample with only sampleID
   * @param sample sampleID
   * @return
   */
  def runSingleSampleJobs(sample: String): SampleOutput = {
    var map = any2map(getSamplesConfig(sample))
    if (map.contains("ID") && map("ID") != sample)
      throw new IllegalStateException("ID in config not the same as the key")
    else map += ("ID" -> sample)
    return runSingleSampleJobs(map)
  }

  /**
   * Runs runSingleLibraryJobs method for each library found in sampleConfig
   * @param sampleConfig sample config
   * @return Map with libraryID -> LibraryOutput object
   */
  final def runLibraryJobs(sampleConfig: Map[String, Any]): Map[String, LibraryOutput] = {
    var output: Map[String, LibraryOutput] = Map()
    val sampleID = sampleConfig("ID").toString
    if (sampleConfig.contains("libraries")) {
      val runs = any2map(sampleConfig("libraries"))
      for ((key, value) <- runs) {
        var library = any2map(value)
        if (!library.contains("ID")) library += ("ID" -> key)
        if (library("ID") == key) {
          currentLibrary = key
          output += key -> runSingleLibraryJobs(library, sampleConfig)
          currentLibrary = null
        } else logger.warn("Key is not the same as ID on value for run of sample: " + sampleID)
      }
    } else logger.warn("No runs found in config for sample: " + sampleID)
    return output
  }
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput

  protected var currentSample: String = null
  protected var currentLibrary: String = null

  /**
   * Set current sample manual, only use this when not using runSamplesJobs method
   * @param sample
   */
  def setCurrentSample(sample: String) {
    logger.debug("Manual sample set to: " + sample)
    currentSample = sample
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
    currentSample = null
  }

  /**
   * Set current library manual, only use this when not using runLibraryJobs method
   * @param library
   */
  def setCurrentLibrary(library: String) {
    logger.debug("Manual library set to: " + library)
    currentLibrary = library
  }

  /**
   * Gets current library
   * @return current library
   */
  def getCurrentLibrary = currentLibrary

  /**
   * Reset current library manual, only use this when not using runLibraryJobs method
   */
  def resetCurrentLibrary() {
    logger.debug("Manual library reset")
    currentLibrary = null
  }

  override protected[core] def configFullPath: List[String] = {
    (if (currentSample != null) "samples" :: currentSample :: Nil else Nil) :::
      (if (currentLibrary != null) "libraries" :: currentLibrary :: Nil else Nil) :::
      super.configFullPath
  }

  protected class ConfigFunctions extends super.ConfigFunctions {
    override def apply(key: String,
                       default: Any = null,
                       submodule: String = null,
                       required: Boolean = false,
                       freeVar: Boolean = true,
                       sample: String = currentSample,
                       library: String = currentLibrary): ConfigValue = {
      super.apply(key, default, submodule, required, freeVar, sample, library)
    }

    override def contains(key: String,
                          submodule: String = null,
                          freeVar: Boolean = true,
                          sample: String = currentSample,
                          library: String = currentLibrary) = {
      super.contains(key, submodule, freeVar, sample, library)
    }
  }
}
