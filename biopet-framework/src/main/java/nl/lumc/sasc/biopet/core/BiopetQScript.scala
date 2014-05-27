package nl.lumc.sasc.biopet.core

//import org.broadinstitute.sting.queue.QScript
import java.io.File
import org.broadinstitute.sting.queue.util.Logging

trait BiopetQScript extends Logging {
  var config: Config = _
  var outputFiles:Map[String,File] = Map()
  
  def runSamplesJobs : Map[String,Map[String,File]] = {
    var output: Map[String,Map[String,File]] = Map()
    if (config.contains("samples")) for ((key,value) <- config.getAsMap("samples")) {
      var sample:Config = config.getAsConfig("samples").getAsConfig(key)
      if (!sample.contains("ID")) sample.map += ("ID" -> key)
      if (sample.getAsString("ID") == key) {
        var files:Map[String,List[File]] = runSingleSampleJobs(sample)
      } else logger.warn("Key is not the same as ID on value for sample")
    } else logger.warn("No Samples found in config")
    return output
  }
  
  def runSingleSampleJobs(sample:String) : Map[String,List[File]] ={
    return runSingleSampleJobs(config.getAsConfig("samples").getAsConfig(sample))
  }
  def runSingleSampleJobs(sampleConfig:Config) : Map[String,List[File]] = {
    logger.debug("Default sample function, function 'runSingleSampleJobs' not defined in pipeline")
    
    runRunsJobs(sampleConfig)
    
    return Map()
  }
  
  def runRunsJobs(sampleConfig:Config) : Map[String,Map[String,File]] = {
    var output: Map[String,Map[String,File]] = Map()
    val sampleID = sampleConfig.getAsString("ID")
    if (sampleConfig.contains("runs")) for (key <- sampleConfig.getAsMap("runs").keySet) {
      var run = sampleConfig.getAsConfig("runs").getAsConfig(key)
      if (!run.contains("ID")) run.map += ("ID" -> key)
      if (run.getAsString("ID") == key) {
        output += key -> runSingleRunJobs(run, sampleConfig)
      } else logger.warn("Key is not the same as ID on value for run of sample: " + sampleID)
    } else logger.warn("No runs found in config for sample: " + sampleID)
    return output
  }
  def runSingleRunJobs(runConfig:Config, sampleConfig:Config) : Map[String,File] = {
    logger.debug("Default run function, function 'runSingleRunJobs' not defined in pipeline")
    
    return Map()
  }
}
