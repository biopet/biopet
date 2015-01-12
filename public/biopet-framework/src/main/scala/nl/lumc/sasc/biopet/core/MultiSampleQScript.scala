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
  val getSamplesConfig: Map[String, Any] = config("samples", default = Map())
  val getSamples: Set[String] = getSamplesConfig.keySet
  def globalSampleDir: String = outputDir + "samples/"

  var samplesOutput: Map[String, SampleOutput] = Map()

  final def runSamplesJobs() {
    for ((key, value) <- getSamplesConfig) {
      var sample = any2map(value)
      if (!sample.contains("ID")) sample += ("ID" -> key)
      if (sample("ID") == key) {
        setCurrentSample(key)
        samplesOutput += key -> runSingleSampleJobs(sample)
        unsetCurrentSample()
      } else logger.warn("Key is not the same as ID on value for sample")
    }
  }

  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput
  def runSingleSampleJobs(sample: String): SampleOutput = {
    var map = any2map(getSamplesConfig(sample))
    if (map.contains("ID") && map("ID") != sample)
      throw new IllegalStateException("ID in config not the same as the key")
    else map += ("ID" -> sample)
    return runSingleSampleJobs(map)
  }

  final def runLibraryJobs(sampleConfig: Map[String, Any]): Map[String, LibraryOutput] = {
    var output: Map[String, LibraryOutput] = Map()
    val sampleID = sampleConfig("ID").toString
    if (sampleConfig.contains("libraries")) {
      val runs = any2map(sampleConfig("libraries"))
      for ((key, value) <- runs) {
        var library = any2map(value)
        if (!library.contains("ID")) library += ("ID" -> key)
        if (library("ID") == key) {
          setCurrentLibrary(key)
          output += key -> runSingleLibraryJobs(library, sampleConfig)
          unsetCurrentLibrary()
        } else logger.warn("Key is not the same as ID on value for run of sample: " + sampleID)
      }
    } else logger.warn("No runs found in config for sample: " + sampleID)
    return output
  }
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput

  private var currentSample: String = null
  private var currentLibrary: String = null

  def setCurrentSample(sample: String) {
    currentSample = sample
  }

  def getCurrentSample = currentSample

  def unsetCurrentSample() {
    currentSample = null
  }

  def setCurrentLibrary(library: String) {
    currentLibrary = library
  }

  def getCurrentLibrary = currentLibrary

  def unsetCurrentLibrary() {
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
