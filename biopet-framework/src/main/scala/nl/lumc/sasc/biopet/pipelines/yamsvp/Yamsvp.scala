/*
 * Structural variation calling
 */

package nl.lumc.sasc.biopet.pipelines.yamsvp

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.core.PipelineCommand

import nl.lumc.sasc.biopet.extensions.Cat
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import nl.lumc.sasc.biopet.extensions.svcallers.Breakdancer
import nl.lumc.sasc.biopet.extensions.svcallers.Clever

import nl.lumc.sasc.biopet.pipelines.mapping.Mapping

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function._
import org.broadinstitute.gatk.queue.engine.JobRunInfo



class Yamsvp(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)

  var reference: File = _
  var finalBamFiles: List[File] = Nil


  class LibraryOutput extends AbstractLibraryOutput {
    var mappedBamFile: File = _
  }

  class SampleOutput extends AbstractSampleOutput {
    var vcf: Map[String, List[File]] = Map()
    var mappedBamFile: File = _
  }

  
  override def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    reference = config("reference", required = true)
    if (outputDir == null)
      throw new IllegalStateException("Output directory is not specified in the config / argument")
    else if (!outputDir.endsWith("/"))
      outputDir += "/"
  }

  def biopetScript() {
    // write the pipeline here
    // start with QC, alignment, call sambamba, call sv callers, reporting

    // read config and set all parameters for the pipeline
    logger.info("Starting YAM SV Pipeline")
    runSamplesJobs
    // 

  }

  override def onExecutionDone(jobs: Map[QFunction, JobRunInfo], success: Boolean) {
    logger.info("YAM SV Pipeline has run ...............................................................")
  }
  
  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput = {
    val sampleOutput = new SampleOutput
    var libraryBamfiles: List[File] = List()
    var outputFiles: Map[String, List[File]] = Map()
    var libraryFastqFiles: List[File] = List()
    val sampleID: String = sampleConfig("ID").toString
    val sampleDir: String = outputDir + sampleID + "/"

    val svcallingDir: String = sampleDir + "svcalls/"

    sampleOutput.libraries = runLibraryJobs(sampleConfig)
    for ((libraryID, libraryOutput) <- sampleOutput.libraries) {
      // this is extending the libraryBamfiles list like '~=' in D or .append in Python or .push_back in C++
      libraryBamfiles = List(libraryOutput.mappedBamFile)
    }

    val bamFile: File = if (libraryBamfiles.size == 1) libraryBamfiles.head
    else if (libraryBamfiles.size > 1) {
      val mergeSamFiles = MergeSamFiles(this, libraryBamfiles, sampleDir)
      add(mergeSamFiles)
      mergeSamFiles.output
    } else null

    /// bamfile will be used as input for the SV callers. First run Clever
    //    val cleverVCF : File = sampleDir + "/" + sampleID + ".clever.vcf"

    val cleverDir = svcallingDir + sampleID + ".clever/"
    val clever = Clever(this, bamFile, this.reference, cleverDir)
    sampleOutput.vcf += ("clever" -> List(clever.outputvcf))
    add(clever)

    //    val breakdancerDir = sampleDir + sampleID + ".breakdancer/"
    //    val breakdancer = Breakdancer(this, bamFile, this.reference, breakdancerDir )
    //    outputFiles += ("breakdancer_vcf" -> List(breakdancer.output) )
    //    addAll(breakdancer.functions)

    return sampleOutput
  }

  // Called for each run from a sample
  
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput = {
    val libraryOutput = new LibraryOutput
    var outputFiles: Map[String, File] = Map()
    val runID: String = runConfig("ID").toString
    val sampleID: String = sampleConfig("ID").toString
    val alignmentDir: String = outputDir + sampleID + "/alignment/"

    val runDir: String = alignmentDir + "run_" + runID + "/"
    if (runConfig.contains("R1")) {
//      val mapping = Mapping.loadFromLibraryConfig(this, runConfig, sampleConfig, runDir)
      val mapping = new Mapping(this)
      
      mapping.defaultAligner = "stampy"
      mapping.skipFlexiprep = false
      mapping.skipMarkduplicates = true // we do the dedup marking using Sambamba
      
      if (runConfig.contains("R1")) mapping.input_R1 = new File(runConfig("R1").toString)
      if (runConfig.contains("R2")) mapping.input_R2 = new File(runConfig("R2").toString)
      mapping.paired = (mapping.input_R2 != null)
      mapping.RGLB = runConfig("ID").toString
      mapping.RGSM = sampleConfig("ID").toString
      if (runConfig.contains("PL")) mapping.RGPL = runConfig("PL").toString
      if (runConfig.contains("PU")) mapping.RGPU = runConfig("PU").toString
      if (runConfig.contains("CN")) mapping.RGCN = runConfig("CN").toString
      mapping.outputDir = runDir

      mapping.init
      mapping.biopetScript
      addAll(mapping.functions)

      // start sambamba dedup
      
//      outputFiles += ("final_bam" -> mapping.outputFiles("finalBamFile"))
      libraryOutput.mappedBamFile = mapping.outputFiles("finalBamFile")
    } else this.logger.error("Sample: " + sampleID + ": No R1 found for run: " + runConfig)
    return libraryOutput
//    logger.debug(outputFiles)
//    return outputFiles
  }
}

object Yamsvp extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/yamsvp/Yamsvp.class"
}
