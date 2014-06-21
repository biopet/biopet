package nl.lumc.sasc.biopet.core

//import org.broadinstitute.sting.queue.QScript
import java.io.File
import nl.lumc.sasc.biopet.core.config._

abstract trait BiopetQScript extends Configurable {
  //var config: Config = _
  var outputFiles:Map[String,File] = Map()
  var samples:Map[String,Any] = Map()
  
  final def runSamplesJobs : Map[String,Map[String,File]] = {
    var output: Map[String,Map[String,File]] = Map()
    samples = config("samples")
    if (globalConfig.contains("samples")) for ((key,value) <- samples) {
      var sample = Configurable.any2map(value)
      if (!sample.contains("ID")) sample += ("ID" -> key)
      if (sample("ID") == key) {
        var files:Map[String,List[File]] = runSingleSampleJobs(sample)
      } else logger.warn("Key is not the same as ID on value for sample")
    } else logger.warn("No Samples found in config")
    return output
  }
  
  def runSingleSampleJobs(sample:String) : Map[String,List[File]] ={
    return runSingleSampleJobs(Configurable.any2map(samples(sample)))
  }
  def runSingleSampleJobs(sampleConfig:Map[String,Any]) : Map[String,List[File]] = {
    logger.debug("Default sample function, function 'runSingleSampleJobs' not defined in pipeline")
    
    runRunsJobs(sampleConfig)
    
    return Map()
  }
  
  final def runRunsJobs(sampleConfig:Map[String,Any]) : Map[String,Map[String,File]] = {
    var output: Map[String,Map[String,File]] = Map()
    val sampleID = sampleConfig("ID")
    if (sampleConfig.contains("runs")) {
      val runs = Configurable.any2map(sampleConfig("runs"))
      for ((key,value) <- runs) {
        var run = Configurable.any2map(value)
        if (!run.contains("ID")) run += ("ID" -> key)
        if (run("ID") == key) {
          output += key -> runSingleRunJobs(run, sampleConfig)
        } else logger.warn("Key is not the same as ID on value for run of sample: " + sampleID)
      }
    } else logger.warn("No runs found in config for sample: " + sampleID)
    return output
  }
  def runSingleRunJobs(runConfig:Map[String,Any], sampleConfig:Map[String,Any]) : Map[String,File] = {
    logger.debug("Default run function, function 'runSingleRunJobs' not defined in pipeline")
    
    return Map()
  }
}
