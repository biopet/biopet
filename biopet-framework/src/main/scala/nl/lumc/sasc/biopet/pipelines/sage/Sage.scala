package nl.lumc.sasc.biopet.pipelines.sage

import nl.lumc.sasc.biopet.core.{ MultiSampleQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.Cat
import nl.lumc.sasc.biopet.extensions.bedtools.BedtoolsCoverage
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import nl.lumc.sasc.biopet.scripts.PrefixFastq
import nl.lumc.sasc.biopet.scripts.SquishBed
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function._

class Sage(val root: Configurable) extends QScript with MultiSampleQScript {
  def this() = this(null)
  
  @Input(doc = "countBed", required = false)
  var countBed : File = _
  
  @Input(doc = "squishedCountBed, by suppling this file the auto squish job will be skipped", required = false)
  var squishedCountBed : File = _
  
  def init() {
    for (file <- configfiles) globalConfig.loadConfigFile(file)
    if (countBed == null && squishedCountBed == null) throw new IllegalStateException("No bedfile supplied, please add a countBed or squishedCountBed")
  }

  def biopetScript() {
    if (squishedCountBed == null) {
      val squishBed = SquishBed.apply(this, countBed, outputDir)
      add(squishBed)
      squishedCountBed = squishBed.output
    }
    
    // Tag library creation
    
    runSamplesJobs
  }
  
  // Called for each sample
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
    val fastqFile: File = if (libraryBamfiles.size == 1) libraryBamfiles.head
                  else if (libraryBamfiles.size > 1) {
                    val cat = Cat.apply(this, libraryBamfiles, sampleDir + sampleID + ".fastq")
                    add(cat)
                    cat.output
                  } else null
    
    this.addBedtoolsCounts(bamFile, sampleID, sampleDir)
    
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
      add(prefixFastq)
      outputFiles += ("prefix_fastq" -> prefixFastq.output)
      
      val mapping = new Mapping(this)
      mapping.skipFlexiprep = true
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
      
      if (config("library_counts", default = false).getBoolean) 
        this.addBedtoolsCounts(mapping.outputFiles("finalBamFile"), sampleID + "-" + runID, runDir)
      
      outputFiles += ("FinalBam" -> mapping.outputFiles("finalBamFile"))
    } else this.logger.error("Sample: " + sampleID + ": No R1 found for run: " + runConfig)
    return outputFiles
  }
  
  def addBedtoolsCounts(bamFile:File, outputPrefix: String, outputDir: String) {
    add(BedtoolsCoverage(this, bamFile, squishedCountBed, outputDir + outputPrefix + ".sense.count", 
                         depth = false, sameStrand = true, diffStrand = false))
    add(BedtoolsCoverage(this, bamFile, squishedCountBed, outputDir + outputPrefix + ".antisense.count", 
                         depth = false, sameStrand = false, diffStrand = true))
    add(BedtoolsCoverage(this, bamFile, squishedCountBed, outputDir + outputPrefix + ".count", 
                         depth = false, sameStrand = false, diffStrand = false))
  }
}

object Sage extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/pipelines/sage/Sage.class"
}
