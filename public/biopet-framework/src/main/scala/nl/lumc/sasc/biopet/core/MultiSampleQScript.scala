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

import nl.lumc.sasc.biopet.core.config.{ Config, Configurable }
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

  var samplesConfig: Map[String, Any] = config("samples")
  var samplesOutput: Map[String, SampleOutput] = Map()
  def globalSampleDir: String = outputDir + "samples/"

  final def runSamplesJobs() {
    if (samplesConfig == null) samplesConfig = Map()
    if (Config.global.contains("samples")) for ((key, value) <- samplesConfig) {
      var sample = any2map(value)
      if (!sample.contains("ID")) sample += ("ID" -> key)
      if (sample("ID") == key) {
        samplesOutput += key -> runSingleSampleJobs(sample)
      } else logger.warn("Key is not the same as ID on value for sample")
    }
    else logger.warn("No Samples found in config")
  }

  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput
  def runSingleSampleJobs(sample: String): SampleOutput = {
    var map = any2map(samplesConfig(sample))
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
          output += key -> runSingleLibraryJobs(library, sampleConfig)
        } else logger.warn("Key is not the same as ID on value for run of sample: " + sampleID)
      }
    } else logger.warn("No runs found in config for sample: " + sampleID)
    return output
  }
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput
}
