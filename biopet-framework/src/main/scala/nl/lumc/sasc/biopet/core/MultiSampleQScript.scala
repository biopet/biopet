package nl.lumc.sasc.biopet.core

import nl.lumc.sasc.biopet.core.config.Configurable

trait MultiSampleQScript extends BiopetQScript {
  type LibraryOutput <: AbstractLibraryOutput
  type SampleOutput[LibraryOutput] <: AbstractSampleOutput[LibraryOutput]
  
  abstract class AbstractLibraryOutput
  abstract class AbstractSampleOutput[LibraryOutput] {
    protected[MultiSampleQScript] var libraries: Map[String, LibraryOutput] = Map()
    def getAllLibraries = libraries
    def getLibrary(key:String) = libraries(key)
  }
  
  var samplesConfig: Map[String, Any] = Map()
  var samplesOutput: Map[String, SampleOutput[LibraryOutput]] = Map()
  def globalSampleDir: String = outputDir + "samples/"
  
  final def runSamplesJobs() {
    samplesConfig = config("samples")
    if (samplesConfig == null) samplesConfig = Map()
    if (globalConfig.contains("samples")) for ((key, value) <- samplesConfig) {
      var sample = Configurable.any2map(value)
      if (!sample.contains("ID")) sample += ("ID" -> key)
      if (sample("ID") == key) {
        val output = runSingleSampleJobs(sample)
        if (samplesOutput.contains(key)) output.libraries = samplesOutput(key).libraries
        samplesOutput += key -> output
      }
      else logger.warn("Key is not the same as ID on value for sample")
    }
    else logger.warn("No Samples found in config")
  }

  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput[LibraryOutput]
  def runSingleSampleJobs(sample: String): SampleOutput[LibraryOutput] = {
    return runSingleSampleJobs(Configurable.any2map(samplesConfig(sample)))
  }

  final def runLibraryJobs(sampleConfig: Map[String, Any]): Map[String, LibraryOutput] = {
    var output: Map[String, LibraryOutput] = Map()
    val sampleID = sampleConfig("ID")
    if (sampleConfig.contains("libraries")) {
      val runs = Configurable.any2map(sampleConfig("libraries"))
      for ((key, value) <- runs) {
        var library = Configurable.any2map(value)
        if (!library.contains("ID")) library += ("ID" -> key)
        if (library("ID") == key) {
          output += key -> runSingleLibraryJobs(library, sampleConfig)
        }
        else logger.warn("Key is not the same as ID on value for run of sample: " + sampleID)
      }
    } else logger.warn("No runs found in config for sample: " + sampleID)
    return output
  }
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput
}
