/*
 * Structural variation calling
 */

package nl.lumc.sasc.biopet.pipelines.yamsvp

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.core.PipelineCommand

import nl.lumc.sasc.biopet.extensions.Cat
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles

import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping

import nl.lumc.sasc.biopet.scripts.PrefixFastq

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function._
import org.broadinstitute.gatk.utils.commandline.{ Argument }




class Yamsvp(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)

  var reference: File = _
  var finalBamFiles: List[File] = Nil
  
  @Input(doc = "countBed", required = false)
  var countBed : File = _
  
  def init() {
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
    logger.warn("runSamplesJobs")
    runSamplesJobs

  }
  
  def runSingleSampleJobs(sampleConfig: Map[String, Any]): Map[String, List[File]] = {
    var outputFiles: Map[String, List[File]] = Map()
    var libraryBamfiles: List[File] = List()
    var libraryFastqFiles: List[File] = List()
    val sampleID: String = sampleConfig("ID").toString
    val sampleDir: String = outputDir + sampleID + "/" 
    for ((library, libraryFiles) <- runLibraryJobs(sampleConfig)) {
      libraryFastqFiles +:= libraryFiles("prefix_fastq")
      libraryBamfiles +:= libraryFiles("FinalBam")
    }
    
    val bamFile: File = if (libraryBamfiles.size == 1) libraryBamfiles.head
                  else if (libraryBamfiles.size > 1) {
                    val mergeSamFiles = MergeSamFiles(this, libraryBamfiles, sampleDir)
                    add(mergeSamFiles)
                    mergeSamFiles.output
                  } else null
    val fastqFile: File = if (libraryFastqFiles.size == 1) libraryFastqFiles.head
                  else if (libraryFastqFiles.size > 1) {
                    val cat = Cat.apply(this, libraryFastqFiles, sampleDir + sampleID + ".fastq")
                    add(cat)
                    cat.output
                  } else null
    
    return outputFiles
  }
  
  // Called for each run from a sample
  def runSingleLibraryJobs(runConfig: Map[String, Any], sampleConfig: Map[String, Any]): Map[String, File] = {
    var outputFiles: Map[String, File] = Map()
    val runID: String = runConfig("ID").toString
    val sampleID: String = sampleConfig("ID").toString
    val runDir: String = outputDir + sampleID + "/run_" + runID + "/"
    if (runConfig.contains("R1")) {
      val flexiprep = new Flexiprep(this)
      flexiprep.outputDir = runDir + "flexiprep/"
      flexiprep.input_R1 = new File(runConfig("R1").toString)
      flexiprep.skipClip = true
      flexiprep.skipTrim = true
      flexiprep.sampleName = sampleID
      flexiprep.libraryName = runID
      flexiprep.init
      flexiprep.biopetScript
      addAll(flexiprep.functions)
      
      val flexiprepOutput = for ((key,file) <- flexiprep.outputFiles if key.endsWith("output_R1")) yield file
      val prefixFastq = PrefixFastq.apply(this, flexiprepOutput.head, runDir)
      prefixFastq.prefix = config("sage_tag", default = "CATG")
      prefixFastq.deps +:= flexiprep.outputFiles("fastq_input_R1")
      add(prefixFastq)
      outputFiles += ("prefix_fastq" -> prefixFastq.output)
      
      val mapping = new Mapping(this)
      mapping.skipFlexiprep = true
      mapping.skipMarkduplicates = true
      mapping.defaultAligner = "bwa"
      mapping.input_R1 = prefixFastq.output
      mapping.RGLB = runConfig("ID").toString
      mapping.RGSM = sampleConfig("ID").toString
      if (runConfig.contains("PL")) mapping.RGPL = runConfig("PL").toString
      if (runConfig.contains("PU")) mapping.RGPU = runConfig("PU").toString
      if (runConfig.contains("CN")) mapping.RGCN = runConfig("CN").toString
      mapping.outputDir = runDir
      mapping.init
      mapping.biopetScript
      addAll(mapping.functions)
      
      outputFiles += ("FinalBam" -> mapping.outputFiles("finalBamFile"))
    } else this.logger.error("Sample: " + sampleID + ": No R1 found for run: " + runConfig)
    return outputFiles
  }
  
}

object Yamsvp extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/yamsvp/Yamsvp.class"
}
