package nl.lumc.sasc.biopet.pipelines.basty

import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.pipelines.gatk.GatkPipeline
import org.broadinstitute.gatk.queue.QScript

class Basty (val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)
  
  val gatkPipeline = new GatkPipeline(this)
  
  class LibraryOutput extends AbstractLibraryOutput {
  }
  
  class SampleOutput extends AbstractSampleOutput {
  }
  
  defaults += "ploidy" -> 1
  
  def init() {
    gatkPipeline.outputDir = outputDir
    gatkPipeline.init
  }
  
  def biopetScript() {
    gatkPipeline.biopetScript
    addAll(gatkPipeline.functions)
    
    runSamplesJobs()
  }
  
  // Called for each sample
  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput = {
    val sampleOutput = new SampleOutput
    val sampleID: String = sampleConfig("ID").toString
    val sampleDir = globalSampleDir + sampleID
        
    sampleOutput.libraries = runLibraryJobs(sampleConfig)
    
    return sampleOutput
  }
  
  // Called for each run from a sample
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput = {
    val libraryOutput = new LibraryOutput
    
    val runID: String = runConfig("ID").toString
    val sampleID: String = sampleConfig("ID").toString
    val runDir: String = globalSampleDir + sampleID + "/run_" + runID + "/"
    
    return libraryOutput
  }
}

object Basty extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/basty/Basty.class"  
}
